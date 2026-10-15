package app.wardrobe.domain.engine

import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import app.wardrobe.domain.model.WeatherCondition
import app.wardrobe.domain.model.WeatherCondition.CLEAR
import app.wardrobe.domain.model.WeatherCondition.CLOUDY
import app.wardrobe.domain.model.WeatherCondition.PARTLY_CLOUDY
import app.wardrobe.domain.model.WeatherCondition.RAIN
import app.wardrobe.domain.model.WeatherCondition.SNOW
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The test cases of docs/ENGINE.md, section 10. Expected values come from the spec's prototype. */
class OutfitEngineTest {

    private val october = LocalDate.of(2026, 10, 15)
    private val july = LocalDate.of(2026, 7, 15)
    private val sample = SampleData.items
    private val byId = sample.associateBy { it.id }

    private fun item(id: String) = byId.getValue(id)

    private fun day(low: Int, high: Int, condition: WeatherCondition, rain: Int, date: LocalDate = october) =
        DayForecast(date, low, high, condition, rain)

    private val coolDay = day(7, 14, PARTLY_CLOUDY, 10)

    private fun summerItem(id: String, garment: Garment, color: ItemColor, warmth: Warmth) =
        ClothingItem(id, id, garment, color, garment.defaultMaterial, warmth, setOf(Season.SUMMER))

    private fun suggest(
        wardrobe: List<ClothingItem>,
        anchor: String,
        day: DayForecast,
        recent: List<Set<String>> = emptyList(),
    ) = OutfitEngine.suggest(wardrobe, wardrobe.first { it.id == anchor }, day, recent)

    private fun EngineResult.outfits() = (this as EngineResult.Outfits).outfits

    private fun EngineResult.itemIds() = outfits().map { outfit -> outfit.items.map { it.id } }

    private fun ScoredOutfit.points() = terms.map { it.reason to it.points }

    @Test
    fun colorTableMatchesTheSpec() {
        val expected = mapOf(
            ItemColor.WHITE to (40 to 96), ItemColor.CREAM to (41 to 89), ItemColor.BEIGE to (37 to 78),
            ItemColor.CAMEL to (34 to 60), ItemColor.BROWN to (26 to 34), ItemColor.GREY to (60 to 58),
            ItemColor.BLACK to (0 to 14), ItemColor.NAVY to (222 to 21), ItemColor.DENIM to (213 to 42),
            ItemColor.LIGHT_BLUE to (211 to 78), ItemColor.BLUE to (215 to 43), ItemColor.GREEN to (137 to 46),
            ItemColor.OLIVE to (65 to 45), ItemColor.YELLOW to (47 to 78), ItemColor.ORANGE to (26 to 61),
            ItemColor.RED to (359 to 42), ItemColor.BURGUNDY to (345 to 26), ItemColor.PINK to (348 to 75),
            ItemColor.PURPLE to (269 to 39),
        )
        assertEquals(
            expected,
            ItemColor.entries.associateWith { Math.round(it.hue).toInt() to Math.round(it.lightness).toInt() },
        )
        assertEquals(
            setOf(ItemColor.WHITE, ItemColor.CREAM, ItemColor.BEIGE, ItemColor.LIGHT_BLUE, ItemColor.YELLOW, ItemColor.PINK),
            ItemColor.entries.filter { it.isLight }.toSet(),
        )
        assertEquals(
            setOf(ItemColor.BROWN, ItemColor.BLACK, ItemColor.NAVY, ItemColor.BURGUNDY),
            ItemColor.entries.filter { it.isDark }.toSet(),
        )
    }

    @Test
    fun t1MissingFootwear() {
        val wardrobe = listOf(item("black-tee"), item("dark-jeans"))
        assertEquals(EngineResult.Missing(Category.FOOTWEAR), suggest(wardrobe, "dark-jeans", coolDay))
    }

    @Test
    fun t2ShortsNeverFitTheSampleWeek() {
        SampleData.forecast(october).days.forEach { day ->
            assertEquals(day.toString(), EngineResult.NothingFits, suggest(sample, "khaki-shorts", day))
        }
    }

    @Test
    fun t3SnowWithoutBootsFitsNothing() {
        val wardrobe = listOf(item("cream-knit"), item("dark-jeans"), item("white-sneakers"), item("camel-coat"))
        assertEquals(EngineResult.NothingFits, suggest(wardrobe, "dark-jeans", day(-1, 4, SNOW, 60)))
    }

    @Test
    fun t4SnowWithBoots() {
        val wardrobe = listOf(item("cream-knit"), item("dark-jeans"), item("white-sneakers"), item("camel-coat"), item("brown-boots"))
        val outfits = suggest(wardrobe, "dark-jeans", day(-1, 4, SNOW, 60)).outfits()
        assertEquals(listOf(listOf("camel-coat", "cream-knit", "dark-jeans", "brown-boots")), outfits.map { o -> o.items.map { it.id } })
        assertEquals(
            listOf(Reason.LAYERS_FIT to 1, Reason.RAIN_READY to 2, Reason.NEUTRAL_BASE to 3, Reason.LIGHT_DARK to 1),
            outfits.single().points(),
        )
        assertEquals(7, outfits.single().score)
    }

    @Test
    fun t5CoolDayWithoutOuterwear() {
        val wardrobe = listOf(item("white-tee"), item("dark-jeans"), item("white-sneakers"))
        assertEquals(EngineResult.Missing(Category.OUTERWEAR), suggest(wardrobe, "dark-jeans", coolDay))
    }

    @Test
    fun t6HotDayLeavesTheCoatOut() {
        val wardrobe = listOf(item("white-tee"), item("khaki-shorts"), item("white-sneakers"), item("camel-coat"))
        val outfits = suggest(wardrobe, "white-tee", day(19, 26, CLEAR, 0, july)).outfits()
        assertEquals(listOf(listOf("white-tee", "khaki-shorts", "white-sneakers")), outfits.map { o -> o.items.map { it.id } })
        assertEquals(listOf(Reason.LAYERS_FIT to 2, Reason.NEUTRAL_BASE to 3), outfits.single().points())
        assertEquals(listOf(Reason.NEUTRAL_BASE, Reason.LAYERS_FIT), outfits.single().why.map { it.reason })
    }

    @Test
    fun t7ThreeAccentsAreTooBusy() {
        val wardrobe = listOf(
            summerItem("red-tee", Garment.T_SHIRT, ItemColor.RED, Warmth.LIGHT),
            summerItem("green-trousers", Garment.TROUSERS, ItemColor.GREEN, Warmth.MEDIUM),
            summerItem("yellow-sneakers", Garment.SNEAKERS, ItemColor.YELLOW, Warmth.LIGHT),
        )
        assertEquals(EngineResult.NothingFits, suggest(wardrobe, "red-tee", day(13, 20, CLEAR, 0, july)))
    }

    @Test
    fun t8OppositeColors() {
        val wardrobe = listOf(
            summerItem("orange-tee", Garment.T_SHIRT, ItemColor.ORANGE, Warmth.LIGHT),
            summerItem("blue-trousers", Garment.TROUSERS, ItemColor.BLUE, Warmth.MEDIUM),
            summerItem("white-sneakers", Garment.SNEAKERS, ItemColor.WHITE, Warmth.LIGHT),
        )
        val outfit = suggest(wardrobe, "orange-tee", day(13, 20, CLEAR, 0, july)).outfits().single()
        assertEquals(listOf(Reason.LAYERS_FIT to 1, Reason.COMPLEMENTARY to 2), outfit.points())
        assertEquals(listOf("orange-tee", "blue-trousers"), outfit.colorTerm?.items?.map { it.id })
    }

    @Test
    fun t9ShadesOfOneColor() {
        val wardrobe = listOf(
            summerItem("burgundy-tee", Garment.T_SHIRT, ItemColor.BURGUNDY, Warmth.LIGHT),
            summerItem("pink-skirt", Garment.SKIRT, ItemColor.PINK, Warmth.LIGHT),
            summerItem("white-sneakers", Garment.SNEAKERS, ItemColor.WHITE, Warmth.LIGHT),
        )
        val outfit = suggest(wardrobe, "burgundy-tee", day(13, 20, CLEAR, 0, july)).outfits().single()
        assertEquals(listOf(Reason.LAYERS_FIT to 1, Reason.TONAL to 2, Reason.LIGHT_DARK to 1), outfit.points())
    }

    @Test
    fun t10NeighboringColors() {
        val wardrobe = listOf(
            summerItem("yellow-tee", Garment.T_SHIRT, ItemColor.YELLOW, Warmth.LIGHT),
            summerItem("orange-skirt", Garment.SKIRT, ItemColor.ORANGE, Warmth.LIGHT),
            summerItem("white-sneakers", Garment.SNEAKERS, ItemColor.WHITE, Warmth.LIGHT),
        )
        val outfit = suggest(wardrobe, "yellow-tee", day(13, 20, CLEAR, 0, july)).outfits().single()
        assertEquals(listOf(Reason.LAYERS_FIT to 1, Reason.ANALOGOUS to 2), outfit.points())
    }

    @Test
    fun t11WorkedExample() {
        val outfits = suggest(sample, "dark-jeans", coolDay).outfits()
        assertEquals(
            listOf(
                listOf("camel-coat", "black-tee", "dark-jeans", "white-sneakers"),
                listOf("camel-coat", "oxford-shirt", "dark-jeans", "black-sneakers"),
                listOf("denim-jacket", "cream-knit", "dark-jeans", "black-sneakers"),
            ),
            outfits.map { o -> o.items.map { it.id } },
        )
        assertEquals(listOf(6, 5, 5), outfits.map { it.score })
        assertEquals(
            listOf(Reason.NEUTRAL_BASE, Reason.LAYERS_FIT, Reason.LIGHT_DARK),
            outfits.first().why.map { it.reason },
        )
        assertEquals(listOf("white-sneakers"), outfits.first().why.last().items.map { it.id })
        assertEquals(
            listOf(Reason.SINGLE_ACCENT, Reason.LAYERS_FIT, Reason.LIGHT_DARK),
            outfits[1].why.map { it.reason },
        )
    }

    @Test
    fun t12RecentOutfitsAndNearCopiesDropBack() {
        val recent = listOf(setOf("camel-coat", "black-tee", "dark-jeans", "white-sneakers"))
        assertEquals(
            listOf(
                listOf("camel-coat", "oxford-shirt", "dark-jeans", "black-sneakers"),
                listOf("denim-jacket", "cream-knit", "dark-jeans", "black-sneakers"),
                listOf("olive-rain-jacket", "olive-overshirt", "dark-jeans", "black-sneakers"),
            ),
            suggest(sample, "dark-jeans", coolDay, recent).itemIds(),
        )
    }

    @Test
    fun t13RainyDayWhyOrder() {
        val first = suggest(sample, "dark-jeans", day(5, 11, RAIN, 80)).outfits().first()
        assertEquals(listOf("camel-coat", "black-tee", "dark-jeans", "brown-boots"), first.items.map { it.id })
        assertEquals(7, first.score)
        assertEquals(listOf(Reason.NEUTRAL_BASE, Reason.LAYERS_FIT, Reason.RAIN_READY), first.why.map { it.reason })
    }

    @Test
    fun t14EverySampleDayGivesThreeOutfitsInAStableOrder() {
        SampleData.forecast(october).days.forEach { day ->
            val once = suggest(sample, "dark-jeans", day).itemIds()
            assertEquals(day.toString(), 3, once.size)
            assertEquals(day.toString(), once, suggest(sample, "dark-jeans", day).itemIds())
        }
    }

    @Test
    fun t15AnchorOutOfSeasonIsNotPenalized() {
        val outfits = suggest(sample, "cream-knit", day(12, 17, CLOUDY, 20, july)).outfits()
        assertEquals(
            listOf(
                listOf("cream-knit", "beige-chinos", "black-sneakers"),
                listOf("cream-knit", "black-trousers", "white-sneakers"),
                listOf("cream-knit", "dark-jeans", "brown-boots"),
            ),
            outfits.map { o -> o.items.map { it.id } },
        )
        assertEquals(listOf(5, 5, 3), outfits.map { it.score })
        val penalty = outfits.last().terms.single { it.reason == Reason.OUT_OF_SEASON }
        assertEquals(-2 to listOf("brown-boots"), penalty.points to penalty.items.map { it.id })
    }

    @Test
    fun t16OutOfSeasonItemsCostPoints() {
        val wardrobe = listOf(item("white-tee"), item("dark-jeans"), item("white-sneakers"), item("camel-coat"))
        val outfit = suggest(wardrobe, "dark-jeans", coolDay).outfits().single()
        assertEquals(listOf("camel-coat", "white-tee", "dark-jeans", "white-sneakers"), outfit.items.map { it.id })
        assertEquals(
            listOf(Reason.LAYERS_FIT to 2, Reason.NEUTRAL_BASE to 3, Reason.OUT_OF_SEASON to -2),
            outfit.points(),
        )
        assertTrue(outfit.why.none { it.reason == Reason.OUT_OF_SEASON })
    }
}
