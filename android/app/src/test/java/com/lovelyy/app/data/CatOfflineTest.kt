package com.lovelyy.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// FirebaseFirestoreException.Code can't be initialized in plain JVM tests, so the code check is tested by name.
class CatOfflineTest {
    @Test
    fun unavailableIsOffline() {
        assertTrue(CatOffline.isOfflineCode("UNAVAILABLE"))
    }

    @Test
    fun otherErrorsAreNot() {
        assertFalse(CatOffline.isOfflineCode("PERMISSION_DENIED"))
        assertFalse(CatOffline.isOfflineCode(null))
        assertFalse(CatOffline.isOffline(IllegalStateException("x")))
    }

    @Test
    fun timeoutIsOffline() {
        val e = runCatching { runBlocking { withTimeout(1) { kotlinx.coroutines.delay(1_000) } } }.exceptionOrNull()!!
        assertTrue(CatOffline.isOffline(e))
    }
}
