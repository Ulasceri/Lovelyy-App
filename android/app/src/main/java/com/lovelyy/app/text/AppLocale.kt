package com.lovelyy.app.text

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * The one place that knows which locale the UI formats dates, numbers and case in.
 *
 * Dates must be in the same language as the UI strings, and the UI strings only exist in the languages in
 * [supportedLanguages] (keep in sync with res/xml/locales_config.xml and the res/values-xx folders). So [current]
 * is the process default (which Android points at the per-app language on API 33+, or the system language) only
 * when the app has a translation for it; otherwise it is Turkish, the language the resources fall back to.
 * Nothing here caches a locale: always call [current] at the point of use.
 * A new language needs only its entry in [supportedLanguages] (plus the resources).
 */
object AppLocale {
    private val FALLBACK: Locale = Locale.forLanguageTag("tr-TR")

    /** Languages that have app strings. Internal so tests can simulate a translated language. */
    @Volatile
    internal var supportedLanguages: Set<String> = setOf("tr")

    fun current(): Locale {
        val device = Locale.getDefault()
        return if (device.language in supportedLanguages) device else FALLBACK
    }

    fun isTurkish(): Boolean = current().language == "tr"

    /** Skeleton -> pattern. Internal so plain JVM tests (where android.text.format is a stub) can replace it. */
    @Volatile
    internal var patternFor: (Locale, String) -> String = { l, sk -> DateFormat.getBestDateTimePattern(l, sk) }

    private val cache = ConcurrentHashMap<Pair<Locale, String>, DateTimeFormatter>()

    /**
     * A date/time formatter for a CLDR skeleton such as "dMMMM" (day + month name), "dMMMMy" (with year),
     * "dMMMMyEEEE" (with weekday), "LLLLy" (standalone month + year), "EEEE" or "jm" (time in the locale's
     * preferred 24 or 12 hour style). The skeleton is expanded to the right order and punctuation for the
     * locale, so no screen writes month names or "d MMMM" patterns by hand.
     */
    fun formatter(skeleton: String): DateTimeFormatter {
        val locale = current()
        return cache.getOrPut(locale to skeleton) {
            runCatching { DateTimeFormatter.ofPattern(patternFor(locale, skeleton), locale) }
                .getOrElse { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
        }
    }
}
