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
        AnalyticsEvent.StartDateSet,
        AnalyticsEvent.CounterShared,
        AnalyticsEvent.HeartbeatSent(emotion = "ozlem"),
        AnalyticsEvent.NudgeSent,
        AnalyticsEvent.MoodShared(mood = "mutlu", hasNote = true),
        AnalyticsEvent.QuestionAnswered,
        AnalyticsEvent.NoteAdded,
        AnalyticsEvent.PlanAdded(yearly = true),
        AnalyticsEvent.LocationSharingChanged(sharing = true),
        AnalyticsEvent.LocationUpdated(cityOnly = true),
        AnalyticsEvent.ProfileNameChanged,
        AnalyticsEvent.DreamAdded(category = "seyahat"),
        AnalyticsEvent.DreamCompleted,
        AnalyticsEvent.WheelSpun(builtIn = true, items = 8),
        AnalyticsEvent.WheelCreated,
        AnalyticsEvent.CapsuleSealed(days = 81),
        AnalyticsEvent.DrawingSaved(strokes = 12),
        AnalyticsEvent.DrawingDeleted,
        AnalyticsEvent.AccessoryWorn("Papyon"),
        AnalyticsEvent.AccessoryWorn(null),
        AnalyticsEvent.InboxReadAll,
        AnalyticsEvent.PasswordResetSent,
        AnalyticsEvent.CoupleUnpaired,
        AnalyticsEvent.AccountDeleted,
        AnalyticsEvent.IntroFinished(skipped = false, demosTried = 3),
        AnalyticsEvent.PermissionPrimerAnswered(permission = "location", allowed = true),
        AnalyticsEvent.ScreenView("home"),
        AnalyticsEvent.ScreenView("day_counter"),
    )

    @Test
    fun everyEventFollowsTheNamingRules() {
        for (event in all) {
            assertEquals("${event.name}: ${EventNames.problems(event)}", emptyList<String>(), EventNames.problems(event))
        }
    }

    @Test
    fun eventNamesAreUnique() {
        // One name per event type: ScreenView appears twice above (different screens), same name by design.
        val names = all.distinctBy { it::class }.map { it.name }
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
