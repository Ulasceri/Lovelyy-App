# Analytics, logging and crash reporting

Everything goes through `com.lovelyy.app.telemetry.Telemetry`, set up once in `LovelyyApplication.onCreate()`.

| Piece | Where | What |
|---|---|---|
| Analytics | `analytics/AnalyticsEvent.kt`, `AnalyticsTracker`, `FirebaseAnalyticsTracker` | Typed events sent to Google Analytics for Firebase |
| Screen views | `analytics/Screens.kt`, hooked in `AppViewModel` | One `screen_view` per screen change (route name only) |
| Crash reports | `telemetry/CrashReporter.kt` | `logNonFatal`, `breadcrumb`, `setKey` on Crashlytics |
| Logging | Timber, `telemetry/CrashlyticsTree.kt` | Single logging API for the whole app |
| Performance | `telemetry/PerfTrace.kt` + Perf plugin | Automatic app-start/screen/network traces, plus `trace("name") { }` |
| Consent | `telemetry/TelemetryConsent.kt` | One switch for all collection |

## Privacy rules (always)

- No personal data in events, params, breadcrumbs, keys or trace names: no names, emails, uids,
  couple ids, pairing codes, notes, answers or any free text.
- Never call `setUserId` on Analytics or Crashlytics.
- Advertising id collection is off (`google_analytics_adid_collection_enabled=false`).

## Event naming

- `snake_case`, `object_action` in past tense: `cat_fed`, `pairing_completed`, `note_added`.
- At most 40 characters. Don't start with `firebase_`, `google_` or `ga_`.
- Param keys `snake_case` (max 40 chars, max 25 params). Values: String, Long/Int, Double, Boolean (sent as 0/1).
- Prefer a small number of events with params over many near-duplicate events.

Current events: `pairing_completed`, `cat_fed`, `cat_watered`, `cat_played`, `cat_petted` (`strokes`), `cat_slept`,
`start_date_set`, `counter_shared`, `heartbeat_sent` (`emotion`), `nudge_sent`, `mood_shared` (`mood`, `has_note`), `question_answered`, `note_added`, `plan_added` (`yearly`), and `screen_view`.
Screen names: `login`, `name_setup`, `pairing`, `home`, `error`, `firebase_setup`, and the inner screens opened from home
(`analytics/Screens.kt` → `InnerScreens`, logged by `HomeScreen`): `day_counter`, `heartbeat`, `mood`, `daily_question`, `notes`, `plans` (the Planlar tab).

### Adding an event

1. Add a `data object` (or `data class` with params) to `AnalyticsEvent`.
2. Add it to the list in `AnalyticsEventTest` (checks the naming rules).
3. Call `Telemetry.track(AnalyticsEvent.YourEvent)` from a ViewModel or repository when the action
   has actually succeeded. Not from composables.
4. A new screen: add the `AppState` case to `Screens.kt` (the `when` won't compile until you do).

## Logging

Use Timber everywhere (never `android.util.Log` or `println`):

```kotlin
Timber.d("debug detail")                 // logcat in debug only
Timber.w(e, "Signal check failed")       // + Crashlytics breadcrumb
Timber.e(e, "Cat action failed")         // + breadcrumb + non-fatal report
```

| Build | Logcat | Crashlytics |
|---|---|---|
| debug | everything (`Timber.DebugTree`) | WARN/ERROR breadcrumbs, ERROR+exception as non-fatal, only when collection is on |
| release | nothing | WARN/ERROR breadcrumbs, ERROR+exception as non-fatal |

## Turning collection on and off

`TelemetryConsent` stores the boolean `telemetry_enabled`. When nothing is stored the default is
`BuildConfig.TELEMETRY_DEFAULT_ENABLED`: **on in release, off in debug**. Changing it
(`Telemetry.consent.setEnabled(false)`) immediately calls `setAnalyticsCollectionEnabled`,
`setCrashlyticsCollectionEnabled` (and deletes unsent crash reports when turning off) and
`FirebasePerformance.isPerformanceCollectionEnabled`. A future privacy setting only needs that call.

The manifest disables all three by default so nothing is collected before the app has read the choice;
Firebase remembers the value the app sets, so from the second launch on, early events (app start trace,
early crashes) follow the user's choice too.

### Testing from a debug build

Either build with collection on by default:

```
./gradlew assembleDebug -Ptelemetry.debug=true
```

or switch it at runtime with the debug-only receiver (`src/debug`, not in release builds, adb only):

```
adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.TELEMETRY --ez enabled true
adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.TELEMETRY   # back to default
adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.PING          # logs debug_ping
adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.EVENT --es name cat_fed
adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.NON_FATAL
adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.CRASH         # then reopen the app to upload
```

Watching it work:

```
adb shell setprop debug.firebase.analytics.app com.lovelyy.app   # DebugView in the Firebase console
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb logcat -s FA FA-SVC FirebaseCrashlytics FirebasePerformance
```

Turn DebugView off again with `adb shell setprop debug.firebase.analytics.app .none.`
