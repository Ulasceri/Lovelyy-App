# Architecture (step 1)

## Android app (`android/`)

```
com.lovelyy.app
├── LovelyyApplication   tiny dependency container, detects whether Firebase is configured
├── MainActivity         edge-to-edge + splash + LovelyyTheme
├── data/                Firebase access (AuthRepository, UserRepository, PairingRepository)
└── ui/
    ├── AppViewModel     decides which screen to show from auth + Firestore state
    ├── LovelyyRoot      cross-fades between: Setup → Login (Karşılama → intro → form) → Name → Pairing → JustPaired → Home
    ├── theme/           colors, Quicksand typography, rounded shapes
    ├── components/      shared buttons, text field, avatars, beating heart
    └── auth/ profile/ pairing/ home/ setup/
```

There is no navigation library yet: the app state (signed out / no name / not paired / paired)
fully decides the screen, so the back stack can't get out of sync with the data.

## Firestore data model

| Path | Fields | Who can read / write |
|---|---|---|
| `users/{uid}` | displayName, photoUrl, coupleId, pairCode, createdAt | self; partner can read |
| `pairCodes/{CODE}` | ownerUid, createdAt | any signed-in user can *get* by exact code (never list); expires after 24h; one use |
| `couples/{coupleId}` | members [A, B], createdAt, startDate | the two members only; members can't be changed |
| `couples/{id}/**` | every future shared feature (cat, notes, mood...) | the two members only |

### Pairing flow

1. A opens "Kodumu paylaş": the app writes `pairCodes/CODE` and `users/A.pairCode`.
2. B enters CODE. One transaction: create `couples/CODE` (members [A, B]), set `coupleId` on both
   user docs, delete `pairCodes/CODE`.
3. A's app is listening to `users/A` and jumps to Home as soon as `coupleId` appears.

`firebase/firestore.rules` only accepts that exact shape: the code must exist, be under 24h old,
belong to someone else, both people must be unpaired, and the code is consumed in the same write.
No Cloud Functions are needed, so it works on the free Spark plan.

Tests: `firebase/tests/firestore.rules.test.mjs` runs these flows (and attacks) against the
Firestore emulator.

## Realtime Database

`firebase/database.rules.json` is locked (deny all) until live features (drawing, heartbeat)
arrive.

# Step 2: the shared cat

## Data: `couples/{coupleId}/cat/main`

| Field | Meaning |
|---|---|
| `name` | given by the couple (either partner, editable) |
| `hunger`, `thirst`, `energy`, `love` | `{level 0..100, at}`: the level at time `at` |
| `sleepingSince` | set while asleep; energy refills, the cat wakes up by itself when full |
| `lastAction`, `recent` | who did what ("Deniz besledi"), last 10 |
| `streak` | shared care streak: `count`, `best`, `lastCompleteDay`, `careDay`, `careBy` |

No server code: needs decay with time, so every phone computes the current levels from the
stored snapshot and the clock (`data/Cat.kt`, `CatRules`). Actions run in a Firestore transaction
that snapshots all needs at "now" and applies the change, so two partners acting at the same
moment never lose an update. Rates: hunger empties in 12h, thirst 10h, energy 16h (1h of sleep
refills it), love 8h. The streak grows by one for each day on which both partners cared.

## Animation (`ui/cat/`)

- 14 video clips in `assets/cat/` (720×720 H.264, 24 fps, ~5 MB total) generated with fal
  (`tools/cat-animation/`). Every clip starts and ends on one of five key poses
  (sitting, sleeping, with food bowl, water bowl, yarn ball).
- `CatAnimator` plays them on two ExoPlayer/TextureView layers. The next clip is always preloaded
  and paused on its first frame. Same pose at the boundary → instant cut (identical frames, so
  invisible); different pose or an interruption (touch, feeding) → 280 ms cross-fade with the new
  layer on top. Lying down / waking up are bridge clips that are never interrupted.
- Mood (from needs) picks the loop: hungry, thirsty, sleepy, wants love, happy, idle, sleeping.
  Actions play eating / drinking / playing; tap = surprised; stroking = petted + hearts + haptics.
- The partner's actions arrive through the Firestore listener and play the same animation.

## Widget (`widget/CatWidget.kt`)

RemoteViews widget with one picture per mood. The app saves the cat whenever it changes; the
widget computes the mood itself (time-based), and WorkManager refreshes from Firestore every 30 min.

Four widgets (all RemoteViews, `widget/CatWidget.kt` + `widget/MoreWidgets.kt`): small cat, wide cat
(needs bars + streak + couple day), day counter with a heart, and live drawing. Widgets can't read
Firestore while the app is closed, so `WidgetData` keeps the couple (ids, names, start date) in
SharedPreferences; sign-out clears it. The heart is a `HeartReceiver` broadcast that sends a "Sevgi"
heartbeat with the signed-in user. The drawing widget shows a 420 px PNG rendered by the app (or by the
30-min worker) into app storage. In-app showcase: `ui/widgets/WidgetsScreen.kt` (Biz → Ana ekran widget'ları).
Taps open their screen: widgets and notifications pass a `DeepLink` route (`MainActivity.kt`) that Home pushes.
Cat accessories were removed on 2026-11-26 (Ulaş): drawn over the video they stayed still while the cat moved.
If they come back they will be designed first and baked into the video clips (tools/cat-animation). The care streak
stays (Kedi bakımı shows the current and best streak). `firestore.rules` still accepts the old `wearing` field on
`cat/main` so older installs keep working; the app no longer writes it. Unsealed capsule letters are kept as a draft on the phone.

# Step 3: redesigned screens + first three features

## Navigation

`AppState` still picks the big screen (login → name → pairing → home). Inside Home,
`ui/home/HomeScreen.kt` adds a bottom nav (Yuva · Anılar · ♥ · Planlar · Biz). Shared building
blocks of the approved design are in `ui/components/Design.kt`.

**Only designed and approved screens are in the app**: Karşılama, Giriş, Eşleşme, Anasayfa. The
feature screens built earlier without a design (gün sayacı, kalp atışı, ruh hali, günün sorusu,
notlar, takvim) were removed on 2026-11-25 at the owner's request; they are in git history
(up to commit cf23db5). Home cards stay as in the design and answer a tap with "Yakında". The
data layer below (`data/*`, `signals/*`) is kept for when each screen is designed, approved and
rebuilt. Rule from now on: design first, approval, then implementation.

Pairing codes are now 6 digits (2-9, so they still match the existing rules regex) and the app
treats them as valid for 30 minutes (the rules still allow up to 24h).

## Data

| Path | Fields | Notes |
|---|---|---|
| `couples/{id}.startDate` | timestamp | day counter; start of the chosen day in the couple's time zone |
| `couples/{id}.endedAt / endedBy / endedByName` | server time, uid, name | set once when a partner unpairs; the other phone then leaves and deletes the couple doc |
| `couples/{id}/settings/main` | zone (IANA id), setBy, at | the couple's shared time zone: written once (transaction) with the first phone's zone; every shared "today" (care streak, daily question, capsule open day, calendar, start date) is computed in it via `CoupleClock`/`CoupleDay`, falling back to the phone's zone until loaded. No UI to change it |
| `couples/{id}/signals/{auto}` | kind (heartbeat/nudge/mood/question/note/event), emotion, mood, text, from, fromName, to, at, createdAt | heartbeats, nudges and notices for the partner |
| `couples/{id}/limits/{uid}` | signalAt, hourStart, hourCount, dayStart, dayCount (server times) | signal rate limit, written in the same batch as every signal; limits from `config/limits` (default 15/hour, 60/day, 2 s gap), see docs/security.md |
| `config/limits` | signalsPerHour, signalsPerDay, minGapSeconds | app-wide config edited in the Firebase console, read-only for the app |
| `couples/{id}/moods/{uid}` | kind, note, at | each partner's current mood |
| `couples/{id}/moodLog/{uid}_{yyyy-MM-dd}` | kind, note, at, uid, day | one entry per person per day (the last mood that day), written next to `moods/{uid}`; read by Ruh hali geçmişi (last 31 days). Started 2026-11-26, older moods were not logged |
| `couples/{id}/questions/{yyyy-MM-dd}` | day, question, answers.{uid} = {name, text, at} | günün sorusu; question text comes from `QuestionBank.forDay` so both phones agree without a server |
| `couples/{id}/notes/{auto}` | text, color (0-4), pinned, by, byName, at, updatedAt, updatedBy (last text/color edit; missing on old notes) | shared sticky notes; an edit is saved in a transaction and asks before overwriting a partner's newer change |
| `couples/{id}/events/{auto}` | title, date (yyyy-MM-dd), emoji, yearly, by, byName, at | plans and special days; the anniversary is built in from `startDate`, not stored |
| `couples/{id}/locations/{uid}` | city, lat, lng (rounded: 0.1° city-only or 0.01°), cityOnly, at | only while the partner shares location on Mesafe; turning sharing off deletes it. The app shows only city and distance |
| `couples/{id}/dreams/{auto}` | title, category (seyahat/deneyim/hayat/diger), goal, by, byName, at, doneAt | shared bucket list (Hayaller) |
| `couples/{id}/wheels/{id}` | label, items[], at | decision wheels; ids `yemek`, `gezi`, `izle` override the built-in wheels, other ids are custom wheels. Spins are not stored |
| `couples/{id}/wheelSpins/{wheelId}` | label, result, by, byName, at | the last result of each wheel, shared: the spinner saves it before the wheel stops, the partner's wheel rests on the same result and gets a "wheel" signal |
| `couples/{id}/wheelLog/{auto}` | wheelId, label, result, by, byName, at | every spin that counted (written in the same transaction as `wheelSpins`, or by the offline fallback); read by Çark geçmişi |
| `couples/{id}/capsules/{auto}` | title, text, openOn (yyyy-MM-dd), by, byName, at | time-capsule letters; the app hides the text until `openOn` (rules can't hide a field, so this is not a hard lock) |
| `couples/{id}/strokes/{auto}` | by, color, width, points[] (normalized 0..1, max 400 points), at | the current shared canvas (Canlı çizim), max 600 strokes |
| `couples/{id}/drawing/presence` | {uid: millis} | last time each partner drew; "drawing now" = within 45 s |
| `couples/{id}/drawings/{auto}` | strokes[], by, byName, at | drawings saved to Anılar; saving clears the canvas. Çizimlerimiz lists the newest 60 and can delete one (for both partners) |

Günün sorusu hides the partner's answer until you answer, but only in the app: Firestore rules
can't hide one field of a document from a member, and both partners are members. That's fine for
a couple's own data. You may change your answer only until the partner has answered too; after that
both answers are locked (checked in a transaction in `QuestionRepository.answer`, `QuestionMath.isLocked`). Calendar date math (yearly repeats, 29 Feb, countdowns) is in
`CalendarMath` and covered by `CalendarMathTest`.

History, search and inbox screens read what is already there: Kalp atışı geçmişi and Bildirimler use the last
`signals` (150 / 100), Önceki sorular uses `questions`, and Arama searches notes, dreams and opened letters on the
phone (sealed letters never match). The inbox's read state is per phone (`InboxSeen`, SharedPreferences), not shared.

## Unpairing and deleting the account (Spark, no server code)

Decision (Ulaş, 2026-11-26): unpairing deletes the shared data **for both** partners. `data/LeaveRepository.kt`:
1. The leaving partner in one batch sets `endedAt/endedBy/endedByName` on the couple and clears their own
   `coupleId`, then deletes every doc of every shared collection (`LeaveRules.SHARED`; a unit test checks it lists
   every collection in `Paths`). Ending first means a refused batch deletes nothing, and the rules block new
   shared writes while the purge runs.
2. The other phone (`AppViewModel`) sees the ended couple, deletes anything written meanwhile, clears its own
   `coupleId` and deletes the couple doc in one batch, and shows "X eşleşmeyi kaldırdı".
3. Deleting the account unpairs first, then deletes `users/{uid}` and the Firebase account. Firebase needs a
   recent sign-in, so the app checks the sign-in is under 4 minutes old *before* deleting anything; otherwise it
   signs out and asks to sign in again. It is reachable from Biz > Hesap and, for someone who never paired, from
   the account button on the pairing screen (`ui/pairing/PairingAccountSheet.kt`; Google Play requires in-app
   deletion). Both go through `LeaveViewModel.deleteAccount`.

The rules allow exactly this: ending once with the server time and your own uid, leaving only an ended (or
missing) couple, deleting your user doc only when unpaired, and no new shared writes after the couple ended
(deletes still work). Covered by three tests in `firebase/tests`.

Every shared collection has its own write rule (exact fields, size limits, author = signed-in user);
see [security.md](security.md). A new shared collection needs a new match block in `firestore.rules`
and tests, otherwise the app's writes are denied.

## Partner notifications on the free Spark plan

Real push (FCM) needs a trusted sender (a Cloud Function), and functions need the Blaze plan.
So `signals/SignalCenter.kt` does the best possible without a server:

- While the app process is alive (open or recently backgrounded) a Firestore listener sees a new
  signal within about a second and posts a local notification. The listener starts with the
  process (`LovelyyApplication.onCreate`) and re-subscribes after transient errors.
- When the app goes to the background, a few one-time `SignalWorker` checks run ~1, 3, 7 and 15
  minutes later; after that (and when the app is closed) the periodic `SignalWorker` checks every
  ~15 minutes, Android's minimum, and posts what was missed.
- "Is this new?" never uses the sender's clock (`at` is only for display): `SignalFreshness`
  remembers the ids of signals already seen and compares the server `createdAt` with a floor set
  on the first check (first-run grace: only the last 10 minutes notify after install or sign-in).
  The query is ordered by `createdAt` too.
- At most 3 notifications one by one; older new signals become one summary notification
  ("Bora sana 5 kalp daha gönderdi").

Remaining platform limits: Android freezes cached background processes (cached apps freezer,
Doze, App Standby), so a backgrounded app's listener can still lag by minutes until a job or the
user wakes it, and WorkManager delays are "at the earliest" (Doze can defer them further).

With Blaze: add `firebase-messaging`, store each user's FCM token, and a function on
`couples/{id}/signals/{sid}` create that sends to the partner's token. Then delivery is instant
even when the app is closed. The client side above stays as the in-app experience.

## Onboarding

Two parts, both in `ui/onboarding/` (design boards A1-A6). They share one idea, `SplitStage`: the top of
the screen is split by a slanted seam into the partner's side (mint) and your side (peach).

- **Intro** (`IntroScreen`): a mode of the signed-out screen (`LoginViewModel.Mode.Intro`), shown only after
  "Başlayalım" on Karşılama. Three pages, each with one optional tap whose result shows on the partner's
  side (feed the cat, send a heartbeat, draw half a heart). It is a local demo: nothing is written to
  Firestore. "Giriş yap" on Karşılama and "Atla" go straight to the form.
- **After pairing** (`PairedFlow`): the `AppState.JustPaired` state. `OnboardingPrefs` marks a user when they
  see the pairing screen on this phone; once they have a couple, `JustPaired` is shown instead of `Home`
  until the flow ends. People who were paired before never see it. Steps: Eşleştiniz, then a primer for
  notifications (Android 13+) and one for location; a permission that is already granted skips its primer.
  The system window opens only from the primer's main button. Allowing location shares it the same way as
  the Mesafe screen (city and distance only).

No names appear in onboarding (product decision): the copy says "Sevgilin" and "Sen".
