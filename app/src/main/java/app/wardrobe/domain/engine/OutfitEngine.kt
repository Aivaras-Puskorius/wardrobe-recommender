package app.wardrobe.domain.engine

import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Lab
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import app.wardrobe.domain.model.WeatherCondition
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min

/** Why an outfit gained or lost points. Ties between why lines follow this order. */
enum class Reason {
    LAYERS_FIT, RAIN_READY, WARM_WOOL, COOL_FABRIC,
    NEUTRAL_BASE, SINGLE_ACCENT, TONAL, ANALOGOUS, COMPLEMENTARY,
    BALANCED_FIT, LIGHT_DARK, OUTDOOR_HOLDS,
    TASTE_OUTFIT, TASTE_COLORS, TASTE_GARMENTS,
    OUT_OF_SEASON, RECENT_REPEAT,
}

private val COLOR_REASONS = setOf(Reason.NEUTRAL_BASE, Reason.SINGLE_ACCENT, Reason.TONAL, Reason.ANALOGOUS, Reason.COMPLEMENTARY)
private val TASTE_REASONS = setOf(Reason.TASTE_OUTFIT, Reason.TASTE_COLORS, Reason.TASTE_GARMENTS)

/** The kinds of color match of docs/ENGINE.md section 16.1. The three two-color terms are one kind (user, 2026-11-25). */
private enum class ColorKind { ALL_NEUTRAL, ONE_ACCENT, TWO_COLORS }

/** The outfit's kind of color match, or null for two accents that get no color term. */
private fun ScoredOutfit.colorKind(): ColorKind? = terms.firstNotNullOfOrNull { term ->
    when (term.reason) {
        Reason.NEUTRAL_BASE -> ColorKind.ALL_NEUTRAL
        Reason.SINGLE_ACCENT -> ColorKind.ONE_ACCENT
        Reason.TONAL, Reason.ANALOGOUS, Reason.COMPLEMENTARY -> ColorKind.TWO_COLORS
        else -> null
    }
}

/** One rule that applied to an outfit: its [points] and the [items] the rule is about. */
data class ScoreTerm(val reason: Reason, val points: Int, val items: List<ClothingItem> = emptyList())

/** An outfit with [items] in display order (top, bottom, footwear, outerwear) and the rules that scored it. */
data class ScoredOutfit(val items: List<ClothingItem>, val terms: List<ScoreTerm>) {
    val score: Int get() = terms.sumOf { it.points }

    /**
     * The reasons shown under the outfit: the three strongest positive terms. Taste that adds points always shows,
     * as a fourth line when three others outrank it, so it never pushes a weather reason out.
     */
    val why: List<ScoreTerm>
        get() {
            val positive = terms.filter { it.points > 0 }
                .sortedWith(compareByDescending<ScoreTerm> { it.points }.thenBy { it.reason.ordinal })
            val strongest = positive.take(MAX_WHY_LINES)
            val taste = positive.firstOrNull { it.reason in TASTE_REASONS }
            return if (taste == null || taste in strongest) strongest else strongest + taste
        }

    /** The term that describes the outfit's colors, or null when its two accents are unrelated. */
    val colorTerm: ScoreTerm? get() = terms.firstOrNull { it.reason in COLOR_REASONS }
}

sealed interface EngineResult {
    /** One to three outfits, best first. */
    data class Outfits(val outfits: List<ScoredOutfit>) : EngineResult

    /** The wardrobe has nothing in [category], which the day needs. */
    data class Missing(val category: Category) : EngineResult

    /** Every combination breaks a hard rule. */
    data object NothingFits : EngineResult
}

/** Builds and ranks outfits around one item. The rules and their reasons are in docs/ENGINE.md. */
object OutfitEngine {

    /**
     * Outfits around [anchor] for [day], at most [limit], best first. [recent] holds the item ids of outfits shown in
     * the last 7 days. A higher [limit] keeps the order and adds the next outfits after the first ones. [taste] adds
     * points from the user's ratings.
     *
     * INFO(limit): tries every combination, about 360 for the sample wardrobe. At a few hundred items,
     * filter each slot by the hard rules before combining.
     */
    fun suggest(
        wardrobe: List<ClothingItem>,
        anchor: ClothingItem,
        day: DayForecast,
        recent: List<Set<String>> = emptyList(),
        limit: Int = DEFAULT_LIMIT,
        taste: Taste = Taste.NONE,
    ): EngineResult = when (val ranked = rank(wardrobe, anchor, day, recent, taste)) {
        is EngineResult.Outfits -> EngineResult.Outfits(pick(ranked.outfits.asSequence(), anchor, limit))
        else -> ranked
    }

    /**
     * Every outfit around [anchor] that passes the hard rules, best first, or why there is none. With [mustInclude],
     * which must be in the [wardrobe], only the outfits that have it.
     */
    internal fun rank(
        wardrobe: List<ClothingItem>,
        anchor: ClothingItem,
        day: DayForecast,
        recent: List<Set<String>> = emptyList(),
        taste: Taste = Taste.NONE,
        mustInclude: ClothingItem? = null,
    ): EngineResult {
        val items = (wardrobe + anchor).distinctBy { it.id }
        for (category in listOf(Category.TOP, Category.BOTTOM, Category.FOOTWEAR)) {
            if (items.none { it.category == category }) return EngineResult.Missing(category)
        }
        fun slot(category: Category) = when (category) {
            anchor.category -> listOf(anchor)
            mustInclude?.category -> listOf(mustInclude)
            else -> items.filter { it.category == category }
        }

        val wet = day.condition == WeatherCondition.RAIN || day.condition == WeatherCondition.SNOW ||
            day.precipitationChancePercent >= WET_CHANCE_PERCENT
        val low = day.minTemperatureC
        val outerwear = slot(Category.OUTERWEAR)
        // On these days every outfit has outerwear, so OUTDOOR_HOLDS can't favor wearing it when it's optional.
        val outerwearNeeded = anchor.category == Category.OUTERWEAR || low < MILD_LOW || wet
        val outerwearOptions: List<ClothingItem?> = when {
            anchor.category == Category.OUTERWEAR -> outerwear
            low < MILD_LOW || wet -> outerwear.ifEmpty { return EngineResult.Missing(Category.OUTERWEAR) }
            low < HOT_LOW -> listOf(null) + outerwear
            else -> listOf(null)
        }.let { options -> if (mustInclude?.category == Category.OUTERWEAR) options.filterNotNull() else options }

        val candidates = buildList {
            for (outer in outerwearOptions) for (top in slot(Category.TOP)) for (bottom in slot(Category.BOTTOM)) {
                for (footwear in slot(Category.FOOTWEAR)) {
                    score(listOfNotNull(top, bottom, footwear, outer), anchor, day, wet, outerwearNeeded, recent, taste)
                        ?.let(::add)
                }
            }
        }
        if (candidates.isEmpty()) return EngineResult.NothingFits
        return EngineResult.Outfits(candidates.sortedWith(RANKING))
    }

    /**
     * Takes outfits from [ranked], which is in ranking order, until there are [limit], in pages of three as the results
     * screen shows them (docs/ENGINE.md section 16.2). The first two of a page are the best left. The third has a kind
     * of color match the page's first two don't have, when one is left, and otherwise is the best left too. Near copies
     * of picked outfits are skipped throughout, and a higher [limit] keeps the earlier pages.
     */
    internal fun pick(ranked: Sequence<ScoredOutfit>, anchor: ClothingItem, limit: Int = DEFAULT_LIMIT): List<ScoredOutfit> {
        // The ranking is read once and kept, since a page's third may look further down than its first two.
        val read = mutableListOf<ScoredOutfit>()
        val source = ranked.iterator()
        val outfits = sequence {
            var i = 0
            while (i < read.size || source.hasNext()) {
                if (i == read.size) read += source.next()
                yield(read[i++])
            }
        }
        val picked = mutableListOf<ScoredOutfit>()
        // A picked outfit differs from itself in no items, so this also skips the ones already picked.
        fun isNew(outfit: ScoredOutfit): Boolean {
            val others = outfit.items.map { it.id }.toSet() - anchor.id
            return picked.all { (others - it.items.map { item -> item.id }.toSet()).size >= MIN_DIFFERENT_ITEMS }
        }
        while (picked.size < limit) {
            val varied = if (picked.size % PAGE_SIZE == PAGE_SIZE - 1) {
                val page = picked.takeLast(PAGE_SIZE - 1).map { it.colorKind() }
                outfits.firstOrNull { outfit -> outfit.colorKind().let { it != null && it !in page } && isNew(outfit) }
            } else {
                null
            }
            picked += varied ?: outfits.firstOrNull(::isNew) ?: break
        }
        return picked
    }

    /** The order of section 5: score, then fewer accent colors indoors, then item ids. */
    internal val RANKING: Comparator<ScoredOutfit> = compareByDescending<ScoredOutfit> { it.score }
        .thenBy { it.accentColors().size }
        .thenComparator { a, b -> compareIds(a.sortedIds(), b.sortedIds()) }

    /** Applies the hard rules and scores the outfit, or returns null when it breaks a hard rule. */
    private fun score(
        items: List<ClothingItem>,
        anchor: ClothingItem,
        day: DayForecast,
        wet: Boolean,
        outerwearNeeded: Boolean,
        recent: List<Set<String>>,
        taste: Taste,
    ): ScoredOutfit? {
        val outer = items.firstOrNull { it.category == Category.OUTERWEAR }
        val top = items.first { it.category == Category.TOP }
        val bottom = items.first { it.category == Category.BOTTOM }
        val footwear = items.first { it.category == Category.FOOTWEAR }

        if (bottom.garment == Garment.SHORTS && day.maxTemperatureC < SHORTS_HIGH) return null
        if (day.condition == WeatherCondition.SNOW && footwear.garment != Garment.BOOTS) return null
        val layerWarmth = top.warmth.value + (outer?.warmth?.value ?: 0)
        val warmthGap = abs(layerWarmth - targetWarmth(day.minTemperatureC))
        if (warmthGap > 1) return null
        if (items.accents().size > MAX_ACCENTS) return null
        if (outer?.fit == Fit.FITTED && top.fit in ROOMY) return null
        // Light drizzle counts as cloudy, but a wool top still shouldn't get wet.
        if (day.drizzle && outer == null && top.material == Material.WOOL) return null
        // Outerwear comes off indoors, so the color rules judge the indoor look. The outerwear only adds OUTDOOR_HOLDS.
        val indoor = listOf(top, bottom, footwear)
        val accents = indoor.accents()

        val terms = mutableListOf<ScoreTerm>()
        terms += ScoreTerm(Reason.LAYERS_FIT, if (warmthGap == 0) 2 else 1, listOfNotNull(outer, top))
        if (wet && outer?.material == Material.WATERPROOF && footwear.material in RAIN_FOOTWEAR) {
            terms += ScoreTerm(Reason.RAIN_READY, 2, listOf(outer, footwear))
        }
        val wool = items.filter { it.material == Material.WOOL }
        if (day.minTemperatureC < COOL_LOW && wool.isNotEmpty()) terms += ScoreTerm(Reason.WARM_WOOL, 1, wool)
        if (day.maxTemperatureC >= WARM_HIGH && top.material in COOL_FABRICS) {
            terms += ScoreTerm(Reason.COOL_FABRIC, 1, listOf(top))
        }
        colorTerm(indoor, accents)?.let { terms += it }
        if ((top.fit in ROOMY && bottom.fit == Fit.FITTED) || (top.fit == Fit.FITTED && bottom.fit in ROOMY)) {
            terms += ScoreTerm(Reason.BALANCED_FIT, 1, listOf(top, bottom))
        }
        val light = indoor.filter { it.color.isLight }
        if (light.isNotEmpty() && indoor.any { it.color.isDark }) {
            terms += ScoreTerm(Reason.LIGHT_DARK, 1, listOf(light.maxBy { it.color.lightness }))
        }
        if (outerwearNeeded && outer != null && colorTerm(items, items.accents()) != null) {
            terms += ScoreTerm(Reason.OUTDOOR_HOLDS, 1, listOf(outer))
        }
        taste.term(items)?.let { terms += it }
        val season = seasonOf(day.date)
        items.filter { it.id != anchor.id && season !in it.seasons }.forEach {
            terms += ScoreTerm(Reason.OUT_OF_SEASON, -2, listOf(it))
        }
        val others = items.map { it.id }.toSet() - anchor.id
        if (recent.any { (others - (it - anchor.id)).size <= 1 }) terms += ScoreTerm(Reason.RECENT_REPEAT, -3)
        return ScoredOutfit(items, terms)
    }

    private fun colorTerm(items: List<ClothingItem>, accents: List<ItemColor>): ScoreTerm? = when (accents.size) {
        0 -> ScoreTerm(Reason.NEUTRAL_BASE, 3, items)
        1 -> ScoreTerm(Reason.SINGLE_ACCENT, 3, items.filter { it.color == accents[0] })
        else -> {
            val pair = accents.map { color -> items.first { it.color == color } }
            val distance = hueDistance(accents[0], accents[1])
            when {
                distance < TONAL_MAX -> ScoreTerm(Reason.TONAL, 2, pair)
                distance <= ANALOGOUS_MAX -> ScoreTerm(Reason.ANALOGOUS, 2, pair)
                distance >= COMPLEMENTARY_MIN -> ScoreTerm(Reason.COMPLEMENTARY, 2, pair)
                else -> null
            }
        }
    }

    private fun targetWarmth(low: Int) = when {
        low >= HOT_LOW -> 1
        low >= MILD_LOW -> 2
        low >= COOL_LOW -> 4
        else -> 5
    }

    private fun List<ClothingItem>.accents() = filterNot { it.color.isNeutral }.map { it.color }.distinct()

    /** The accent colors of the indoor look, used to break ties. */
    private fun ScoredOutfit.accentColors() = items.filter { it.category != Category.OUTERWEAR }.accents()

    private fun ScoredOutfit.sortedIds() = items.map { it.id }.sorted()

    /** Compares id lists element by element, so ties always break the same way. */
    private fun compareIds(a: List<String>, b: List<String>): Int {
        for (i in 0 until min(a.size, b.size)) {
            val byId = a[i].compareTo(b[i])
            if (byId != 0) return byId
        }
        return a.size - b.size
    }

    private const val DEFAULT_LIMIT = 3
    private const val MIN_DIFFERENT_ITEMS = 2

    /** The results screen shows three outfits at a time, and each three gets the variety rule. */
    private const val PAGE_SIZE = 3
    private const val MAX_ACCENTS = 2
    private const val WET_CHANCE_PERCENT = 50
    private const val SHORTS_HIGH = 20
    private const val WARM_HIGH = 22
    private const val HOT_LOW = 18
    private const val MILD_LOW = 12
    private const val COOL_LOW = 5
    private const val TONAL_MAX = 20.0
    private const val ANALOGOUS_MAX = 60.0
    private const val COMPLEMENTARY_MIN = 125.0
    private val RAIN_FOOTWEAR = setOf(Material.LEATHER, Material.WATERPROOF)
    private val COOL_FABRICS = setOf(Material.COTTON, Material.LINEN)
    private val ROOMY = setOf(Fit.LOOSE, Fit.OVERSIZED)
}

private const val MAX_WHY_LINES = 3

private val Warmth.value: Int
    get() = when (this) {
        Warmth.LIGHT -> 1
        Warmth.MEDIUM -> 2
        Warmth.WARM -> 3
    }

/** Northern hemisphere seasons by month. */
internal fun seasonOf(date: LocalDate): Season = when (date.monthValue) {
    in 3..5 -> Season.SPRING
    in 6..8 -> Season.SUMMER
    in 9..11 -> Season.AUTUMN
    else -> Season.WINTER
}

private val ItemColor.red get() = ((argb shr 16) and 0xFF) / 255.0
private val ItemColor.green get() = ((argb shr 8) and 0xFF) / 255.0
private val ItemColor.blue get() = (argb and 0xFF) / 255.0

/** Hue in degrees, 0 to 360, as in the HSV color model. 0 for greys. */
internal val ItemColor.hue: Double get() = HUES[ordinal]

/** CIELAB lightness L*: 0 is black, 100 is white. */
internal val ItemColor.lightness: Double get() = LIGHTNESS[ordinal]

// Worked out once per color: the engine reads them for every outfit it scores.
private val HUES = ItemColor.entries.map { it.computeHue() }.toDoubleArray()
private val LIGHTNESS = ItemColor.entries.map { Lab.of(it.argb).l }.toDoubleArray()

private fun ItemColor.computeHue(): Double {
    val max = maxOf(red, green, blue)
    val delta = max - minOf(red, green, blue)
    if (delta == 0.0) return 0.0
    val sector = when (max) {
        red -> ((green - blue) / delta).mod(6.0)
        green -> (blue - red) / delta + 2
        else -> (red - green) / delta + 4
    }
    return sector * 60
}

internal val ItemColor.isLight: Boolean get() = lightness >= 70
internal val ItemColor.isDark: Boolean get() = lightness <= 36

/** Distance between two hues around the color wheel, 0 to 180 degrees. */
private fun hueDistance(a: ItemColor, b: ItemColor): Double {
    val d = abs(a.hue - b.hue)
    return min(d, 360 - d)
}
