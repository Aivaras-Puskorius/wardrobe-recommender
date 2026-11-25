package app.wardrobe.domain.engine

import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
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

    /** T44 and T60: the sample's two groups of section 16.5. */
    @Test
    fun sampleWardrobe() {
        assertEquals(
            GapGroups(
                neutral = listOf(
                    Gap(Garment.LONG_SLEEVE, listOf(WHITE, CREAM, BEIGE), goesWith = 10, itemDays = 22),
                    Gap(Garment.COAT, listOf(WHITE, CREAM, BEIGE, BROWN, GREY, BLACK, NAVY, DENIM), goesWith = 9, itemDays = 11),
                    Gap(Garment.SNEAKERS, listOf(BROWN, NAVY), goesWith = 5, itemDays = 9),
                    Gap(Garment.JEANS, listOf(BROWN, BLACK, NAVY), goesWith = 4, itemDays = 8),
                ),
                bold = listOf(
                    Gap(Garment.T_SHIRT, listOf(LIGHT_BLUE, YELLOW, PINK), goesWith = 11, itemDays = 11),
                    Gap(Garment.JEANS, listOf(BLUE), goesWith = 10, itemDays = 29),
                    Gap(Garment.SNEAKERS, listOf(BLUE), goesWith = 10, itemDays = 29),
                    Gap(Garment.COAT, listOf(LIGHT_BLUE, BLUE, OLIVE, YELLOW, ORANGE, PURPLE), goesWith = 9, itemDays = 11),
                ),
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

    /** Seasons play no part (user, 2026-11-21): an all-year wardrobe gets the same rows. */
    @Test
    fun seasonsDoNotChangeTheResult() {
        val allYear = SampleData.items.map { it.copy(seasons = Season.entries.toSet()) }
        assertEquals(WardrobeGaps.find(allYear), WardrobeGaps.find(SampleData.items))
    }

    @Test
    fun candidatesHaveTheGarmentDefaultsAndEverySeason() {
        val candidate = WardrobeGaps.candidates(SampleData.items).first { it.garment == Garment.COAT && it.color == NAVY }
        assertEquals(Garment.COAT.defaultMaterial, candidate.material)
        assertEquals(Garment.COAT.defaultWarmth, candidate.warmth)
        assertEquals(Fit.REGULAR, candidate.fit)
        assertEquals(Season.entries.toSet(), candidate.seasons)
    }

    /** T46, in the groups of section 16.5. */
    @Test
    fun tenItems() {
        assertEquals(
            GapGroups(
                neutral = listOf(
                    Gap(Garment.JEANS, listOf(WHITE, CREAM, BEIGE), goesWith = 8, itemDays = 25),
                    Gap(Garment.COAT, listOf(WHITE, CREAM, BEIGE, BROWN, GREY, BLACK, NAVY, DENIM), goesWith = 8, itemDays = 13),
                    Gap(Garment.LONG_SLEEVE, listOf(WHITE, CREAM, BEIGE), goesWith = 7, itemDays = 18),
                    Gap(Garment.SNEAKERS, listOf(BEIGE), goesWith = 2, itemDays = 5),
                ),
                bold = listOf(
                    Gap(Garment.JEANS, listOf(YELLOW, PINK), goesWith = 8, itemDays = 25),
                    Gap(
                        Garment.COAT,
                        listOf(LIGHT_BLUE, BLUE, GREEN, OLIVE, YELLOW, ORANGE, RED, BURGUNDY, PINK, PURPLE),
                        goesWith = 8,
                        itemDays = 13,
                    ),
                    Gap(Garment.LONG_SLEEVE, listOf(LIGHT_BLUE, YELLOW, PINK), goesWith = 7, itemDays = 15),
                    Gap(Garment.SNEAKERS, listOf(BLUE), goesWith = 6, itemDays = 8),
                ),
            ),
            WardrobeGaps.find(tenItems),
        )
    }

    /** T47: a jacket comes first in both groups. */
    @Test
    fun withoutOuterwearAJacketComesFirst() {
        val wardrobe = SampleData.items.filterNot { it.category == Category.OUTERWEAR }
        val groups = WardrobeGaps.find(wardrobe)
        listOf(groups.neutral.first(), groups.bold.first()).forEach { first ->
            assertEquals(Garment.JACKET, first.garment)
            assertEquals(13, first.goesWith)
        }
    }

    /** T48: tops alone get nothing. With a tee and jeans, sneakers in any color go with both, in each group. */
    @Test
    fun topsAloneHaveNoGapAnItemCanFill() {
        assertEquals(GapGroups(emptyList(), emptyList()), WardrobeGaps.find(items("white-tee", "black-tee")))
        assertTrue(WardrobeGaps.find(items("white-tee", "black-tee")).isEmpty)
        val (neutral, bold) = ItemColor.entries.partition { it.isNeutral }
        assertEquals(
            GapGroups(
                neutral = listOf(Gap(Garment.SNEAKERS, neutral, goesWith = 2, itemDays = 4)),
                bold = listOf(Gap(Garment.SNEAKERS, bold, goesWith = 2, itemDays = 4)),
            ),
            WardrobeGaps.find(items("white-tee", "dark-jeans")),
        )
    }

    /** The search as first written: every engine run from scratch, with the new item in the wardrobe. */
    private fun fullRunScore(allYear: List<ClothingItem>, new: ClothingItem): Pair<Int, Int> {
        var itemDays = 0
        val goesWith = allYear.count { anchor ->
            val days = WardrobeGaps.REFERENCE_DAYS.values.count { day ->
                val result = OutfitEngine.suggest(allYear + new, anchor, day)
                result is EngineResult.Outfits && result.outfits.any { new in it.items }
            }
            itemDays += days
            days > 0
        }
        return goesWith to itemDays
    }

    /** The faster search reuses each item's ranking. It must count exactly what full engine runs count. */
    @Test
    fun theFastSearchAgreesWithFullEngineRuns() {
        val wardrobes = listOf(
            tenItems,
            SampleData.items.filterNot { it.category == Category.OUTERWEAR },
            items("white-tee", "dark-jeans"),
            items("black-tee", "cream-knit", "dark-jeans", "white-sneakers", "camel-coat"),
        )
        for (wardrobe in wardrobes) {
            val allYear = WardrobeGaps.allYear(wardrobe)
            val search = WardrobeGaps.Search(allYear)
            for (candidate in WardrobeGaps.candidates(wardrobe)) {
                val fast = search.score(candidate)
                assertEquals(candidate.id, fullRunScore(allYear, candidate), fast.goesWith to fast.itemDays)
            }
        }
    }
}
