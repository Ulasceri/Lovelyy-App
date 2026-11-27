package com.lovelyy.app.text

import android.content.Context
import android.text.format.DateFormat
import com.lovelyy.app.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * The one place that knows which locale the UI formats dates, numbers and case in.
 *
 * The UI language is whatever language Android picked for our strings: the `app_language` resource holds
 * the language code of each `values-xx/strings.xml` (default `values/` = "tr"). When the device or per-app
 * language has a translation, that locale is used as is (keeping its region); when it doesn't, Android shows
 * the Turkish default strings and dates and name endings follow them, so an English phone never mixes
 * "December 11" into Turkish text. Adding a language needs only its `values-xx` folder and locales_config.
 * Nothing caches a locale: always call [current] at the point of use.
 */
object AppLocale {
    /** Used before [init] runs (unit tests, very early startup): the default strings' language. */
    private const val DEFAULT_LANGUAGE = "tr"

    @Volatile private var appContext: Context? = null

    /** Called once from LovelyyApplication.onCreate. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Language code of the strings Android is showing right now (e.g. "tr", later "en"). */
    fun uiLanguage(): String =
        appContext?.let { ctx -> runCatching { ctx.getString(R.string.app_language) }.getOrNull() }
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_LANGUAGE

    fun current(): Locale {
        val device = Locale.getDefault()
        val ui = uiLanguage()
        return if (device.language == ui) device else Locale.forLanguageTag(ui)
    }

    fun isTurkish(): Boolean = current().language == "tr"

    /** Skeleton to pattern; Android's DateFormat at runtime, swappable in JVM unit tests (no Android there). */
    internal var patternFor: (Locale, String) -> String = { locale, skeleton -> DateFormat.getBestDateTimePattern(locale, skeleton) }

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
