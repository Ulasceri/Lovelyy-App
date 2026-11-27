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
`start_date_set`, `counter_shared`, `heartbeat_sent` (`emotion`), `nudge_sent`, `mood_shared` (`mood`, `has_note`), `question_answered`, `note_added`, `plan_added` (`yearly`), `location_sharing_changed` (`sharing`), `location_updated` (`city_only`), `profile_name_changed`, `dream_added` (`category`), `dream_completed`, `wheel_spun` (`built_in`, `items`), `wheel_created`, `capsule_sealed` (`days`), `drawing_saved` (`strokes`), `drawing_deleted`, `inbox_read_all`, `password_reset_sent`, `couple_unpaired`, `account_deleted`, `intro_finished` (`skipped`, `demos_tried`), `permission_primer_answered` (`permission` = `notifications`/`location`, `allowed`), `streak_explainer_shown` (`source` = `auto` (by itself, the first time the couple's streak reached 1), `chip` (Yuva streak chip) or `card` (Kedi bakımı streak card)), Lovelyy Plus: `trial_started`, `paywall_shown` (`source` = `trial_end`, `lock_<feature>`, `home_card`, ...), `purchase_started` / `purchase_completed` (`product` = `yearly`/`monthly`/`gift`), `purchase_failed` (`reason` = `cancelled`, `pending`, `not_loaded`, `blocked`, `record_failed` or `play_<code>`), `restore_tapped` (`found`), `gift_sent` (`has_note`), `takeover_shown`, and `screen_view`. Plus events never carry prices, purchase tokens or the gift note. Location events never carry coordinates or a city; dream, wheel, capsule and drawing events never carry titles, options, letter text or drawings. The home-screen widget's heart logs `heartbeat_sent` like the app does.
Screen names: `login`, `onboarding_intro`, `name_setup`, `pairing`, `paired_onboarding`, `home`, `error`, `firebase_setup`, and the inner screens opened from home
(`analytics/Screens.kt` → `InnerScreens`, logged by `HomeScreen`): `day_counter`, `heartbeat`, `mood`, `daily_question`, `notes`, `plans` (the Planlar tab), `cat_care`, `profile` (the Biz tab), `distance`, `dreams`, `wheel`, `time_capsule`, `live_drawing`, `memories` (the Anılar tab), `widgets`, `mood_history`, `question_history`, `heartbeat_history`, `wheel_history`, `search`, `saved_drawings`, `opened_letters`, `inbox`, `account`, `unpair`, `plus_management`, `plus_gift`, and the Plus overlays `plus_paywall`, `plus_lock`, `plus_celebration`, `plus_takeover`; `plus_trial_gift` is the P1 step of the post-pairing flow. Search never logs what was typed. Bottom sheets are not screens and log no `screen_view`; the "Seri nedir?" sheet is counted by `streak_explainer_shown`.

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
`FirebasePerformance.isPerformanceCollectionEnabled`. The "Kullanım verisi" switch in the Biz tab calls it. Opting out also resets the analytics app-instance id.

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
