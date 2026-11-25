package com.lovelyy.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventTest {

    private val all = listOf(
        AnalyticsEvent.PairingCompleted,
        AnalyticsEvent.CatFed,
        AnalyticsEvent.CatWatered,
        AnalyticsEvent.CatPlayed,
        AnalyticsEvent.CatPetted(strokes = 4),
        AnalyticsEvent.CatSlept,
        AnalyticsEvent.ScreenView("home"),
    )

    @Test
    fun everyEventFollowsTheNamingRules() {
        for (event in all) {
            assertEquals("${event.name}: ${EventNames.problems(event)}", emptyList<String>(), EventNames.problems(event))
        }
    }

    @Test
    fun eventNamesAreUnique() {
        val names = all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun paramValuesAreOnlySimpleTypes() {
        for (event in all) for (value in event.params.values) {
            assertTrue("${event.name}: $value", value is String || value is Number || value is Boolean)
        }
    }

    @Test
    fun badNamesAreCaught() {
        assertTrue(EventNames.problems("Cat-Fed", emptyList()).isNotEmpty())
        assertTrue(EventNames.problems("cat_fed_by_a_partner_who_really_loves_the_cat", emptyList()).isNotEmpty())
        assertTrue(EventNames.problems("firebase_thing", emptyList()).isNotEmpty())
        assertTrue(EventNames.problems("cat_fed", listOf("Bad Key")).isNotEmpty())
    }
}
