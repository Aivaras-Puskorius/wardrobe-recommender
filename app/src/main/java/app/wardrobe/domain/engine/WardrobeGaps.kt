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
import java.util.stream.Collectors

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
     *
     * PERF: 0.15 s for 17 items, 0.3 s for 34 and 1.2 s for 51 on the desktop JVM with 16 cores (2026-11-21), after
     * [Search] began reusing each item's ranking and candidates ran in parallel. Before, every engine run ranked every
     * outfit again: 1.9 s, 21 s and 101 s. Not timed on the phone, which has 8 cores.
     */
    fun find(wardrobe: List<ClothingItem>): List<Gap> {
        val search = Search(allYear(wardrobe))
        // Each candidate is scored on its own, so they run on all cores. The list keeps the candidates' order.
        val scored = candidates(wardrobe).parallelStream()
            .map { it to search.score(it) }
            .collect(Collectors.toList())
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
                .map { color -> newItem(garment, color) }
        }

    /**
     * Seasons play no part (user, 2026-11-21): a new item would otherwise get a head start over the user's own,
     * which only count in the seasons marked on them.
     */
    internal fun allYear(items: List<ClothingItem>) = items.map { it.copy(seasons = Season.entries.toSet()) }

    /** Two colors closer than [NEAR_COLOR] count as the same. */
    internal fun near(a: ItemColor, b: ItemColor) = Lab.of(a.argb).distanceTo(Lab.of(b.argb)) < NEAR_COLOR

    internal data class Score(val goesWith: Int, val itemDays: Int)

    /**
     * Scores new items against an [allYear] [wardrobe]: the items whose three outfits include the new one on at least
     * one reference day, and on how many item-days. Each item's ranking for each day is worked out once. A new item
     * only adds the outfits that have it, so those are ranked and merged in, and the top three picked from the merge,
     * as a full engine run with the new item in the wardrobe would pick them.
     */
    internal class Search(private val wardrobe: List<ClothingItem>) {

        private val rankings: List<List<List<ScoredOutfit>>> = wardrobe.map { anchor ->
            REFERENCE_DAYS.values.map { day -> outfitsOf(OutfitEngine.rank(wardrobe, anchor, day)) }
        }

        fun score(new: ClothingItem): Score {
            val withNew = wardrobe + new
            var itemDays = 0
            val goesWith = wardrobe.indices.count { i ->
                val anchor = wardrobe[i]
                // The anchor holds its own slot, so a new item of the same category is never in its outfits.
                if (anchor.category == new.category) return@count false
                val days = REFERENCE_DAYS.values.withIndex().count { (d, day) ->
                    val withIt = outfitsOf(OutfitEngine.rank(withNew, anchor, day, mustInclude = new))
                    withIt.isNotEmpty() &&
                        OutfitEngine.pick(merged(rankings[i][d], withIt), anchor).any { new in it.items }
                }
                itemDays += days
                days > 0
            }
            return Score(goesWith, itemDays)
        }

        private fun outfitsOf(result: EngineResult) = (result as? EngineResult.Outfits)?.outfits.orEmpty()

        /** Two lists in ranking order, merged in that order. */
        private fun merged(a: List<ScoredOutfit>, b: List<ScoredOutfit>) = sequence {
            var i = 0
            var j = 0
            while (i < a.size || j < b.size) {
                if (j == b.size || (i < a.size && OutfitEngine.RANKING.compare(a[i], b[j]) <= 0)) yield(a[i++]) else yield(b[j++])
            }
        }
    }

    private fun newItem(garment: Garment, color: ItemColor) = ClothingItem(
        id = newItemId(garment, color),
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

    /**
     * WARN: the id sorts after every real id on purpose. Where outfits tie on score, the engine orders them by item
     * id, so the user's own outfits come first and a new item counts only where it does better.
     */
    internal fun newItemId(garment: Garment, color: ItemColor) = "\uFFFF${garment.name}-${color.name}"

    /** One dry day per band, so the result doesn't change with this week's forecast. */
    internal val REFERENCE_DAYS = mapOf(
        DayBand.HOT to DayForecast(LocalDate.of(2026, 7, 15), 18, 26, WeatherCondition.CLEAR, 0),
        DayBand.MILD to DayForecast(LocalDate.of(2026, 5, 15), 12, 19, WeatherCondition.PARTLY_CLOUDY, 10),
        DayBand.COOL to DayForecast(LocalDate.of(2026, 10, 15), 7, 14, WeatherCondition.PARTLY_CLOUDY, 10),
        DayBand.COLD to DayForecast(LocalDate.of(2026, 1, 15), -3, 3, WeatherCondition.CLOUDY, 20),
    )
}
