package com.lovelyy.app.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TurkishSuffixTest {
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
}
