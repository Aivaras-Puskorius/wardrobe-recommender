package app.wardrobe.domain.engine

import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import app.wardrobe.domain.model.Rating.DISLIKED
import app.wardrobe.domain.model.Rating.LIKED
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The taste rules of docs/ENGINE.md section 13. Expected values come from docs/taste_prototype.py. */
class TasteTest {

    private val byId = SampleData.items.associateBy { it.id }
    private var clock = Instant.parse("2026-11-21T08:00:00Z")

    private fun items(vararg ids: String) = ids.map { byId.getValue(it) }

    /** A rating given a minute after the previous one. */
    private fun rated(rating: Rating, ids: List<String>) =
        OutfitRating(ids.toSet(), rating, clock).also { clock = clock.plusSeconds(60) }

    private fun taste(vararg ratings: OutfitRating, items: List<ClothingItem> = SampleData.items, since: Instant? = null) =
        Taste.of(ratings.toList(), items, since)

    private fun colors(a: ItemColor, b: ItemColor) = TastePair.Colors(a, b)
    private fun garments(a: Garment, b: Garment) = TastePair.Garments(a, b)

    private val outfit1 = listOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat")
    private val outfit2 = listOf("oxford-shirt", "dark-jeans", "black-sneakers", "denim-jacket")
    private val outfit3 = listOf("olive-overshirt", "dark-jeans", "black-sneakers", "olive-rain-jacket")

    private val outfit3Pairs = setOf(
        colors(ItemColor.DENIM, ItemColor.OLIVE),
        colors(ItemColor.BLACK, ItemColor.OLIVE),
        colors(ItemColor.BLACK, ItemColor.DENIM),
        garments(Garment.SHIRT, Garment.JEANS),
        garments(Garment.SHIRT, Garment.SNEAKERS),
        garments(Garment.JEANS, Garment.SNEAKERS),
    )

    /** Example 3 of section 13.9: likes of outfits 3 and 2, a dislike of outfit 1. */
    private fun example3() = taste(rated(LIKED, outfit3), rated(LIKED, outfit2), rated(DISLIKED, outfit1))

    @Test
    fun weightsFollowTheTable() {
        assertEquals(1 / 3.0, PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 1, 0).weight, 1e-9)
        assertEquals(0.5, PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 2, 0).weight, 1e-9)
        assertEquals(2 / 3.0, PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 4, 0).weight, 1e-9)
        assertEquals(0.0, PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 1, 1).weight, 1e-9)
        assertEquals(0.2, PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 2, 1).weight, 1e-9)
        assertEquals(-1 / 3.0, PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 0, 1).weight, 1e-9)
    }

    /** T29, first part. */
    @Test
    fun aLikeTeachesTheSixPairsOfItsIndoorLook() {
        val taste = taste(rated(LIKED, outfit3))
        assertEquals(outfit3Pairs.map { PairTaste(it, 1, 0) }.toSet(), taste.pairs.toSet())
        assertEquals(1, taste.ratingCount)
    }

    /** T29, second part. */
    @Test
    fun example3TeachesTwelvePairs() {
        val expected = setOf(
            PairTaste(garments(Garment.SHIRT, Garment.JEANS), 2, 0),
            PairTaste(garments(Garment.SHIRT, Garment.SNEAKERS), 2, 0),
            PairTaste(colors(ItemColor.BLACK, ItemColor.LIGHT_BLUE), 1, 0),
            PairTaste(colors(ItemColor.BLACK, ItemColor.OLIVE), 1, 0),
            PairTaste(colors(ItemColor.DENIM, ItemColor.LIGHT_BLUE), 1, 0),
            PairTaste(colors(ItemColor.DENIM, ItemColor.OLIVE), 1, 0),
            PairTaste(colors(ItemColor.BLACK, ItemColor.DENIM), 2, 1),
            PairTaste(garments(Garment.JEANS, Garment.SNEAKERS), 2, 1),
            PairTaste(colors(ItemColor.WHITE, ItemColor.BLACK), 0, 1),
            PairTaste(colors(ItemColor.WHITE, ItemColor.DENIM), 0, 1),
            PairTaste(garments(Garment.T_SHIRT, Garment.JEANS), 0, 1),
            PairTaste(garments(Garment.T_SHIRT, Garment.SNEAKERS), 0, 1),
        )
        assertEquals(expected, example3().pairs.toSet())
        assertEquals(3, example3().ratingCount)
    }

    /** T42, the order of the profile's lists. */
    @Test
    fun likedAndAvoidedPairsAreSortedForTheProfile() {
        val taste = example3()
        assertEquals(
            listOf(
                garments(Garment.SHIRT, Garment.JEANS),
                garments(Garment.SHIRT, Garment.SNEAKERS),
                colors(ItemColor.BLACK, ItemColor.LIGHT_BLUE),
                colors(ItemColor.BLACK, ItemColor.OLIVE),
                colors(ItemColor.DENIM, ItemColor.LIGHT_BLUE),
                colors(ItemColor.DENIM, ItemColor.OLIVE),
                colors(ItemColor.BLACK, ItemColor.DENIM),
                garments(Garment.JEANS, Garment.SNEAKERS),
            ),
            taste.liked.map { it.pair },
        )
        assertEquals(
            listOf(
                colors(ItemColor.WHITE, ItemColor.BLACK),
                colors(ItemColor.WHITE, ItemColor.DENIM),
                garments(Garment.T_SHIRT, Garment.JEANS),
                garments(Garment.T_SHIRT, Garment.SNEAKERS),
            ),
            taste.avoided.map { it.pair },
        )
    }

    @Test
    fun aRatedOutfitGetsTwoPoints() {
        val liked = taste(rated(LIKED, outfit3))
        assertEquals(ScoreTerm(Reason.TASTE_OUTFIT, 2, items(*outfit3.toTypedArray())), liked.term(items(*outfit3.toTypedArray())))
        val disliked = example3()
        assertEquals(ScoreTerm(Reason.TASTE_OUTFIT, -2, items(*outfit1.toTypedArray())), disliked.term(items(*outfit1.toTypedArray())))
    }

    /** T34: one like of outfit 3, then indoor looks that share 3 and 2 of its pairs. */
    @Test
    fun pairSumsRoundTowardZero() {
        val taste = taste(rated(LIKED, outfit3))
        assertEquals(
            ScoreTerm(Reason.TASTE_GARMENTS, 1, items("oxford-shirt", "dark-jeans")),
            taste.term(items("oxford-shirt", "dark-jeans", "white-sneakers")),
        )
        assertNull(taste.term(items("olive-overshirt", "dark-jeans", "brown-boots")))
        // Black with denim appears twice in this look and counts once.
        assertNull(taste.term(items("black-tee", "dark-jeans", "black-sneakers")))
    }

    /** T35: the liked indoor look with another jacket is not the rated outfit, but has all six pairs. */
    @Test
    fun theSameIndoorLookWithOtherOuterwearGetsTwoPoints() {
        val taste = taste(rated(LIKED, outfit3))
        assertEquals(
            ScoreTerm(Reason.TASTE_COLORS, 2, items("olive-overshirt", "dark-jeans")),
            taste.term(items("olive-overshirt", "dark-jeans", "black-sneakers", "camel-coat")),
        )
    }

    /** The named pair is the strongest one: here denim with black, the third color pair. */
    @Test
    fun theTermNamesTheStrongestPair() {
        val taste = taste(rated(LIKED, outfit3))
        assertEquals(
            ScoreTerm(Reason.TASTE_COLORS, 1, items("dark-jeans", "black-sneakers")),
            taste.term(items(*outfit2.toTypedArray())),
        )
    }

    /** T36. */
    @Test
    fun pointsAreCappedAtTwo() {
        val taste = taste(
            rated(LIKED, outfit3),
            rated(LIKED, outfit2),
            rated(LIKED, listOf("oxford-shirt", "dark-jeans", "brown-boots", "olive-rain-jacket")),
            rated(LIKED, listOf("olive-overshirt", "dark-jeans", "white-sneakers", "camel-coat")),
        )
        assertEquals(2, taste.term(items("olive-overshirt", "dark-jeans", "black-sneakers", "camel-coat"))?.points)
    }

    @Test
    fun pointsRoundTheSumBeforeCuttingTheFraction() {
        // Four pairs at −2/3, −2/3, −1/3 and −1/3 make exactly −2, but doubles add them up to −1.9999999999999998.
        assertEquals(-2, tastePoints(-2 / 3.0 + -2 / 3.0 + -1 / 3.0 + -1 / 3.0))
        assertEquals(1, tastePoints(1 / 3.0 + 1 / 3.0 + 1 / 3.0))
        assertEquals(0, tastePoints(-0.93))
        assertEquals(-1, tastePoints(-4 / 3.0))
        assertEquals(2, tastePoints(3.2))
        assertEquals(-2, tastePoints(-3.2))
    }

    /** T37, the taste part: only the dislike after Start over counts. */
    @Test
    fun ratingsBeforeStartOverDoNotCount() {
        val like = rated(LIKED, outfit3)
        val since = clock
        val dislike = rated(DISLIKED, outfit2)
        val taste = taste(like, dislike, since = since)
        assertEquals(1, taste.ratingCount)
        assertEquals(-1, taste.term(items(*outfit3.toTypedArray()))?.points)
        assertEquals(-2, taste.term(items(*outfit2.toTypedArray()))?.points)
    }

    @Test
    fun aRatingGivenAtTheStartOverTimeCounts() {
        val like = rated(LIKED, outfit3)
        assertEquals(1, taste(like, since = like.ratedAt).ratingCount)
    }

    /** T38. */
    @Test
    fun aRatingWithADeletedItemTeachesNothing() {
        val wardrobe = SampleData.items.filterNot { it.id == "olive-rain-jacket" }
        val taste = taste(rated(LIKED, outfit3), items = wardrobe)
        assertEquals(emptyList<PairTaste>(), taste.pairs)
        assertEquals(0, taste.ratingCount)
    }

    @Test
    fun aRatingWithoutOneTopBottomsAndShoesTeachesNothing() {
        // The olive overshirt was edited into a jacket after the rating.
        val wardrobe = SampleData.items.map { if (it.id == "olive-overshirt") it.copy(garment = Garment.JACKET) else it }
        val taste = taste(rated(LIKED, outfit3), items = wardrobe)
        assertEquals(0, taste.ratingCount)
    }

    /** T40. */
    @Test
    fun anEditedColorChangesWhatTheRatingTeaches() {
        val wardrobe = SampleData.items.map { if (it.id == "olive-overshirt") it.copy(color = ItemColor.GREEN) else it }
        val taste = taste(rated(LIKED, outfit3), items = wardrobe)
        val colorPairs = taste.pairs.map { it.pair }.filterIsInstance<TastePair.Colors>().toSet()
        assertEquals(
            setOf(
                colors(ItemColor.DENIM, ItemColor.GREEN),
                colors(ItemColor.BLACK, ItemColor.GREEN),
                colors(ItemColor.BLACK, ItemColor.DENIM),
            ),
            colorPairs,
        )
        val edited = outfit3.map { id -> wardrobe.first { it.id == id } }
        assertEquals(ScoreTerm(Reason.TASTE_OUTFIT, 2, edited), taste.term(edited))
    }

    @Test
    fun noRatingsGiveNoTerm() {
        assertNull(Taste.NONE.term(items(*outfit3.toTypedArray())))
        assertEquals(0, Taste.NONE.ratingCount)
    }
}
