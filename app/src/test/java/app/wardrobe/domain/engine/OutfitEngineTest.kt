package app.wardrobe.domain.engine

import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
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
import org.junit.Assert.assertFalse
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

    private fun summerItem(
        id: String,
        garment: Garment,
        color: ItemColor,
        warmth: Warmth,
        material: Material = garment.defaultMaterial,
        fit: Fit = Fit.REGULAR,
    ) = ClothingItem(id, id, garment, color, material, fit, warmth, setOf(Season.SUMMER), photo = null)

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
        assertEquals(listOf(listOf("cream-knit", "dark-jeans", "brown-boots", "camel-coat")), outfits.map { o -> o.items.map { it.id } })
        assertEquals(
            listOf(
                Reason.LAYERS_FIT to 1, Reason.WARM_WOOL to 1, Reason.NEUTRAL_BASE to 3, Reason.LIGHT_DARK to 1,
                Reason.OUTDOOR_HOLDS to 1,
            ),
            outfits.single().points(),
        )
        assertEquals(7, outfits.single().score)
        assertEquals(listOf("cream-knit", "camel-coat"), outfits.single().term(Reason.WARM_WOOL).items.map { it.id })
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
        assertEquals(
            listOf(Reason.LAYERS_FIT to 2, Reason.COOL_FABRIC to 1, Reason.NEUTRAL_BASE to 3),
            outfits.single().points(),
        )
        assertEquals(
            listOf(Reason.NEUTRAL_BASE, Reason.LAYERS_FIT, Reason.COOL_FABRIC),
            outfits.single().why.map { it.reason },
        )
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
                listOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat"),
                listOf("oxford-shirt", "dark-jeans", "black-sneakers", "denim-jacket"),
                listOf("olive-overshirt", "dark-jeans", "black-sneakers", "olive-rain-jacket"),
            ),
            outfits.map { o -> o.items.map { it.id } },
        )
        assertEquals(listOf(7, 7, 7), outfits.map { it.score })
        assertEquals(
            listOf(Reason.SINGLE_ACCENT, Reason.LAYERS_FIT, Reason.BALANCED_FIT),
            outfits[2].why.map { it.reason },
        )
        assertTrue(outfits.all { outfit -> outfit.terms.any { it.reason == Reason.OUTDOOR_HOLDS } })
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
        val recent = listOf(setOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat"))
        assertEquals(
            listOf(
                listOf("oxford-shirt", "dark-jeans", "black-sneakers", "denim-jacket"),
                listOf("olive-overshirt", "dark-jeans", "black-sneakers", "olive-rain-jacket"),
                listOf("oxford-shirt", "dark-jeans", "brown-boots", "olive-rain-jacket"),
            ),
            suggest(sample, "dark-jeans", coolDay, recent).itemIds(),
        )
    }

    @Test
    fun t13RainyDayFavorsTheRainJacket() {
        val (first, second) = suggest(sample, "dark-jeans", day(5, 11, RAIN, 80)).outfits()
        assertEquals(listOf("olive-overshirt", "dark-jeans", "brown-boots", "olive-rain-jacket"), first.items.map { it.id })
        assertEquals(9, first.score)
        assertEquals(listOf(Reason.SINGLE_ACCENT, Reason.LAYERS_FIT, Reason.RAIN_READY), first.why.map { it.reason })
        assertEquals(listOf("olive-rain-jacket", "brown-boots"), first.term(Reason.RAIN_READY).items.map { it.id })
        assertTrue(first.terms.none { it.reason == Reason.WARM_WOOL })

        assertEquals(listOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat"), second.items.map { it.id })
        assertEquals(7, second.score)
        assertTrue(second.terms.none { it.reason == Reason.RAIN_READY })
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
        assertEquals(listOf("white-tee", "dark-jeans", "white-sneakers", "camel-coat"), outfit.items.map { it.id })
        assertEquals(
            listOf(Reason.LAYERS_FIT to 2, Reason.NEUTRAL_BASE to 3, Reason.OUTDOOR_HOLDS to 1, Reason.OUT_OF_SEASON to -2),
            outfit.points(),
        )
        assertTrue(outfit.why.none { it.reason == Reason.OUT_OF_SEASON })
    }

    @Test
    fun t17RainReadyFootwearIsLeatherOrWaterproof() {
        val base = listOf(item("olive-rain-jacket"), item("cream-knit"), item("dark-jeans"))
        val rain = day(5, 11, RAIN, 80)
        val synthetic = suggest(base + item("white-sneakers"), "dark-jeans", rain).outfits().single()
        assertEquals(5, synthetic.score)
        assertTrue(synthetic.terms.none { it.reason == Reason.RAIN_READY })

        val rainSneakers = ClothingItem(
            "black-rain-sneakers", "Black rain sneakers", Garment.SNEAKERS, ItemColor.BLACK, Material.WATERPROOF,
            Fit.REGULAR, Warmth.LIGHT, setOf(Season.AUTUMN), photo = null,
        )
        val waterproof = suggest(base + rainSneakers, "dark-jeans", rain).outfits().single()
        assertEquals(8, waterproof.score)
        assertEquals(2, waterproof.term(Reason.RAIN_READY).points)
    }

    @Test
    fun t18WoolEarnsAPointBelow5Degrees() {
        val wardrobe = listOf(item("olive-rain-jacket"), item("cream-knit"), item("dark-jeans"), item("brown-boots"))
        val cold = suggest(wardrobe, "dark-jeans", day(4, 12, CLEAR, 5)).outfits().single()
        assertEquals(8, cold.score)
        assertEquals(1 to listOf("cream-knit"), cold.term(Reason.WARM_WOOL).let { it.points to it.items.map { i -> i.id } })

        val cool = suggest(wardrobe, "dark-jeans", day(5, 12, CLEAR, 5)).outfits().single()
        assertEquals(6, cool.score)
        assertTrue(cool.terms.none { it.reason == Reason.WARM_WOOL })
    }

    @Test
    fun t19CottonOrLinenTopsEarnAPointFrom22Degrees() {
        val trousers = summerItem("beige-trousers", Garment.TROUSERS, ItemColor.BEIGE, Warmth.MEDIUM)
        val sneakers = summerItem("white-sneakers", Garment.SNEAKERS, ItemColor.WHITE, Warmth.LIGHT)
        fun outfit(material: Material, high: Int): ScoredOutfit {
            val shirt = summerItem("white-shirt", Garment.SHIRT, ItemColor.WHITE, Warmth.MEDIUM, material)
            return suggest(listOf(shirt, trousers, sneakers), "white-shirt", day(15, high, CLEAR, 0, july)).outfits().single()
        }

        val linenAt22 = outfit(Material.LINEN, 22)
        assertEquals(6, linenAt22.score)
        assertEquals(listOf("white-shirt"), linenAt22.term(Reason.COOL_FABRIC).items.map { it.id })
        assertEquals(5, outfit(Material.LINEN, 21).score)
        assertFalse(outfit(Material.SYNTHETIC, 22).terms.any { it.reason == Reason.COOL_FABRIC })
    }

    @Test
    fun t20TheJacketEarnsAPointWhenItGoesWithTheRest() {
        assertEquals(
            listOf(
                listOf("burgundy-sweater", "beige-chinos", "black-sneakers", "denim-jacket"),
                listOf("burgundy-sweater", "black-trousers", "white-sneakers", "denim-jacket"),
                listOf("burgundy-sweater", "beige-chinos", "brown-boots", "camel-coat"),
            ),
            suggest(sample, "burgundy-sweater", day(3, 9, CLOUDY, 30)).itemIds(),
        )

        val wardrobe = listOf(item("burgundy-sweater"), item("beige-chinos"), item("brown-boots"), item("olive-rain-jacket"))
        val clash = suggest(wardrobe, "burgundy-sweater", coolDay).outfits().single()
        assertEquals(5, clash.score)
        assertTrue(clash.terms.none { it.reason == Reason.OUTDOOR_HOLDS })
    }

    @Test
    fun t21TheIndoorLookSetsTheColorStory() {
        val wardrobe = listOf(item("olive-rain-jacket"), item("cream-knit"), item("dark-jeans"), item("brown-boots"))
        val outfit = suggest(wardrobe, "dark-jeans", day(4, 12, CLEAR, 5)).outfits().single()
        assertEquals(listOf("cream-knit", "dark-jeans", "brown-boots", "olive-rain-jacket"), outfit.items.map { it.id })
        assertEquals(Reason.NEUTRAL_BASE, outfit.colorTerm?.reason)
        assertEquals(listOf("cream-knit", "dark-jeans", "brown-boots"), outfit.colorTerm?.items?.map { it.id })
        assertEquals(listOf("olive-rain-jacket"), outfit.term(Reason.OUTDOOR_HOLDS).items.map { it.id })
    }

    @Test
    fun t22TiesGoToFewerIndoorAccents() {
        assertEquals(
            listOf(
                listOf("white-tee", "beige-chinos", "black-sneakers", "camel-coat"),
                listOf("white-tee", "black-trousers", "brown-boots", "camel-coat"),
                listOf("white-tee", "beige-chinos", "brown-boots", "denim-jacket"),
            ),
            suggest(sample, "white-tee", coolDay).itemIds(),
        )
    }

    @Test
    fun t23LightAndDarkCountIndoorsOnly() {
        val navyCoat = ClothingItem(
            "navy-coat", "Navy coat", Garment.COAT, ItemColor.NAVY, Material.WOOL, Fit.REGULAR, Warmth.WARM,
            setOf(Season.AUTUMN, Season.WINTER), photo = null,
        )
        val wardrobe = listOf(item("cream-knit"), item("beige-chinos"), item("white-sneakers"), navyCoat)
        val outfit = suggest(wardrobe, "cream-knit", day(3, 9, CLOUDY, 30)).outfits().single()
        assertEquals(
            listOf(Reason.LAYERS_FIT to 1, Reason.WARM_WOOL to 1, Reason.NEUTRAL_BASE to 3, Reason.OUTDOOR_HOLDS to 1),
            outfit.points(),
        )
    }

    @Test
    fun t24NoJacketPointWhenTheDayLeavesTheJacketOptional() {
        val mild = day(13, 19, PARTLY_CLOUDY, 10)
        val chinos = suggest(sample, "beige-chinos", mild).outfits()
        assertEquals(
            listOf(
                listOf("olive-overshirt", "beige-chinos", "black-sneakers"),
                listOf("oxford-shirt", "beige-chinos", "brown-boots"),
                listOf("black-tee", "beige-chinos", "black-sneakers", "denim-jacket"),
            ),
            chinos.map { o -> o.items.map { it.id } },
        )
        assertEquals(listOf(6, 6, 5), chinos.map { it.score })

        val jacket = suggest(sample, "denim-jacket", mild).outfits()
        assertEquals(listOf("black-tee", "grey-skirt", "white-sneakers", "denim-jacket"), jacket.first().items.map { it.id })
        assertTrue(jacket.all { outfit -> outfit.terms.any { it.reason == Reason.OUTDOOR_HOLDS } })
    }

    @Test
    fun t25AFittedJacketCannotGoOverALooseTop() {
        val wardrobe = listOf(item("olive-overshirt"), item("dark-jeans"), item("black-sneakers"), item("denim-jacket"))
        assertEquals(EngineResult.NothingFits, suggest(wardrobe, "dark-jeans", coolDay))

        val outfit = suggest(wardrobe + item("camel-coat"), "dark-jeans", coolDay).outfits().single()
        assertEquals(listOf("olive-overshirt", "dark-jeans", "black-sneakers", "camel-coat"), outfit.items.map { it.id })
        assertEquals(6, outfit.score)
    }

    /** Light drizzle counts as cloudy, but wool stays under a jacket (user, 2026-10-27). */
    @Test
    fun t27OnADrizzleDayAWoolTopNeedsAJacketOverIt() {
        val dry = day(13, 19, CLOUDY, 10)
        val drizzle = dry.copy(drizzle = true)

        val sweater = listOf(item("cream-knit"), item("dark-jeans"), item("black-sneakers"), item("denim-jacket"))
        assertEquals(
            listOf("cream-knit", "dark-jeans", "black-sneakers"),
            suggest(sweater, "cream-knit", dry).outfits().single().items.map { it.id },
        )
        // The denim jacket over the warm sweater is too warm for 13°, so nothing is left.
        assertEquals(EngineResult.NothingFits, suggest(sweater, "cream-knit", drizzle))

        val woolShirt = item("oxford-shirt").copy(material = Material.WOOL)
        val shell = ClothingItem(
            "rain-shell", "Rain shell", Garment.JACKET, ItemColor.BLACK, Material.WATERPROOF, Fit.REGULAR, Warmth.LIGHT,
            setOf(Season.AUTUMN), photo = null,
        )
        val shirt = listOf(woolShirt, item("dark-jeans"), item("black-sneakers"), shell)
        assertEquals(
            listOf("oxford-shirt", "dark-jeans", "black-sneakers", "rain-shell"),
            suggest(shirt, "oxford-shirt", drizzle).outfits().single().items.map { it.id },
        )
        // A cotton top may still go without the jacket.
        val cotton = suggest(shirt - woolShirt + item("oxford-shirt"), "oxford-shirt", drizzle).outfits()
        assertTrue(cotton.any { outfit -> outfit.items.none { it.category == Category.OUTERWEAR } })
    }

    @Test
    fun t26ARoomyPieceWithAFittedOneIsBalanced() {
        val day = day(13, 20, CLEAR, 0, july)
        val sneakers = summerItem("white-sneakers", Garment.SNEAKERS, ItemColor.WHITE, Warmth.LIGHT)
        fun shirt(fit: Fit) = summerItem("white-shirt", Garment.SHIRT, ItemColor.WHITE, Warmth.MEDIUM, fit = fit)
        fun trousers(fit: Fit) = summerItem("beige-trousers", Garment.TROUSERS, ItemColor.BEIGE, Warmth.MEDIUM, fit = fit)
        fun outfit(vararg items: ClothingItem) = suggest(items.toList() + sneakers, items.first().id, day).outfits().single()

        val looseOverFitted = outfit(shirt(Fit.LOOSE), trousers(Fit.FITTED))
        assertEquals(6, looseOverFitted.score)
        assertEquals(listOf("white-shirt", "beige-trousers"), looseOverFitted.term(Reason.BALANCED_FIT).items.map { it.id })
        assertEquals(6, outfit(shirt(Fit.OVERSIZED), trousers(Fit.FITTED)).score)

        val tee = summerItem("white-tee", Garment.T_SHIRT, ItemColor.WHITE, Warmth.LIGHT, fit = Fit.FITTED)
        val skirt = summerItem("grey-skirt", Garment.SKIRT, ItemColor.GREY, Warmth.LIGHT, fit = Fit.LOOSE)
        assertEquals(1, outfit(tee, skirt).term(Reason.BALANCED_FIT).points)

        assertEquals(5, outfit(shirt(Fit.REGULAR), trousers(Fit.FITTED)).score)
        val looseOnLoose = outfit(shirt(Fit.OVERSIZED), trousers(Fit.LOOSE))
        assertEquals(listOf(Reason.LAYERS_FIT to 2, Reason.NEUTRAL_BASE to 3), looseOnLoose.points())
    }

    private fun ScoredOutfit.term(reason: Reason) = terms.single { it.reason == reason }
}
