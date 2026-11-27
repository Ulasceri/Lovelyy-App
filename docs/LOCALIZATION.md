# Localization readiness

Status: the app is **ready to receive new languages**; no translation has been added. Turkish (`values/strings.xml`)
stays the default and the only shipped language.

> Build and unit tests were **not run** for this change (the cloud container cannot build Android). See "Verification".

## What was done

The codebase was already almost fully resourced (about 800 strings in `values/strings.xml`, used through
`stringResource` / `context.getString`, including widgets, notifications, the notification channel and Toasts).
The audit (Compose `Text`, `contentDescription`, Toast, notifications, widgets `RemoteViews`, ViewModels, data classes,
enums) found only a handful of literal UI strings; the rest of the work was making formatting locale-aware.

| Area | Change |
|---|---|
| Plurals | 48 count strings (days, minutes, hours, km, notes, drawings, letters, streaks, "N more", ...) became `<plurals>` with `one`/`other` (same Turkish text). Compose uses `pluralStringResource`, widgets/notifications use `resources.getQuantityString`. Ordinals ("%1$d. gün") stay plain strings. |
| Literals moved | Weekday header of the calendar (`cal_weekdays_short` string-array), letter quote marks (`ol_quote`), the cat need-bar `contentDescription` (reuses `cc_percent`). |
| Dates / time | New `text/AppLocale.kt`: `AppLocale.current()` (never cached) and `AppLocale.formatter(skeleton)`, built on `DateFormat.getBestDateTimePattern`. All hardcoded `Locale("tr")` / `"d MMMM yyyy"` patterns were replaced, the per-file `TR` constants removed, top-level formatters became getters so a language change is picked up. Month/weekday names and word order now follow the locale. |
| Case / numbers | `uppercase/lowercase/titlecase` and `NumberFormat` use `AppLocale.current()` (matters for Turkish dotted/dotless i vs other languages). Geocoder (city names) uses the current locale. |
| UI language | `AppLocale.current()` follows the language of the strings Android shows: the `app_language` string (`tr` in `values/`). If the device or per-app language has a `values-xx`, that locale is used; otherwise the Turkish default strings show and dates/name endings follow them (no English dates inside Turkish text). |
| Turkish grammar | `TurkishSuffix` ("Elif'e", "Ulaş'ın", "Perşembe'den") only applies when the UI language is Turkish; any other language gets the plain name, so a translation just writes `%1$s`. Unit test added for this. |
| Per-app language | `res/xml/locales_config.xml` (only `tr` for now) referenced from the manifest via `android:localeConfig`. |
| Widgets / channel | `widget/LocaleChangedReceiver` (manifest, `LOCALE_CHANGED`) redraws all widgets and refreshes the notification channel name/description. |

## Adding a language (checklist)

1. Create `android/app/src/main/res/values-xx/strings.xml` (e.g. `values-en`) with every key of `values/strings.xml`,
   including each `<plurals>` (use all quantities the language needs, e.g. `few`/`many` for Polish or Russian) and the
   `cal_weekdays_short` array (Monday first). Set `app_language` to the language code (`en`); dates and
   case follow it automatically.
2. Add `<locale android:name="xx" />` to `res/xml/locales_config.xml`. The user then picks the language in
   system Settings > Apps > Lovelyy > Language (Android 13+).
3. Android 12 and below: per-app language needs AppCompat (`AppCompatDelegate.setApplicationLocales`) plus
   `AppLocalesMetadataHolderService` autoStoreLocales; not set up yet. Until then the system language applies.
4. Fonts: Quicksand covers Latin only (plus Latin Extended partially). Cyrillic, Greek, Arabic, Hebrew, CJK, Devanagari
   etc. need a fallback font family or a different font for that locale (`ui/theme/Type.kt`).
5. Right-to-left languages: `supportsRtl="true"` is set, but screens use fixed-direction drawings (SVG paths, arrows,
   the wheel) and some `Alignment.Start/End` choices have not been checked in RTL. Review visually first.
6. Translate the 60 daily questions once they are resourced (see "Remaining" 1).
7. Set the Play Store listing and `app_name` per language if wanted.

## Remaining (not fixed, on purpose)

1. **Daily question bank** (`data/Questions.kt`, 60 Turkish questions) is hardcoded in Kotlin and the chosen text is
   written into Firestore (`couples/{id}/questions/{day}.question`) by `QuestionRepository`. Moving it to a
   `string-array` needs the repository to get a `Context` or to store only the index and resolve the text at display
   time (keep the order and size identical in every language, the index is `epochDay mod size`). Touches Firestore data
   and tests (`CalendarMathTest`, `CoupleDayTest`), so it was left for a separate PR.
2. **Data, not UI**: default cat name `Mırmır` (`DEFAULT_CAT_NAME`, stored name falls back to it), built-in wheel ids
   (`yemek`, `gezi`, `izle`, ids not labels), default event emoji, signal text such as `"<wheel> → <result>"` (stored in
   Firestore and sent to the partner, in the sender's language), user-written notes, titles, answers, city names.
   Stored content stays in the language it was written in.
3. Partner-visible notification text is built on the **receiver's** device from the signal kind (good), but the free text
   inside it (event title, wheel result) is the sender's.
4. Some abbreviated units are still translated as one phrase: `ds_duration` ("%1$d sa %2$d dk", hours + minutes) and
   `dc_unit_*` (countdown unit labels). A translator can handle them, but they are not plural-aware.
5. Calendar grid is Monday-first for every locale (`cal_weekdays_short` is Monday-first); `HistoryScreens` truncates
   a formatted date with `take(6)`. Both are fine in Turkish and should be revisited per language.
6. Digits: `String.format`/`%02d` clocks use the default locale, so e.g. Arabic gets Arabic-Indic digits.
7. Analytics event/screen names, Timber logs, exception messages are English by design and are not UI.

## Verification

- Done: static checks only. Every `R.string.*`, `R.plurals.*`, `R.array.*` reference resolves to a key in
  `values/strings.xml`; XML files parse.
- **Not run**: `./gradlew assembleDebug` and `testDebugUnitTest` (no Android SDK in the cloud container). Please run both.
  Compile risks to check: `pluralStringResource` imports and Int/Long counts (`.toInt()` was added everywhere),
  places where `pluralStringResource` sits inside a non-composable lambda, unused or leftover imports after removing
  `TR` and `java.util.Locale`/`DateTimeFormatter` imports, `AppLocale` import in every touched file,
  `android:localeConfig` needs compileSdk 33+ (it is 35).
- Behaviour risk to eyeball in Turkish: skeleton-based dates should give the same text as before (e.g. "9 Ekim 2025",
  "23 Kasım Pazartesi", "Ekim 2025", time "14:20"); the month chip no longer truncates to 3 letters (Turkish months
  are already 3 letters in the abbreviated form).
