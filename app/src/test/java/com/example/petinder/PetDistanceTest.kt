package com.example.petinder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetDistanceTest {
    private val pet = Pet("test", "Test", "Dog", "Mixed", 1, "Male", "Medium", "buddy", 0.0, 0.0, "Test", "Test", "Test", emptyList(), 0)

    @Test fun sameLocationHasZeroDistance() {
        assertEquals(0.0, pet.distance(0.0, 0.0), 0.00001)
    }

    @Test fun oneDegreeAtEquatorIsAbout111Kilometers() {
        assertEquals(111.195, pet.distance(0.0, 1.0), 0.01)
    }

    @Test fun distanceAcrossDateLineUsesShortArc() {
        val across = pet.copy(latitude = 0.0, longitude = 179.9)
        assertEquals(22.239, across.distance(0.0, -179.9), 0.01)
        assertTrue(across.distance(0.0, -179.9).isFinite())
    }
}
