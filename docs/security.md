# Security

How Lovelyy protects its data on the free Spark plan (no server code), what has to be done in the
Firebase / Google Cloud console, and what is left for later. Rules: `firebase/firestore.rules`, tests:
`firebase/tests/firestore.rules.test.mjs` (`cd firebase/tests && npm install && npm test`).

## Threat model in one paragraph

Anyone can download the app, read the public Firebase config out of the APK and talk to Firestore
directly with a script. They can also make two accounts and pair them, so "only members can write"
alone does not stop a stranger from writing whatever they like into their own couple, or from looping
writes until the project's daily free quota (Spark: 20k writes, 50k reads) runs out, which would take
the app down for everyone. So the defence is in layers:

1. **Firestore rules**: who may read and write what, and exactly which fields with which sizes.
2. **App Check**: only the real app on a real device gets a token; scripts are refused.
3. **Rate limiting**: the one feature that pings the partner (signals) is limited on the server.
4. **Console settings**: API key restrictions, email enumeration protection, budget alert.

## 1. Firestore rules (row-level security)

- `users/{uid}`: only me and my partner can read it; only I edit my profile; a partner-to-be can only
  link me to our new couple. No listing. Name max 40, photo URL max 1000 characters.
- `pairCodes/{CODE}`: single reads only (knowing the code is the secret), no listing, 24 h expiry.
- `couples/{id}`: only the two members; membership and createdAt never change; ending is one-shot.
- `couples/{id}/**`: only the two members read or delete. **Every collection has its own write rule**:
  - exact field list (`keys().hasOnly(...)`), types, size limits matching the app's own limits
    (note 1000, capsule 4000, answer 500, titles 24-80, wheel max 12 items of 20, stroke max 1000 numbers...);
  - the author (`by`, `from`, `uid`, `setBy`) must be the signed-in user and `by/byName/at` never change;
  - an edit of a note/plan/dream stamps `updatedBy` with the editor's own uid;
  - per-person docs (`moods/{uid}`, `moodLog/{uid}_{day}`, `locations/{uid}`, `limits/{uid}`,
    the own key in `drawing/presence`) can only be written by that person;
  - Günün sorusu: I can only write my own answer, and once both answered the answers are locked
    (server-side now, same rule as `QuestionMath.isLocked`);
  - signals, capsules, strokes, saved drawings and wheel history are write-once (no edits);
  - a collection without a match block cannot be written at all, and after unpairing nothing new
    can be written, only deleted.
- Realtime Database: deny all (`database.rules.json`).

**When you add a shared collection or a field, update the rules and the tests in the same PR**, or the
app's writes will fail with PERMISSION_DENIED.

## 2. App Check (Play Integrity)

Code: `security/AppCheckSetup.kt` (installed in `LovelyyApplication` before any Firestore call), provider
per build type in `src/debug` (debug token) and `src/release` (Play Integrity). Free on Spark
(Play Integrity standard quota is 10,000 calls a day; tokens are cached and refreshed about hourly).

Console steps (in this order; **do not enforce before step 5**, or the app stops working):

1. Firebase Console → **App Check** → Apps → `com.lovelyy.app` → **Play Integrity** → Register.
   Use the SHA-256 of the **release / Play app signing** key (Play Console → Test and release → App
   integrity → App signing key certificate). The repo's debug key is not used for Play Integrity.
2. Play Console → App integrity → link the same Google Cloud project (`lovelyy-native`) for Play Integrity API.
3. Debug builds: run the app once, find `DebugAppCheckProvider` in logcat ("Enter this debug secret ..."),
   and add that token under App Check → Apps → ⋮ → **Manage debug tokens**. Each new emulator /
   reinstall prints a new token. Never put a debug token in the repo.
4. Leave it **unenforced** and watch App Check → APIs → Cloud Firestore → "Verified requests" for a few days
   (release build installed from the Play internal testing track, plus debug builds with registered tokens).
5. When nearly all requests are verified: **Enforce** for Cloud Firestore and Realtime Database.
   Old app versions without App Check will then be refused, which is fine before launch.

## 3. Rate limiting

- **Server (rules)**: every signal (heartbeat, nudge, mood/note/plan/question/wheel notice) is written in a
  batch together with `couples/{id}/limits/{me}` = `{signalAt: serverTimestamp()}`. The rules accept the
  signal only if that stamp is in the same write, and accept the stamp only if the previous one is more
  than 2 seconds old. Server time, so the phone's clock doesn't matter. A queued offline signal that syncs
  within 2 s of another is refused (rare; the sender sees "couldn't send").
- **Phone**: `SignalRepository` refuses a second send within 2.1 s from any screen or the widget
  (`SignalThrottledException`), on top of the screens' own cooldowns (heartbeat 3 s, nudge 8 s).
- Other writes are naturally bounded (one doc per person, per day or per action) and protected by
  App Check. A per-hour or per-day quota for every write needs a server (see below).

## 4. Console checklist (Spark, no cost)

- [ ] Publish the latest `firebase/firestore.rules` (Firestore → Rules → paste → Publish), then run the app
      through every screen once on the emulator to see nothing is denied.
- [ ] App Check as in section 2.
- [ ] Google Cloud Console → APIs & Services → Credentials → the Android API key: **Application restriction:
      Android apps** (`com.lovelyy.app` + debug SHA-1 + release / Play signing SHA-1) and **API restrictions**
      to the Firebase APIs the app uses (Identity Toolkit, Token Service, Cloud Firestore, Firebase Installations,
      Firebase App Check, Firebase Crashlytics, Firebase Performance, Google Analytics / Firebase Remote Config
      if used). Check the app still signs in after the change.
- [ ] Authentication → Settings → **Email enumeration protection**: on.
- [ ] Authentication → Settings → **User account linking**: one account per email (default).
- [ ] Authentication → Templates: set the sender name and the Turkish language for password reset mails.
- [ ] Google Cloud → Billing is not enabled on Spark, so there is no bill; if Blaze is enabled later, set a
      **budget alert** first.

## 5. Android app

- `android:allowBackup="false"`: the signed-in session and local drafts are not copied to cloud backups
  or to another device.
- No exported components except the launcher activity; widget receivers are not exported.
- Only coarse location, rounded before upload (0.1° city-only or 0.01°).
- Before the Play release: turn on R8 (`isMinifyEnabled = true` + keep rules) and test the release build;
  sign with Play App Signing; never commit the upload keystore.

## Possible on Spark later (data model changes, no server)

These were thought to need Cloud Functions, but rules can do them if the data is split into separate docs:

- **Günün sorusu privacy**: store each answer as `questions/{day}/answers/{uid}` and allow reading the
  partner's doc only if mine exists (`exists(.../answers/$(request.auth.uid))`). Then the partner's answer
  is really hidden until I answer, not only in the app.
- **Time capsule lock without trusting the phone clock**: store `openOn` as a timestamp and the letter body
  in `capsules/{id}/body/main`, readable only when `request.time >= openOn`. The title stays visible.

Both need app changes (repositories + history screens) and a migration of existing docs.

## Needs Blaze (Cloud Functions) — parked

- FCM push from the server (background heartbeat currently arrives in 2-4 minutes via WorkManager).
- Real per-user write quotas across all collections (counting writes in a trusted function).
- Server-side cleanup jobs (e.g. deleting leftover data of deleted accounts, old signals).
- Abuse reporting / moderation hooks, if ever needed.
