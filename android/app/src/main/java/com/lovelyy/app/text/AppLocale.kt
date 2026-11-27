package com.lovelyy.app.text

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * The one place that knows which locale the UI formats dates, numbers and case in.
 *
 * It follows the process default, which Android points at the per-app language (API 33+) or the
 * system language, so nothing here caches a locale: always call [current] at the point of use.
 * Turkish is the only shipped language today; a new language is added to [SHIPPED].
 */
object AppLocale {
    /**
     * Languages the app ships strings for. On any other device language Android falls back to the Turkish
     * `values/` strings, so dates and name suffixes must fall back to Turkish too; otherwise an English phone
     * shows "December 11 kadar" and "Bora Plus hediye et". Add a language here when its `values-xx` lands.
     */
    private val SHIPPED = setOf("tr")
    private val FALLBACK: Locale = Locale.forLanguageTag("tr-TR")

    fun current(): Locale = Locale.getDefault().let { if (it.language in SHIPPED) it else FALLBACK }

    fun isTurkish(): Boolean = current().language == "tr"

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
            runCatching { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale) }
                .getOrElse { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
        }
    }
}
