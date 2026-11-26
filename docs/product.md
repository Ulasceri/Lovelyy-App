# Lovelyy: product overview

Lovelyy is a mobile app for couples. At its heart is a **shared cat** that both partners
take care of together. Around the cat are small daily rituals that keep the couple close.

## v1 features (built step by step)

| Step | What | Status |
|---|---|---|
| 1 | Login (Google + email), partner pairing with a code, home screen skeleton | **this step** |
| 2 | The cat: name, needs (hunger, thirst, sleep, love), feed / play / pet, animations, home widget | next |
| 3+ | Day counter, heartbeat push, live drawing + widget, daily question, mood, shared piggy bank, distance, notes, calendar + special days, food wheel | later |
| 3+ | "Thinking of you" nudge, cat-care streak, milestone celebrations, time-capsule letters, bucket list | later |

Out of v1: love plant, photo album.

## Platform & tech

- Android first: Kotlin + Jetpack Compose + Material 3. iOS later.
- Backend: Firebase only (Auth, Firestore, Realtime Database for live features, Cloud Functions + FCM for push).
- Animations: generated with fal, played with Lottie / Rive.
- Monorepo: `android/`, `ios/`, `firebase/`, `assets/`, `tools/`, `docs/`.
