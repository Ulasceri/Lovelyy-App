# Architecture (step 1)

## Android app (`android/`)

```
com.lovelyy.app
├── LovelyyApplication   tiny dependency container, detects whether Firebase is configured
├── MainActivity         edge-to-edge + splash + LovelyyTheme
├── data/                Firebase access (AuthRepository, UserRepository, PairingRepository)
└── ui/
    ├── AppViewModel     decides which screen to show from auth + Firestore state; owns the per-account ViewModelStore for screen ViewModels
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
| `couples/{coupleId}` | members [A, B], createdAt, startDate, plus (Lovelyy Plus map, see below) | the two members only; members can't be changed |
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
| `items`, `itemsAt`, `itemsNew` | Mırmır's items (P7): `{feed, water, play, sleep}` → item id (`CatItems`), when they were last saved, and the item that save placed for the first time (Mırmır's reaction) |
| `sleepRefillMin` | 45 while asleep in a Plus bed (fixed at bedtime), else absent = 60 minutes from empty to full |
| `streak` | shared care streak: `count`, `best`, `lastCompleteDay`, `careDay`, `careBy`, and the Plus streak protection: `freezeMonth` (`yyyy-MM`), `freezeUsed` (0..2 in that month), `freezeDays` and `repairDays` (protected days, last 6 kept), `brokenCount` / `brokenLast` (the streak that just broke, for the make-up) |

No server code: needs decay with time, so every phone computes the current levels from the
stored snapshot and the clock (`data/Cat.kt`, `CatRules`). Actions run in a Firestore transaction
that snapshots all needs at "now" and applies the change, so two partners acting at the same
moment never lose an update. Rates: hunger empties in 12h, thirst 10h, energy 16h (1h of sleep
refills it), love 8h. The streak grows by one for each day on which both partners cared.

**Time comes from the server, not the phone** (`data/TrustedClock.kt`). On every app start (Home, and before
the first care action) `ClockSync` writes `couples/{id}/clock/{uid}` = `{at: serverTimestamp}` and reads the stamp
back; `ServerTime.base` turns it into a base on the phone's boot clock (`elapsedRealtime`, which the user can't
change), so `TrustedClock.now()` = server time and keeps running correctly after the user moves the clock or the
time zone. The base is saved with the boot count, so a cold start without a connection still uses it; after a
restart and before the first sync the phone's own clock is used. `CatRules`, `CatRepository.act`,
`CoupleClock.today()` (every shared "today"), `PlusClock` and the widgets use it. A large difference is only
logged (`Phone clock is off`), there is no warning UI.
firestore.rules back this up: in `cat/main` a need's `at`, `sleepingSince` and `lastAction.at` may only change to
a time within ±5 minutes of `request.time`, and `streak.careDay` / `lastCompleteDay` only to a day that is
"today" somewhere at the server's time (`dayNearNow`). So a phone set ahead can no longer wipe the partner's
half-done day or make the cat sleep for 25 hours.
Days a wrong clock stored before this are healed by `CareStreak.afterCare` (a day more than one day ahead is
replaced by today, see the `CareStreak` KDoc). Durations are absolute (epoch millis) and calendar days are counted
in the couple's shared zone, so travelling, partners in different zones and daylight saving change nothing.

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

## Cat card v2 (Yuva) and Kedi bakımı v2

Nothing is drawn over the video any more (boards "V2 · Anasayfa kedi kartı v2" and "14 · Kedi bakımı v2"); only
the petting hearts rise from the finger. Yuva's card (`CatSection`) is, top to bottom: a 36dp strip with the
name (pencil = rename) and the streak chip, the video (`CatStage`), a 36dp slot below it with the speech bubble
(tail pointing up) where banners ("Deniz besledi", refusals, errors) briefly take the bubble's place, then the need
bars. The card keeps its old height (a 1.12 : 1 stage plus the need bars): the video gives up the strip and the
slot, see `CatCardGeometry` (pure, `CatCardGeometryTest` checks the height and that the cat, ears to paws, stays
in the crop from a 320dp card up). The strip's buttons have 48dp touch targets that reach 12dp above the card,
so the strip stays 36dp and never steals touches from the cat (the card is not clipped; the video clips itself).
Kedi bakımı uses the same video size and bubble slot without the strip; the "Bugün: ..." line moved into the
streak card ("N gün bakım serisi", "En uzun seri", each partner's latest care today with a check once they count).

"Seri nedir?" (`StreakExplainerSheet`, board S1) explains a streak day (both partners cared that day), the streak
and a missed day (it restarts from day 1 after midnight). `CatViewModel` opens it by itself once per couple on this
phone, the first time the shown streak is at least 1 (`StreakExplainer.shouldAutoShow`, seen flag
`streak_explainer_seen_{coupleId}` in the `lovelyy_onboarding` prefs); the Yuva streak chip and the Kedi bakımı
streak card open it again and also mark it seen. Its Plus note follows the couple's Plus status (trial, paid,
free) and is hidden outside Home.

### Streak protection (Lovelyy Plus, boards P5a-c)

Pure rules in `data/StreakProtection.kt` (`StreakProtectionTest`); a protected day (`freezeDays` ∪ `repairDays`)
bridges the gap between two complete days, so `CareStreak.current` / `afterCare` keep counting across it. The
streak's "today" is the couple's day at Plus time (`PlusClock`, so the P8 debug clock moves it too).

- **P5a "Seri tehlikede"** (`ui/cat/StreakGuard.kt` → `StreakDangerSection`): after 20:00 in the couple's zone,
  when the streak is above 0 and today isn't complete yet. Yuva (in the Plus card slot, after the day-12 card, so
  never on the same day) and Kedi bakımı. "Dürt" sends the existing nudge signal (only while the partner hasn't
  cared); "İzin günü" opens P5b for Plus, the paywall (`lock_streak_day_off`) otherwise.
- **P5b "İzin günü"**: Plus only, 2 per calendar month shared by the couple, renewed on the 1st; today becomes a
  protected day (`StreakProtection.freeze`, `CatRepository.changeStreak` transaction).
- **P5c "Telafi"**: the day after one missed day, until midnight (`StreakProtection.repairable`). Opens by itself
  on a home tab once per break per phone (`PlusPrefs.repairOffered`); for a free couple it is a Plus offer
  (paywall `lock_streak_repair`, reopened after the purchase) and not on a day the trial-end paywall already
  opened. If both partners already cared today the streak restarted at 1, so the make-up gives `brokenCount + 1`.
  Two missed days can never be made up.

### Mırmır's items (Lovelyy Plus, boards E1, P7a, P7b)

`data/CatItems.kt` (`CatItemsTest`): one item per care action, 12 in all (food 3, water 2, toy 4, bed 3). The plain
("sade") model of each is free, the rest are Plus models with a light bonus (Ulaş, 2026-11-27): food +10 fullness
(40 → 50), water 45 → 55, toy love 25 → 35, and the bed fills energy in 45 minutes instead of 60
(`CatItemRules`, applied in `CatRules.apply(plus = …)`). Nothing else changes for free couples (decay, caps, streak).
Without Plus a stored Plus model stays stored but falls back to the free one (`CatItems.effective`): free picture,
free clip, no bonus; it comes back with Plus. firestore.rules `validItems` allow only each slot's own models and
need `plusActive()` for a newly chosen Plus model or a new 45-minute sleep.

- **P7a** (`ui/cat/CatItemsScreen.kt`): sofa button on Kedi bakımı's top bar. Preview (cat + chosen items), one
  row per care action with its Plus bonus, "Yerleştir" (`CatRepository.setItems`, a transaction that also stores
  `itemsNew`). Plus models without Plus: "Plus ile yerleştir" opens the plans (`lock_cat_items`). A save opens Home.
- **P7b** (`CatSection`): once placed, the items stand on the floor of the cat video (`CatItemsLayer`, two depth
  rows, app-drawn contact shadows, positions from the board). The item inside the clip that is playing is hidden
  (`CatClip.slot`). First placement: the item pops in, the happy clip plays and Mırmır's line
  (`item_love_<id>`) replaces the bubble, once per save on each phone (`OnboardingPrefs.itemsSeenAt`).
  The "Son bakım" card (`LastCareCard`) under the day counter shows each partner's latest care today.
- **Clips** (`CatClip`, files in `assets/cat/` from `tools/item-art`): a Plus model plays its own care clip
  (fish plate, milk, fountain, mouse, feather, scratching post) and the Plus beds their own lie-down / sleeping /
  wake-up clips (`Bed`). Item clips start and end on the cat with the item in place, so they cross-fade in and out.
- Not yet: items on the home-screen widgets.

### Free limits (Lovelyy Plus, board P9)

One place knows the numbers: `data/PlusLimits.kt` (`PlusLimitsTest`); screens use `ui/plus/PlusLimitParts.kt`
(`rememberLimitGate`, `PlusLimitBanner`, `LockedAddButton`). Limits count what the couple has together: 50 notes,
10 calendar events (plans and special days; the built-in anniversary doesn't count), 20 dreams, 1 sealed capsule
letter, 2 custom wheels, 10 saved drawings. **Nothing is ever hidden or deleted**: over a limit (e.g. after Plus
ended) a list stays readable, editable and deletable; only adding is blocked (lock button → paywall
`lock_<limit>`), and the mint note shows only when the list is over (not at) the limit. In the live drawing a
blocked save only shows a message (Plus screens never open there). Basic cat care is never limited.
Decisions: the 30-day history limit blocks nothing (history screens have no add action and old history stays
readable), and no pen colour is locked yet (the palette has 5 of the 6 free colours).

## Widget (`widget/CatWidget.kt`)

RemoteViews widget with one picture per mood. The app saves the cat whenever it changes; the
widget computes the mood itself (time-based), and WorkManager refreshes from Firestore every 30 min.

Four widgets (all RemoteViews, `widget/CatWidget.kt` + `widget/MoreWidgets.kt`): small cat, wide cat
(needs bars + streak + couple day), day counter with a heart, and live drawing. Widgets can't read
Firestore while the app is closed, so `WidgetData` keeps the couple (ids, names, start date) in
SharedPreferences; sign-out clears it. The heart is a `HeartReceiver` broadcast that sends a "Sevgi"
heartbeat with the signed-in user. The drawing widget shows a 420 px PNG rendered by the app (or by the
30-min worker) into app storage. In-app showcase: `ui/widgets/WidgetsScreen.kt` (Biz → Ana ekran widget'ları).
Taps open their screen: widgets and notifications pass a `DeepLink` route (`MainActivity.kt`) that Home pushes. `MainActivity` is `singleTask`, so a tap while the app is open reaches `onNewIntent` and still opens the screen.
Cat accessories were removed on 2026-11-26 (Ulaş): drawn over the video they stayed still while the cat moved.
If they come back they will be designed first and baked into the video clips (tools/cat-animation). The care streak
stays (Kedi bakımı shows the current and best streak; "Seri nedir?" explains it, see "Cat card v2" above). `firestore.rules` still accepts the old `wearing` field on
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
| `couples/{id}/clock/{uid}` | at (server time) | written and read back by `ClockSync` to learn the server time (`TrustedClock`); deleted on unpair |
| `couples/{id}/limits/{uid}` | signalAt, hourStart, hourCount, dayStart, dayCount (server times) | signal rate limit, written in the same batch as every signal; limits from `config/limits` (default 15/hour, 60/day, 2 s gap), see docs/security.md |
| `config/limits` | signalsPerHour, signalsPerDay, minGapSeconds | app-wide config edited in the Firebase console, read-only for the app |
| `couples/{id}/moods/{uid}` | kind, note, at | each partner's current mood |
| `couples/{id}/moodLog/{uid}_{yyyy-MM-dd}` | kind, note, at, uid, day | one entry per person per day (the last mood that day), written next to `moods/{uid}`; read by Ruh hali geçmişi (last 31 days). Started 2026-11-26, older moods were not logged |
| `couples/{id}/questions/{yyyy-MM-dd}` | day, question, answers.{uid} = {name, text, at} | günün sorusu; question text comes from `QuestionBank.forDay` (366 questions, loaded from `res/values/questions.xml`) and the first answerer fixes it: later answers and the screen keep the stored text, so partners on different app versions still see the same question |
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
  until the flow ends. People who were paired before never see it. Steps: Eşleştiniz, the Plus trial gift (P1),
  then a primer for
  notifications (Android 13+) and one for location; a permission that is already granted skips its primer.
  The system window opens only from the primer's main button. Allowing location shares it the same way as
  the Mesafe screen (city and distance only).

No names appear in onboarding (product decision): the copy says "Sevgilin" and "Sen".

# Lovelyy Plus (subscription core)

Approved design: canvas section "Tur 3 · Lovelyy Plus" (boards P1-P10). One entitlement covers **both
partners**; the couple gets a 14-day trial when they pair; after that yearly or monthly, or a 1-year gift.

## Data: `couples/{id}.plus` (a map on the couple doc)

| Field | Type | Meaning |
|---|---|---|
| `trialStart` | int (ms) | Trial start. Set once (P1 "Başlayalım", or Home for couples paired before Plus) in a transaction; the rules keep it within a day of server time and never let it change |
| `until` | int (ms) or null | End of the paid period (null = never bought) |
| `source` | `trial` / `sub` / `gift` | Where Plus comes from |
| `product` | string | `lovelyy_plus:yearly`, `lovelyy_plus:monthly`, `lovelyy_plus_gift_year` (`PlusPlan.key`) |
| `buyerUid` | member uid or null | Who bought it; only this signed-in member may change the purchase fields |
| `autoRenew` | bool or null | Last renewal state the buyer's phone saw in Google Play |
| `orderToken` | string | Google Play purchase token (stable across renewals), kept for a later server check |
| `giftNote` | string ≤ 140 | Optional note sent with a gift, shown once in P6b |

The couple doc is already observed by `AppViewModel`, so `Couple.plus` arrives with it; `PlusRepository` only
writes (`ensureTrial` transaction, `write`). Unpairing deletes the couple doc and the map with it; a buyer whose
subscription still runs gets it written into their next couple by the sync below.

## Rules: `data/Plus.kt` → `PlusRules` (pure, `PlusRulesTest`)

- `isPlus` = trial running (14 days) or paid and running; a renewing subscription keeps Plus 3 days past its
  stored end (`RENEW_GRACE_MS`) so the partner doesn't lose Plus before the buyer's phone writes the renewal.
- `showDay12Card`: trial days 12 and 13, closable per day (P2b).
- `shouldShowTrialEndPaywall`: trial ended and nothing bought, at most once per couple day, and after two
  closes in a row it stays away 7 days (counters in `PlusPrefs`, local). Only asked on a home tab.
- `firstYearOfferActive`: the 48 h after the trial ended (yearly shows the `first-year` offer and a countdown).
- `canPurchase` / `purchaseBlocked`: no second purchase while Plus is paid, active and auto-renewing; a running
  non-renewing period (gift, cancelled subscription) can only be bought again on its last day.
- `canPartnerTakeOver`: the partner who didn't buy, on the last day of a non-renewing paid period or after it
  ended; never while the buyer's subscription renews (P8b button, P10 sheet, small Home card first).
- `shouldCelebrate`: P6b once per purchase (`celebrationKey` = source + buyer + token) for the other partner.
- `subscriptionEnd`: Play doesn't give the app the expiry, so the buyer's phone computes whole periods from the
  purchase time; `sync` rewrites it (and `autoRenew=false` when the subscription is gone) once per app run.

## Billing: `billing/BillingRepository.kt` (Play Billing 7, `billing-ktx`)

Product ids (`PlusProducts`, must match Play Console exactly):

| Play Console | Id | Notes |
|---|---|---|
| Subscription | `lovelyy_plus` | base plans `yearly` (₺499,99/yıl) and `monthly` (₺79,99/ay), auto-renewing |
| Offer on `yearly` | `first-year` | first year ₺399,99, then the base price; the app only offers it in the 48 h window |
| In-app product | `lovelyy_plus_gift_year` | one-time ₺499,99, "1 year of Plus"; consumed after it is recorded so it can be bought again |

Flow (`ui/plus/PlusViewModel`, one keyed instance `plus-<coupleId>`): `launch` opens Play's window (the plan is
put in the obfuscated profile id because purchases don't name the base plan) → `PurchasesUpdatedListener` →
`PlusRules.afterPurchase` written to Firestore → **then** acknowledge (subscription) / consume (gift), so a
purchase that never reached Firestore is refunded by Google instead of being lost. Restore and the once-per-run
sync use `queryPurchasesAsync`. If products can't be loaded the screens show the string-resource prices and a
purchase shows a friendly error.

**Debug fake purchase**: debug builds skip Google Play when `-Pplus.fakeBilling=true` is passed, or
automatically when the products can't be loaded (emulator, no Play listing yet): the purchase is written with a
`debug-…` token. Plus management (P8) shows a debug box: "+1 gün" moves Plus time forward (`PlusClock`, debug
only), "+3 saat" (to reach the 20:00 streak card), "Gerçek zaman", "Yenilemeyi kapat" (to try takeover / second purchase) and "Satın alımı sıfırla".

## UI: `ui/plus/`

| Board | Code | Where |
|---|---|---|
| P1 Deneme hediyesi | `PlusTrialGiftScreen` | step of `PairedFlow` after Eşleştiniz, before the permission primers (no names) |
| P2a Biz kartı | `BizPlusCard` | Biz tab under the couple card → P8 |
| P2b 12. gün kartı | `HomePlusCard` | Yuva under the care buttons (also used for the takeover hint) |
| P3 Paywall | `PaywallScreen` | full-screen overlay in `HomeScreen` (`paywallSource`), automatic only on a home tab |
| P4 Kilit | `PlusLockSheet` | overlay; opened through `LocalPlus.openLock(feature)` or a locked widget tap |
| P6a / P6b | `PlusGiftScreen` (route `plus_gift`) / `PlusCelebration` (overlay) | |
| P8 / P8b | `PlusManageScreen` (route `plus`) | buyer vs. partner view |
| P10 | `TakeoverSheet` | overlay, only when `canPartnerTakeOver` |
| P5a / P5b / P5c | `StreakDangerSection` / `DayOffSheet` / `StreakRepairSheet` (`ui/cat/StreakGuard.kt`) | see "Streak protection" above |
| P9 | `PlusLimitBanner`, `LockedAddButton` (`ui/plus/PlusLimitParts.kt`) | notes, plans, dreams, capsule, wheel, drawings; see "Free limits" above |

`LocalPlus` (`PlusAccess`: map, time, `isPlus`, `openLock`, `openPlans`) is provided by `HomeScreen` for every
screen under it: later Plus features (free-tier limits, streak protection, decorations) plug in there.
Automatic Plus screens never open over the heartbeat, the live drawing or a letter being written (those are
inner screens), and basic cat care is never locked.

Widgets: `WidgetData.savePlus` stores `PlusRules.activeUntil`; without Plus the wide cat widget shows "Plus ile"
and opens P4, and the days widget's heart opens P4 instead of sending (the day counter itself stays free).
