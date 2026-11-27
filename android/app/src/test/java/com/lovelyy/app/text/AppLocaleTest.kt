package com.lovelyy.app.text

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Dates follow the language of the shown strings, not the device language. In JVM tests AppLocale.init is not
 * called, so the UI language is the default strings' language ("tr").
 */
class AppLocaleTest {
    private val original = Locale.getDefault()
    private val originalPattern = AppLocale.patternFor
    private val date = LocalDate.of(2024, 2, 14)

    init {
        // android.text.format.DateFormat is a stub on the JVM. Set fixed patterns before the first formatter() call.
        // The skeletons here are only used by this test, so the shared formatter cache can't hand back another
        // test's entry.
        AppLocale.patternFor = { _, skeleton ->
            when (skeleton) {
                "yMMMMd" -> "d MMMM y"
                "cccc" -> "EEEE"
                else -> "d MMMM"
            }
        }
    }

    @After
    fun restore() {
        Locale.setDefault(original)
        AppLocale.patternFor = originalPattern
    }

    @Test
    fun deviceLanguageWithoutTranslationFallsBackToTurkish() {
        Locale.setDefault(Locale.US)
        assertEquals("tr", AppLocale.current().language)
        assertTrue(AppLocale.isTurkish())
    }

    @Test
    fun englishDeviceStillGetsTurkishDates() {
        Locale.setDefault(Locale.US)
        val formatted = date.format(AppLocale.formatter("yMMMMd"))
        assertTrue(formatted, formatted.contains("Şubat"))
        assertFalse(formatted, formatted.contains("February"))
    }

    @Test
    fun cacheDoesNotLeakAcrossDeviceLocales() {
        Locale.setDefault(Locale.US)
        val first = date.format(AppLocale.formatter("cccc"))
        Locale.setDefault(Locale.FRANCE)
        assertEquals(first, date.format(AppLocale.formatter("cccc")))
        assertEquals("Çarşamba", first)
    }

    @Test
    fun turkishDeviceKeepsItsOwnLocale() {
        val trTR = Locale.forLanguageTag("tr-TR")
        Locale.setDefault(trTR)
        assertEquals(trTR, AppLocale.current())
    }
}
