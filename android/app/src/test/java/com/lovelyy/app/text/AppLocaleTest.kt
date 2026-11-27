package com.lovelyy.app.text

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class AppLocaleTest {
    private val original = Locale.getDefault()
    private val date = LocalDate.of(2024, 2, 14)
    private val originalPattern = AppLocale.patternFor

    init {
        // android.text.format.DateFormat is a stub on the JVM: use fixed patterns.
        AppLocale.patternFor = { _, sk ->
            when (sk) { "dMMMMy" -> "d MMMM y"; "EEEE" -> "EEEE"; else -> "d MMMM" }
        }
    }

    @After
    fun restore() {
        Locale.setDefault(original)
        AppLocale.supportedLanguages = setOf("tr")
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
        val formatted = date.format(AppLocale.formatter("dMMMMy"))
        assertTrue(formatted, formatted.contains("Şubat"))
        assertTrue(formatted, !formatted.contains("February"))
    }

    @Test
    fun cacheDoesNotLeakAcrossDeviceLocales() {
        Locale.setDefault(Locale.US)
        val first = date.format(AppLocale.formatter("EEEE"))
        Locale.setDefault(Locale.FRANCE)
        assertEquals(first, date.format(AppLocale.formatter("EEEE")))
        assertEquals("Çarşamba", first)
    }

    @Test
    fun translatedLanguageIsRespected() {
        AppLocale.supportedLanguages = setOf("tr", "en")
        Locale.setDefault(Locale.US)
        assertEquals("en", AppLocale.current().language)
        assertTrue(date.format(AppLocale.formatter("dMMMMy")).contains("February"))
    }
}
