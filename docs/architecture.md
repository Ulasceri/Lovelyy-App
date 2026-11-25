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
