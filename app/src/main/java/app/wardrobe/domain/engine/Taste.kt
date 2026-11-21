package app.wardrobe.domain.engine

import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import java.time.Instant
import kotlin.math.roundToLong

/** Two colors or two garments that an indoor look puts together. The rules are in docs/ENGINE.md section 13. */
sealed interface TastePair {
    /** Two colors in the app's color order, or one color twice for two items of that color. */
    data class Colors(val first: ItemColor, val second: ItemColor) : TastePair

    /** Two garments in outfit order: top, bottoms, shoes. */
    data class Garments(val first: Garment, val second: Garment) : TastePair
}

/** What the counted ratings say about a [pair]: how many outfits with it were liked and disliked. */
data class PairTaste(val pair: TastePair, val likes: Int, val dislikes: Int) {
    /** From −1 to +1, never reaching either. A pair with few ratings stays small. */
    val weight: Double get() = (likes - dislikes).toDouble() / (likes + dislikes + PRIOR_RATINGS)
}

/**
 * What the user's ratings teach about the colors and garments they like together.
 *
 * INFO(limit): taste learns only from outfits the user was shown and rated, so a pair never shown stays at 0.
 * More outfits is how the user sees others.
 */
class Taste private constructor(
    private val byPair: Map<TastePair, PairTaste>,
    private val rated: Map<Set<String>, Rating>,
) {

    /** Every pair the counted ratings have. */
    val pairs: List<PairTaste> get() = byPair.values.toList()

    /** How many ratings count. */
    val ratingCount: Int get() = rated.size

    /** Pairs with a weight above 0, highest first. */
    val liked: List<PairTaste>
        get() = pairs.filter { it.weight > 0 }.sortedWith(compareByDescending<PairTaste> { it.weight }.then(PROFILE_TIES))

    /** Pairs with a weight below 0, lowest first. */
    val avoided: List<PairTaste>
        get() = pairs.filter { it.weight < 0 }.sortedWith(compareBy<PairTaste> { it.weight }.then(PROFILE_TIES))

    /** The taste term of an outfit with [items] in display order, or null when taste gives it no points. */
    fun term(items: List<ClothingItem>): ScoreTerm? {
        rated[items.map { it.id }.toSet()]?.let { rating ->
            return ScoreTerm(Reason.TASTE_OUTFIT, if (rating == Rating.LIKED) MAX_POINTS else -MAX_POINTS, items)
        }
        val links = indoorLook(items)?.let(::links) ?: return null
        val weights = links.map { byPair[it.pair]?.weight ?: 0.0 }
        val points = tastePoints(weights.sum())
        if (points == 0) return null
        // The pair that pulls hardest in the direction of the points. Only a positive one is shown.
        val named = links[weights.indexOf(if (points > 0) weights.max() else weights.min())]
        val reason = if (named.pair is TastePair.Colors) Reason.TASTE_COLORS else Reason.TASTE_GARMENTS
        return ScoreTerm(reason, points, listOf(named.first, named.second))
    }

    companion object {
        /** No ratings. */
        val NONE = Taste(emptyMap(), emptyMap())

        /** The taste of the [ratings] given at or after [since], read with the wardrobe's current [items]. */
        fun of(ratings: List<OutfitRating>, items: List<ClothingItem>, since: Instant? = null): Taste {
            val byId = items.associateBy { it.id }
            val counts = mutableMapOf<TastePair, PairTaste>()
            val rated = mutableMapOf<Set<String>, Rating>()
            for (rating in ratings) {
                if (since != null && rating.ratedAt.isBefore(since)) continue
                // An outfit with a deleted item, or one edited out of shape, teaches nothing.
                val outfit = rating.itemIds.mapNotNull { byId[it] }
                if (outfit.size != rating.itemIds.size) continue
                val indoor = indoorLook(outfit) ?: continue
                rated[rating.itemIds] = rating.rating
                for (pair in links(indoor).map { it.pair }) {
                    val known = counts[pair] ?: PairTaste(pair, 0, 0)
                    counts[pair] = when (rating.rating) {
                        Rating.LIKED -> known.copy(likes = known.likes + 1)
                        Rating.DISLIKED -> known.copy(dislikes = known.dislikes + 1)
                    }
                }
            }
            return Taste(counts, rated)
        }
    }
}

/** Taste points for a sum of pair weights: rounded toward zero, capped at −2 and +2. */
internal fun tastePoints(sum: Double): Int {
    // Rounded to 6 decimals first, so weights that add up to exactly 2 don't land on 1.9999999999999998.
    val rounded = (sum * 1_000_000).roundToLong() / 1_000_000.0
    return rounded.toInt().coerceIn(-MAX_POINTS, MAX_POINTS)
}

/** A pair of two items of an indoor look. */
private class Link(val pair: TastePair, val first: ClothingItem, val second: ClothingItem)

/** The top, bottoms and shoes, or null unless the outfit has exactly one of each. */
private fun indoorLook(outfit: List<ClothingItem>): List<ClothingItem>? =
    listOf(Category.TOP, Category.BOTTOM, Category.FOOTWEAR).map { category ->
        outfit.singleOrNull { it.category == category } ?: return null
    }

/** The distinct pairs of an indoor look, in the order that breaks ties: colors, then garments. */
private fun links(indoor: List<ClothingItem>): List<Link> {
    val (top, bottom, shoes) = indoor
    val itemPairs = listOf(top to bottom, top to shoes, bottom to shoes)
    val colors = itemPairs.map { (a, b) ->
        Link(TastePair.Colors(minOf(a.color, b.color), maxOf(a.color, b.color)), a, b)
    }
    val garments = itemPairs.map { (a, b) -> Link(TastePair.Garments(a.garment, b.garment), a, b) }
    return (colors + garments).distinctBy { it.pair }
}

/** Ties in the profile's lists: more ratings first, then color pairs, then the app's color and garment order. */
private val PROFILE_TIES = compareByDescending<PairTaste> { it.likes + it.dislikes }
    .thenBy { if (it.pair is TastePair.Colors) 0 else 1 }
    .thenBy { it.pair.ordinals.first }
    .thenBy { it.pair.ordinals.second }

private val TastePair.ordinals: Pair<Int, Int>
    get() = when (this) {
        is TastePair.Colors -> first.ordinal to second.ordinal
        is TastePair.Garments -> first.ordinal to second.ordinal
    }

/** Ratings a pair is assumed to have at 0, so one rating moves it a third of the way. */
private const val PRIOR_RATINGS = 2

private const val MAX_POINTS = 2
