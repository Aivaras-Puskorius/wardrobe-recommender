package app.wardrobe.domain.engine

import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Season
import org.junit.Assert.assertEquals
import org.junit.Test

/** The store check of docs/ENGINE.md section 15. Expected values come from docs/gaps_prototype.py. */
class StoreCheckTest {

    /** An item as the add form would make it, with the garment's defaults. */
    private fun shopItem(garment: Garment, color: ItemColor, seasons: Set<Season> = setOf(Season.SUMMER)) = ClothingItem(
        id = "shop", name = "", garment = garment, color = color, material = garment.defaultMaterial, fit = Fit.REGULAR,
        warmth = garment.defaultWarmth, seasons = seasons, photo = null,
    )

    private fun check(garment: Garment, color: ItemColor) = StoreCheck.check(SampleData.items, shopItem(garment, color))

    /** Each band's outfit by item id, with the shop item as "new". */
    private fun StoreCheckResult.ids() = outfits.map { (band, outfit) ->
        band to outfit.items.map { if (it.id == checked.id) "new" else it.id }
    }

    /** T49. */
    @Test
    fun aNavyCoatGoesWithElevenItemsOnCoolAndColdDays() {
        val result = check(Garment.COAT, ItemColor.NAVY)
        assertEquals(11, result.goesWith)
        assertEquals(17, result.itemCount)
        assertEquals(emptyList<ClothingItem>(), result.similar)
        assertEquals(
            listOf(
                DayBand.COOL to listOf("black-tee", "grey-skirt", "white-sneakers", "new"),
                // The shown outfits count seasons, as outfit ideas do, so the cold day gets winter pieces.
                DayBand.COLD to listOf("cream-knit", "black-trousers", "brown-boots", "new"),
            ),
            result.ids(),
        )
        assertEquals(listOf(8, 7), result.outfits.map { it.outfit.score })
    }

    /** T50: cream is 12.3 from white, so a cream T-shirt nearly repeats the white tee. */
    @Test
    fun aTShirtLikeOneTheUserHasIsFlagged() {
        assertEquals(listOf("white-tee"), check(Garment.T_SHIRT, ItemColor.WHITE).similar.map { it.id })
        assertEquals(listOf("white-tee"), check(Garment.T_SHIRT, ItemColor.CREAM).similar.map { it.id })
        assertEquals(emptyList<String>(), check(Garment.T_SHIRT, ItemColor.BEIGE).similar.map { it.id })
    }

    @Test
    fun aWhiteTShirtGetsAnOutfitForEveryBand() {
        val result = check(Garment.T_SHIRT, ItemColor.WHITE)
        assertEquals(8, result.goesWith)
        assertEquals(
            listOf(
                DayBand.HOT to listOf("new", "beige-chinos", "black-sneakers"),
                DayBand.MILD to listOf("new", "beige-chinos", "black-sneakers", "denim-jacket"),
                DayBand.COOL to listOf("new", "beige-chinos", "black-sneakers", "camel-coat"),
                DayBand.COLD to listOf("new", "black-trousers", "brown-boots", "camel-coat"),
            ),
            result.ids(),
        )
        assertEquals(listOf(7, 5, 7, 7), result.outfits.map { it.outfit.score })
    }

    /** T51: green sneakers can be worn, but never make the top three for one of the user's items. */
    @Test
    fun greenSneakersGoWithNothingButStillHaveOutfits() {
        val result = check(Garment.SNEAKERS, ItemColor.GREEN)
        assertEquals(0, result.goesWith)
        assertEquals(listOf(DayBand.HOT, DayBand.MILD, DayBand.COOL, DayBand.COLD), result.outfits.map { it.band })
    }

    /** T52. */
    @Test
    fun shortsOnlyFitTheHotDay() {
        val result = check(Garment.SHORTS, ItemColor.BLUE)
        assertEquals(listOf(DayBand.HOT to listOf("white-tee", "new", "black-sneakers")), result.ids())
    }

    @Test
    fun theItemsOwnSeasonsDoNotMatter() {
        val summer = StoreCheck.check(SampleData.items, shopItem(Garment.COAT, ItemColor.NAVY, setOf(Season.SUMMER)))
        val allYear = StoreCheck.check(SampleData.items, shopItem(Garment.COAT, ItemColor.NAVY, Season.entries.toSet()))
        assertEquals(allYear.goesWith, summer.goesWith)
        assertEquals(allYear.outfits.map { it.outfit.score }, summer.outfits.map { it.outfit.score })
    }
}
