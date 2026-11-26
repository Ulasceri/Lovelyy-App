package com.lovelyy.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelRepositoryTest {
    @Test
    fun builtInWheelsAreCreatedOnFirstEdit() {
        WheelRepository.BUILT_IN_IDS.forEach { assertTrue(it, WheelRepository.createsOnEdit(it)) }
    }

    @Test
    fun customWheelsAreNeverRecreatedByAnEdit() {
        assertFalse(WheelRepository.createsOnEdit("aB3xYz9QwErTy"))
        assertFalse(WheelRepository.createsOnEdit(""))
    }
}
