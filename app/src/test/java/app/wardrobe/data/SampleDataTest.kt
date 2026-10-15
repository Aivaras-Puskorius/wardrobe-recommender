package app.wardrobe.data

import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.Material
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataTest {

    @Test
    fun sampleItemIdsAreUnique() {
        val ids = SampleData.items.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyCategoryHasAtLeastTwoSampleItems() {
        for (category in Category.entries) {
            val count = SampleData.items.count { it.category == category }
            assertTrue("$category has $count items", count >= 2)
        }
    }

    @Test
    fun everySampleItemHasASeason() {
        assertTrue(SampleData.items.all { it.seasons.isNotEmpty() })
    }

    @Test
    fun jacketsAndCoatHaveTheMaterialTheirNamesSay() {
        val materials = SampleData.items.filter { it.category == Category.OUTERWEAR }.associate { it.id to it.material }
        assertEquals(
            mapOf(
                "denim-jacket" to Material.DENIM,
                "olive-rain-jacket" to Material.WATERPROOF,
                "camel-coat" to Material.WOOL,
            ),
            materials,
        )
    }

    @Test
    fun photoSuggestionsAreNotAlreadyInTheWardrobe() {
        val owned = SampleData.items.map { it.garment to it.color }.toSet()
        assertTrue(SampleData.photoSuggestions.none { (it.garment to it.color) in owned })
    }

    @Test
    fun forecastCoversSevenConsecutiveDaysFromToday() {
        val today = LocalDate.of(2026, 10, 7)
        val forecast = SampleData.forecast(today)
        assertEquals((0L..6L).map { today.plusDays(it) }, forecast.days.map { it.date })
    }

    @Test
    fun forecastIsForKaunas() {
        assertEquals("Kaunas", SampleData.forecast(LocalDate.of(2026, 10, 7)).location)
    }

    @Test
    fun forecastMinimumNeverExceedsMaximum() {
        val forecast = SampleData.forecast(LocalDate.of(2026, 10, 7))
        assertTrue(forecast.days.all { it.minTemperatureC <= it.maxTemperatureC })
    }
}
