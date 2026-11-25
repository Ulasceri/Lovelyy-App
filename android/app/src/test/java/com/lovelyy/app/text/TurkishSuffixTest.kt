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
}
