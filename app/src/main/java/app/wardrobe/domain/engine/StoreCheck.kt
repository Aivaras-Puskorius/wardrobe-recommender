package app.wardrobe.domain.engine

import app.wardrobe.domain.model.ClothingItem

/** The kind of day an outfit is for. */
enum class DayBand { HOT, MILD, COOL, COLD }

/** The best outfit with the checked item on one kind of day. */
data class BandOutfit(val band: DayBand, val outfit: ScoredOutfit)

/**
 * What an item seen in a shop would do for the wardrobe: how many of the [itemCount] items it [goesWith], the
 * owned items it nearly repeats, and its best outfit for each kind of day it suits.
 *
 * @param checked the item as the checks used it.
 */
data class StoreCheckResult(
    val checked: ClothingItem,
    val goesWith: Int,
    val itemCount: Int,
    val similar: List<ClothingItem>,
    val outfits: List<BandOutfit>,
)

/** The store check. The rules are in docs/ENGINE.md section 15. */
object StoreCheck {

    /**
     * Checks [item], which isn't in the [wardrobe], with the same days, tie rule and season rule as What to add.
     * The outfits shown count seasons again, as outfit ideas do, so the cold day gets the user's winter pieces.
     *
     * INFO(limit): about 4 engine runs per wardrobe item, a fraction of a second for the sample. Fine for a
     * wardrobe of hundreds.
     */
    fun check(wardrobe: List<ClothingItem>, item: ClothingItem): StoreCheckResult {
        val checked = WardrobeGaps.allYear(listOf(item)).single().copy(id = WardrobeGaps.newItemId(item.garment, item.color))
        val outfits = WardrobeGaps.REFERENCE_DAYS.mapNotNull { (band, day) ->
            val result = OutfitEngine.suggest(wardrobe, checked, day, limit = 1)
            (result as? EngineResult.Outfits)?.outfits?.firstOrNull()?.let { BandOutfit(band, it) }
        }
        return StoreCheckResult(
            checked = checked,
            goesWith = WardrobeGaps.score(WardrobeGaps.allYear(wardrobe), checked).goesWith,
            itemCount = wardrobe.size,
            similar = wardrobe.filter { it.garment == item.garment && WardrobeGaps.near(it.color, item.color) },
            outfits = outfits,
        )
    }
}
