# Architecture (step 1)

## Android app (`android/`)

```
com.lovelyy.app
├── LovelyyApplication   tiny dependency container, detects whether Firebase is configured
├── MainActivity         edge-to-edge + splash + LovelyyTheme
├── data/                Firebase access (AuthRepository, UserRepository, PairingRepository)
└── ui/
    ├── AppViewModel     decides which screen to show from auth + Firestore state
    ├── LovelyyRoot      cross-fades between: Setup → Login → Name → Pairing → Home
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
| `couples/{id}.startDate` | timestamp | day counter; start of the chosen day in the phone's zone |
| `couples/{id}/signals/{auto}` | kind (heartbeat/nudge/mood/question/note/event), emotion, mood, text, from, fromName, to, at, createdAt | heartbeats, nudges and notices for the partner |
| `couples/{id}/moods/{uid}` | kind, note, at | each partner's current mood |
| `couples/{id}/questions/{yyyy-MM-dd}` | day, question, answers.{uid} = {name, text, at} | günün sorusu; question text comes from `QuestionBank.forDay` so both phones agree without a server |
| `couples/{id}/notes/{auto}` | text, color (0-4), pinned, by, byName, at | shared sticky notes |
| `couples/{id}/events/{auto}` | title, date (yyyy-MM-dd), emoji, yearly, by, byName, at | plans and special days; the anniversary is built in from `startDate`, not stored |

Günün sorusu hides the partner's answer until you answer, but only in the app: Firestore rules
can't hide one field of a document from a member, and both partners are members. That's fine for
a couple's own data. Calendar date math (yearly repeats, 29 Feb, countdowns) is in
`CalendarMath` and covered by `CalendarMathTest`.

All of these are covered by the existing "members only" rule for `couples/{id}/**`; no rules
deploy was needed.

## Partner notifications on the free Spark plan

Real push (FCM) needs a trusted sender (a Cloud Function), and functions need the Blaze plan.
So `signals/SignalCenter.kt` does the best possible without a server:

- While the app process is alive (open or recently backgrounded) a Firestore listener sees a new
  signal within about a second and posts a local notification.
- When the app is closed, `SignalWorker` (WorkManager) checks every ~15 minutes, Android's
  minimum, and posts what was missed.

With Blaze: add `firebase-messaging`, store each user's FCM token, and a function on
`couples/{id}/signals/{sid}` create that sends to the partner's token. Then delivery is instant
even when the app is closed. The client side above stays as the in-app experience.
