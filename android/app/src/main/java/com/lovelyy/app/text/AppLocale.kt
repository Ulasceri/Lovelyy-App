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
 * Turkish is the only shipped language today; a new language needs no change in this file.
 */
object AppLocale {
    fun current(): Locale = Locale.getDefault()

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
