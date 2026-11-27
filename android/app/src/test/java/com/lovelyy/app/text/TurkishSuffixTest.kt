package com.lovelyy.app.text

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class TurkishSuffixTest {
    private val original = Locale.getDefault()

    @Before
    fun turkishUi() = Locale.setDefault(Locale.forLanguageTag("tr-TR"))

    @After
    fun restore() = Locale.setDefault(original)

    @Test
    fun unshippedLanguageFallsBackToTurkish() {
        // Only Turkish strings ship, so an English phone still reads Turkish text and needs Turkish endings.
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Elif'e", TurkishSuffix.dative("Elif"))
        assertEquals("tr", AppLocale.current().language)
    }

    @Test
    fun dative() {
        assertEquals("Elif'e", TurkishSuffix.dative("Elif"))
        assertEquals("Ulaş'a", TurkishSuffix.dative("Ulaş"))
        assertEquals("Ayşe'ye", TurkishSuffix.dative("Ayşe"))
        assertEquals("Doğa'ya", TurkishSuffix.dative("Doğa"))
        assertEquals("Gül'e", TurkishSuffix.dative("Gül"))
    }

    @Test
    fun genitive() {
        assertEquals("Elif'in", TurkishSuffix.genitive("Elif"))
        assertEquals("Ulaş'ın", TurkishSuffix.genitive("Ulaş"))
        assertEquals("Ayşe'nin", TurkishSuffix.genitive("Ayşe"))
        assertEquals("Doğu'nun", TurkishSuffix.genitive("Doğu"))
        assertEquals("Gül'ün", TurkishSuffix.genitive("Gül"))
        assertEquals("Can'ın", TurkishSuffix.genitive("Can"))
    }

    @Test
    fun ablative() {
        assertEquals("Perşembe'den", TurkishSuffix.ablative("Perşembe"))
        assertEquals("Pazar'dan", TurkishSuffix.ablative("Pazar"))
        assertEquals("Salı'dan", TurkishSuffix.ablative("Salı"))
        assertEquals("Ahmet'ten", TurkishSuffix.ablative("Ahmet"))
    }

    @Test
    fun ablativeOfYears() {
        assertEquals("9 Ekim 2025'ten", TurkishSuffix.ablative("9 Ekim 2025"))
        assertEquals("2010'dan", TurkishSuffix.ablative("2010"))
        assertEquals("2020'den", TurkishSuffix.ablative("2020"))
        assertEquals("2000'den", TurkishSuffix.ablative("2000"))
        assertEquals("2026'dan", TurkishSuffix.ablative("2026"))
        assertEquals("1940'tan", TurkishSuffix.ablative("1940"))
    }

    @Test
    fun locative() {
        assertEquals("9 Ekim 2027'de", TurkishSuffix.locative("9 Ekim 2027"))
        assertEquals("16 Ocak 2026'da", TurkishSuffix.locative("16 Ocak 2026"))
        assertEquals("Ankara'da", TurkishSuffix.locative("Ankara"))
        assertEquals("Ahmet'te", TurkishSuffix.locative("Ahmet"))
    }

    @Test
    fun accusative() {
        org.junit.Assert.assertEquals("Elif'i", TurkishSuffix.accusative("Elif"))
        org.junit.Assert.assertEquals("Ulaş'ı", TurkishSuffix.accusative("Ulaş"))
        org.junit.Assert.assertEquals("Ayşe'yi", TurkishSuffix.accusative("Ayşe"))
        org.junit.Assert.assertEquals("Doğu'yu", TurkishSuffix.accusative("Doğu"))
        org.junit.Assert.assertEquals("Gül'ü", TurkishSuffix.accusative("Gül"))
    }
}
