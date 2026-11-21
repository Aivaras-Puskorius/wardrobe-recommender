package app.wardrobe.domain.engine

import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemColor.BEIGE
import app.wardrobe.domain.model.ItemColor.BLACK
import app.wardrobe.domain.model.ItemColor.BLUE
import app.wardrobe.domain.model.ItemColor.BROWN
import app.wardrobe.domain.model.ItemColor.BURGUNDY
import app.wardrobe.domain.model.ItemColor.CAMEL
import app.wardrobe.domain.model.ItemColor.CREAM
import app.wardrobe.domain.model.ItemColor.DENIM
import app.wardrobe.domain.model.ItemColor.GREEN
import app.wardrobe.domain.model.ItemColor.GREY
import app.wardrobe.domain.model.ItemColor.LIGHT_BLUE
import app.wardrobe.domain.model.ItemColor.NAVY
import app.wardrobe.domain.model.ItemColor.OLIVE
import app.wardrobe.domain.model.ItemColor.ORANGE
import app.wardrobe.domain.model.ItemColor.PINK
import app.wardrobe.domain.model.ItemColor.PURPLE
import app.wardrobe.domain.model.ItemColor.RED
import app.wardrobe.domain.model.ItemColor.WHITE
import app.wardrobe.domain.model.ItemColor.YELLOW
import app.wardrobe.domain.model.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wardrobe gap rules of docs/ENGINE.md section 14. Expected values come from docs/gaps_prototype.py. */
class WardrobeGapsTest {

    private val byId = SampleData.items.associateBy { it.id }

    private fun items(vararg ids: String) = ids.map { byId.getValue(it) }

    private val tenItems = items(
        "white-tee", "black-tee", "oxford-shirt", "dark-jeans", "black-trousers",
        "white-sneakers", "black-sneakers", "brown-boots", "denim-jacket", "camel-coat",
    )

    /** T44. */
    @Test
    fun sampleWardrobe() {
        assertEquals(
            listOf(
                Gap(Garment.SNEAKERS, listOf(BROWN, NAVY), goesWith = 14, itemDays = 33),
                Gap(
                    Garment.COAT,
                    listOf(
                        WHITE, CREAM, BEIGE, BROWN, GREY, BLACK, NAVY, DENIM, LIGHT_BLUE, BLUE, GREEN, ORANGE, RED,
                        BURGUNDY, PINK,
                    ),
                    goesWith = 13,
                    itemDays = 20,
                ),
                Gap(Garment.LONG_SLEEVE, listOf(WHITE, CREAM, BEIGE), goesWith = 11, itemDays = 34),
                Gap(Garment.JEANS, listOf(WHITE, CREAM, BEIGE), goesWith = 11, itemDays = 15),
            ),
            WardrobeGaps.find(SampleData.items),
        )
    }

    /** T45: white and cream are 12.3 apart, white and beige 26.7, cream and beige 14.9. */
    @Test
    fun aColorNearOneTheUserHasInThatGarmentIsSkipped() {
        val candidates = WardrobeGaps.candidates(SampleData.items).map { it.garment to it.color }
        assertTrue(Garment.SNEAKERS to BEIGE in candidates)
        assertTrue(Garment.SNEAKERS to CREAM !in candidates)
        assertTrue(Garment.SNEAKERS to WHITE !in candidates)
        // Cream boots are fine: the brown boots are far from cream.
        assertTrue(Garment.BOOTS to CREAM in candidates)
    }

    @Test
    fun candidatesHaveTheGarmentDefaultsAndEverySeason() {
        val candidate = WardrobeGaps.candidates(SampleData.items).first { it.garment == Garment.COAT && it.color == NAVY }
        assertEquals(Garment.COAT.defaultMaterial, candidate.material)
        assertEquals(Garment.COAT.defaultWarmth, candidate.warmth)
        assertEquals(Fit.REGULAR, candidate.fit)
        assertEquals(Season.entries.toSet(), candidate.seasons)
    }

    /** T46. */
    @Test
    fun tenItems() {
        assertEquals(
            listOf(
                Gap(Garment.JEANS, listOf(WHITE, CREAM, BEIGE), goesWith = 8, itemDays = 25),
                Gap(
                    Garment.COAT,
                    listOf(
                        WHITE, CREAM, BEIGE, BROWN, GREY, BLACK, NAVY, DENIM, LIGHT_BLUE, BLUE, GREEN, OLIVE, YELLOW,
                        ORANGE, RED, BURGUNDY, PINK, PURPLE,
                    ),
                    goesWith = 8,
                    itemDays = 15,
                ),
                Gap(Garment.LONG_SLEEVE, listOf(WHITE, CREAM, BEIGE, BROWN, BLACK, NAVY), goesWith = 7, itemDays = 21),
                Gap(Garment.SNEAKERS, listOf(BEIGE, LIGHT_BLUE), goesWith = 7, itemDays = 16),
            ),
            WardrobeGaps.find(tenItems),
        )
    }

    /** T47. */
    @Test
    fun withoutOuterwearAJacketComesFirst() {
        val wardrobe = SampleData.items.filterNot { it.category == Category.OUTERWEAR }
        val first = WardrobeGaps.find(wardrobe).first()
        assertEquals(Garment.JACKET, first.garment)
        assertEquals(13, first.goesWith)
    }

    /** T48. */
    @Test
    fun topsAloneHaveNoGapAnItemCanFill() {
        assertEquals(emptyList<Gap>(), WardrobeGaps.find(items("white-tee", "black-tee")))
        assertEquals(
            listOf(Gap(Garment.SNEAKERS, ItemColor.entries, goesWith = 2, itemDays = 4)),
            WardrobeGaps.find(items("white-tee", "dark-jeans")),
        )
    }
}
