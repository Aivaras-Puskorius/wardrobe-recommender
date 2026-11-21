package app.wardrobe.domain.engine

import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Lab
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.WeatherCondition
import java.time.LocalDate

/**
 * A garment the user could add: the [colors] that do best, how many of the user's items it [goesWith], and on how
 * many item and day pairs ([itemDays]).
 */
data class Gap(val garment: Garment, val colors: List<ItemColor>, val goesWith: Int, val itemDays: Int)

/** What to add to the wardrobe. The rules are in docs/ENGINE.md section 14. */
object WardrobeGaps {

    /**
     * The best garment per category, best first, or none when no new item would go with anything.
     *
     * INFO(limit): tries every candidate with every item and day, about 240 × items × 4 engine runs. Cache the
     * result per wardrobe when that gets slow on the phone.
     */
    fun find(wardrobe: List<ClothingItem>): List<Gap> {
        val scored = candidates(wardrobe).map { it to score(wardrobe, it) }
            .sortedWith(
                compareByDescending<Pair<ClothingItem, Score>> { it.second.goesWith }
                    .thenByDescending { it.second.itemDays }
                    .thenBy { it.first.garment.ordinal }
                    .thenBy { it.first.color.ordinal },
            )
        return Category.entries.mapNotNull { category ->
            val inCategory = scored.filter { it.first.category == category }
            val (best, score) = inCategory.firstOrNull() ?: return@mapNotNull null
            if (score.goesWith == 0) return@mapNotNull null
            val colors = inCategory.filter { it.first.garment == best.garment && it.second == score }.map { it.first.color }
            Gap(best.garment, colors, score.goesWith, score.itemDays)
        }.sortedWith(compareByDescending<Gap> { it.goesWith }.thenByDescending { it.itemDays })
    }

    /** Every item the user could add: each garment in each color, except those the user has or nearly has. */
    fun candidates(wardrobe: List<ClothingItem>): List<ClothingItem> =
        Garment.entries.flatMap { garment ->
            val owned = wardrobe.filter { it.garment == garment }.map { Lab.of(it.color.argb) }
            ItemColor.entries
                .filter { color -> owned.none { it.distanceTo(Lab.of(color.argb)) < NEAR_COLOR } }
                .map { color -> candidate(garment, color) }
        }

    private data class Score(val goesWith: Int, val itemDays: Int)

    /** The items whose three outfits include [new] on at least one reference day, and on how many item-days. */
    private fun score(wardrobe: List<ClothingItem>, new: ClothingItem): Score {
        var itemDays = 0
        val goesWith = wardrobe.count { anchor ->
            val days = REFERENCE_DAYS.count { day ->
                val result = OutfitEngine.suggest(wardrobe + new, anchor, day)
                result is EngineResult.Outfits && result.outfits.any { outfit -> new in outfit.items }
            }
            itemDays += days
            days > 0
        }
        return Score(goesWith, itemDays)
    }

    /**
     * WARN: the id sorts after every real id on purpose. Where outfits tie on score, the engine orders them by item
     * id, so the user's own outfits come first and a new item counts only where it does better.
     */
    private fun candidate(garment: Garment, color: ItemColor) = ClothingItem(
        id = "\uFFFF${garment.name}-${color.name}",
        name = "",
        garment = garment,
        color = color,
        material = garment.defaultMaterial,
        fit = Fit.REGULAR,
        warmth = garment.defaultWarmth,
        seasons = Season.entries.toSet(),
        photo = null,
    )

    /** Colors closer than this in CIELAB count as the same: white and cream are 12.3 apart, white and beige 26.7. */
    private const val NEAR_COLOR = 15.0

    /** One dry day per temperature band, so the result doesn't change with this week's forecast. */
    private val REFERENCE_DAYS = listOf(
        DayForecast(LocalDate.of(2026, 7, 15), 18, 26, WeatherCondition.CLEAR, 0),
        DayForecast(LocalDate.of(2026, 5, 15), 12, 19, WeatherCondition.PARTLY_CLOUDY, 10),
        DayForecast(LocalDate.of(2026, 10, 15), 7, 14, WeatherCondition.PARTLY_CLOUDY, 10),
        DayForecast(LocalDate.of(2026, 1, 15), -3, 3, WeatherCondition.CLOUDY, 20),
    )
}
