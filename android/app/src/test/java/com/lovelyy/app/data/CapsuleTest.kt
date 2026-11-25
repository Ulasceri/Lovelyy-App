package com.lovelyy.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CapsuleTest {
    private fun capsule(openOn: LocalDate) = Capsule("1", "t", "x", openOn, "u", "U", 0L)

    @Test
    fun opensOnItsDayAndAfter() {
        val day = LocalDate.of(2027, 2, 14)
        assertFalse(capsule(day).isOpen(day.minusDays(1)))
        assertTrue(capsule(day).isOpen(day))
        assertTrue(capsule(day).isOpen(day.plusDays(30)))
    }
}
