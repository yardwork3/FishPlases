package ru.fishplaces

import org.junit.Assert.*
import org.junit.Test
import ru.fishplaces.data.FishingSpot
import ru.fishplaces.data.SpotValidation

class SpotValidationTest {
    @Test fun blankNamesAreRejected() {
        listOf("", " ", "\n\t", "\u00A0").forEach { assertFalse(SpotValidation.validName(it)) }
    }
    @Test fun meaningfulNameIsAccepted() {
        assertTrue(SpotValidation.validName("  Заводь у моста  "))
    }
    @Test fun ratingIsOptionalAndLimitedToOneThroughFive() {
        assertTrue(SpotValidation.validRating(null))
        (1..5).forEach { assertTrue(SpotValidation.validRating(it)) }
        listOf(-1, 0, 6, Int.MAX_VALUE).forEach { assertFalse(SpotValidation.validRating(it)) }
    }
    @Test fun coordinatesMustBeFiniteAndInRange() {
        assertTrue(SpotValidation.validCoordinates(55.0084, 82.9357))
        assertTrue(SpotValidation.validCoordinates(-90.0, 180.0))
        assertFalse(SpotValidation.validCoordinates(90.01, 0.0))
        assertFalse(SpotValidation.validCoordinates(0.0, -180.01))
        assertFalse(SpotValidation.validCoordinates(Double.NaN, 0.0))
        assertFalse(SpotValidation.validCoordinates(0.0, Double.POSITIVE_INFINITY))
    }
    @Test(expected = IllegalArgumentException::class)
    fun categoryExpansionIsNotEnabled() {
        SpotValidation.requireValid(FishingSpot(name = "Лес", latitude = 0.0, longitude = 0.0, category = "mushrooms"))
    }
}
