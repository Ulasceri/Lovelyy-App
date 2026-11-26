package com.lovelyy.app.data

import com.lovelyy.app.text.TurkishSuffix
import org.junit.Assert.assertEquals
import org.junit.Test

class CatNameTest {
    @Test fun newCatStartsWithDefaultName() {
        assertEquals("Mırmır", CatDoc().name)
        assertEquals(DEFAULT_CAT_NAME, CatDoc().name)
    }

    @Test fun blankOrMissingNameFallsBackToDefault() {
        assertEquals("Mırmır", displayCatName(null))
        assertEquals("Mırmır", displayCatName(""))
        assertEquals("Mırmır", displayCatName("   "))
    }

    @Test fun realNameIsKept() {
        assertEquals("Pamuk", displayCatName(" Pamuk "))
    }

    @Test fun suffixesWorkOnDefaultName() {
        assertEquals("Mırmır'ın", TurkishSuffix.genitive(DEFAULT_CAT_NAME))
        assertEquals("Mırmır'a", TurkishSuffix.dative(DEFAULT_CAT_NAME))
    }
}
