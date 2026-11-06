package com.example

import com.example.model.PRESET_CITY_LANDMARKS
import com.example.model.TourLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `preset landmarks and languages are configured`() {
        assertEquals(3, PRESET_CITY_LANDMARKS.size)
        assertTrue(PRESET_CITY_LANDMARKS.any { it.title.contains("Colosseum") })
        assertTrue(TourLanguage.entries.size >= 8)
    }
}
