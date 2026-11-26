# CLAUDE.md

Instructions for Claude Code sessions on **Lovelyy**. Read this first, then the docs it links.

Language: talk to the user (Ulaş) in **Turkish**. Code, comments, docs and this file are English.
All user-facing UI strings are **Turkish** and live in `android/app/src/main/res/values/strings.xml`.

## Hard rules

- **README.md stays current.** Every change that affects features, setup or structure updates
  [README.md](README.md) in the same PR.
- **Design first.** Every new screen is DESIGNED by Fable (model `claude-fable-5-2`) as an artifact on the
  design canvas (https://claude.ai/artifact/FzNY4Jh2r7cyjayPKBFZk8) and APPROVED by Ulaş before it is
  built in the app. Never implement an undesigned screen.
- **Step by step.** Never build the whole app at once. Discuss design and colors with the user before building.
- **Follow [DESIGN.md](DESIGN.md)** for all UI: tokens, components, no hardcoded colors
  (also [docs/design-principles.md](docs/design-principles.md)).
- **Analytics** ([docs/analytics.md](docs/analytics.md)):
  - Every new feature screen adds its events as typed `AnalyticsEvent` entries and a screen name in
    `analytics/Screens.kt` (the `when` over `AppState` will not compile until you do).
  - No PII and no user id in events, params, breadcrumbs, keys or trace names. Respect the consent setting.
  - Log with Timber only (no `android.util.Log`, no `println`). Wrap performance-sensitive work in `trace("name") { }`.
  - Track from ViewModels/repositories after the action succeeded, never from composables.
- **Secrets.** Never commit secrets. fal key comes only from the `FAL_KEY` env var. `google-services.json`
  is gitignored.
- **Firebase projects.** Never touch the old project `lovelyy-ee6ed`. The native app uses `lovelyy-native`.
- **Product decisions:** shared pet, cat only, always adult. Shared piggy bank is removed from v1.
  Light theme only for v1. The decision wheel is general (what to eat / where to go / what to watch).
  (`docs/product.md` still lists the piggy bank and food wheel; this file wins.)
- **Git:** branch from latest `main`, small focused PRs, Turkish UI strings in `strings.xml`.

## Project overview and status

Android app for couples with a **shared cat** they care for together. Kotlin + Jetpack Compose +
Material 3, Firebase only (Auth, Firestore, Crashlytics, Analytics, Performance), monorepo.
iOS comes later (`ios/` is a stub). Product: [docs/product.md](docs/product.md).

**Built (designed + approved):** Karşılama, Giriş, Eşleşme, Anasayfa with the bottom nav (Yuva, Anılar,
Planlar, Biz), the cat and Kedi bakımı (streak, accessories), day counter, heartbeat, mood, daily question,
notes, calendar, bucket list, decision wheel, live drawing, time capsule, distance, profile/settings,
4 home-screen widgets, telemetry, and the second design round (mood/question/heartbeat/wheel history, search,
saved drawings, opened letters, accessories, inbox, account, unpair screen).
**Pending:** the final unpair / delete-account action waits for Ulaş's decision on what happens to shared data.
**Not designed yet = "Yakında" placeholder** (`home_soon` string).

## Architecture and where things live

Details: [docs/architecture.md](docs/architecture.md) (do not duplicate it here).

```
android/app/src/main/java/com/lovelyy/app/
  LovelyyApplication   hand-made DI container (lazy repositories), Telemetry.init()
  MainActivity         edge-to-edge, splash, LovelyyTheme -> LovelyyRoot
  analytics/           AnalyticsEvent (typed), AnalyticsTracker, Screens.kt (AppState -> screen name)
  telemetry/           Telemetry, TelemetryConsent, CrashReporter, CrashlyticsTree, PerfTrace
  data/                Repositories + models + pure rules (Auth, User, Pairing, Cat, Signals, ...), Firestore.kt (Paths)
  signals/             SignalCenter: local notifications + WorkManager (no FCM, Spark plan)
  widget/              CatWidget (RemoteViews, mood pictures)
  ui/AppViewModel      AppState from auth + Firestore listeners
  ui/LovelyyRoot       renders the screen for the current AppState
  ui/theme/            Color, Type (Quicksand), Shape, Theme
  ui/components/       Components.kt (buttons, text field, avatar), Design.kt (approved shared blocks)
  ui/{auth,profile,pairing,home,cat,heartbeat,setup,history,search,inbox,...}/
android/app/src/debug/   debug-only telemetry receiver
android/app/src/main/assets/cat/   14 mp4 clips (played by CatAnimator, ExoPlayer)
firebase/                firestore.rules, database.rules.json (deny all), functions/ (TS), tests/
tools/cat-animation/     fal.ai clip generation + ffmpeg encode (see tools/README.md)
assets/                  shared fonts/images; docs/  product, architecture, analytics, design
```

**Navigation has no NavController.** `AppState` (Loading, FirebaseMissing, SignedOut, NeedsName, Pairing,
Home, Failed) is computed in `AppViewModel` and `LovelyyRoot` cross-fades via `when (state)`. Inside Home,
`HomeScreen` has the bottom nav. A new top-level screen means a new `AppState` case plus its `Screens.kt` name.

Firestore layout: `users/{uid}`, `pairCodes/{CODE}`, `couples/{id}` (members, startDate) and
`couples/{id}/**` for all shared features (`cat/main`, `signals`, `moods`, `questions`, `notes`, `events`),
readable/writable by the two members only. Realtime Database is locked. Rules and tests must change
together (`firebase/firestore.rules` + `firebase/tests/firestore.rules.test.mjs`).

## Build, test, run

```bash
# Android (from android/)
./gradlew assembleDebug
./gradlew testDebugUnitTest -Dorg.gradle.jvmargs=-Xmx2048m
./gradlew assembleDebug -Ptelemetry.debug=true     # debug build with telemetry on

# Firestore rules tests (needs Java 11+)
cd firebase/tests && npm install && npm test

# Cat animation (needs Python 3, ffmpeg, FAL_KEY env var)
cd tools/cat-animation && python generate.py keys.json raw/ [clip] && ./encode.sh raw/
```

Without `android/app/google-services.json` the app still builds and shows the "Firebase kurulumu
gerekiyor" screen. Stack: AGP 8.10.1, Kotlin 2.1.21, compileSdk/targetSdk 35, minSdk 26, Java 17,
Compose BOM 2025.06.00, Firebase BoM 33.15.0 (versions in `android/gradle/libs.versions.toml`).

### Environment notes

- **The cloud container cannot build Android** (no SDK, `dl.google.com` blocked). Builds and emulator
  tests are done on the user's Windows PC. Do not try to install the SDK here; the firebase rules tests
  and Python tools can run in the container.
- The user's Windows profile path contains a non-ASCII character, so on the PC set
  `JAVA_HOME` to Android Studio's `jbr` folder and `TEMP`/`TMP` to an ASCII folder (e.g. `C:\gtmp`).
- Gradle **unit tests only run from an ASCII-path copy** of the repo (`android.overridePathCheck=true`
  lets builds work, tests still fail on the non-ASCII path).
- Pure Kotlin logic (rules, date math) is unit-tested in `android/app/src/test/` (`CatRulesTest`,
  `CalendarMathTest`, `DayCounterTest`, `AnalyticsEventTest`). Add tests for new logic.

## Code conventions

- Package-by-layer under `com.lovelyy.app`: `data/` (Firebase + rules), `ui/<feature>/` (screens + ViewModel),
  `ui/components` (shared), `analytics/`, `telemetry/`.
- **Repository:** class taking `FirebaseFirestore`/`FirebaseAuth` in the constructor; exposes `Flow` via
  `callbackFlow` for listeners and `suspend` functions (`.await()`) for actions; multi-field shared
  state changes run in Firestore **transactions**. Register it as a `by lazy` property in `LovelyyApplication`.
  Collection names come from `Paths` in `data/Firestore.kt` and must match the rules.
- **Time-based state without a server:** store a snapshot + timestamp, compute current values on the client
  (`CatRules`, `CalendarMath`); keep that logic pure so it is testable.
- **ViewModel:** `XViewModel(app: LovelyyApplication, ...) : ViewModel()`, created with `viewModel { ... }`;
  state as `StateFlow` (`stateIn`), one-time effects through a `Channel` + `receiveAsFlow()`; collect in
  composables with `collectAsStateWithLifecycle()`. Get the app via `lovelyyApp()`.
- **Compose:** `XScreen`/`XParts` files per feature; use `MaterialTheme.colorScheme/typography/shapes`
  only, shared components from `ui/components`, strings via `stringResource` (Turkish, in `strings.xml`).
  Soft, rounded, one main action per screen, light haptics and micro-animations.
- Errors: `Timber.e(e, "...")`, show a friendly Turkish message; never swallow `CancellationException`.
- Kotlin official style; KDoc comments explain *why*; keep files focused.
- Pairing codes: 6 chars from 2-9, valid 30 min in-app (rules allow up to 24h).
- No server code on the Spark plan: no Cloud Functions in the critical path (push via FCM needs Blaze).

## Links

[README.md](README.md) (setup, Turkish) · [DESIGN.md](DESIGN.md) (UI tokens/components) ·
[docs/architecture.md](docs/architecture.md) · [docs/analytics.md](docs/analytics.md) ·
[docs/design-principles.md](docs/design-principles.md) · [docs/product.md](docs/product.md) ·
[tools/README.md](tools/README.md)
