package app.wardrobe.data

import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** What every [RatingRepository] must do. Each implementation's test extends this class. */
abstract class RatingRepositoryContract {

    /** A new, empty repository whose clock reads [now]. */
    protected abstract fun createRepository(now: () -> Instant): RatingRepository

    private var now = Instant.parse("2026-10-29T08:00:00Z")
    private val repository by lazy { createRepository { now } }
    private val outfit = setOf("white-tee", "dark-jeans", "white-sneakers")
    private val other = setOf("black-tee", "dark-jeans", "brown-boots")

    private suspend fun ratings() = repository.ratings.first()

    @Test
    fun startsEmpty() = runTest {
        assertEquals(emptyList<OutfitRating>(), ratings())
    }

    @Test
    fun keepsTheRatingAndWhenItWasGiven() = runTest {
        repository.rate(outfit, Rating.LIKED)
        assertEquals(listOf(OutfitRating(outfit, Rating.LIKED, now)), ratings())
    }

    @Test
    fun rateAgainReplacesTheRating() = runTest {
        repository.rate(outfit, Rating.LIKED)
        now = now.plusSeconds(60)
        repository.rate(outfit, Rating.DISLIKED)
        assertEquals(listOf(OutfitRating(outfit, Rating.DISLIKED, now)), ratings())
    }

    @Test
    fun clearRemovesTheRating() = runTest {
        repository.rate(outfit, Rating.LIKED)
        repository.rate(other, Rating.DISLIKED)
        repository.clear(outfit)
        assertEquals(listOf(other), ratings().map { it.itemIds })
    }

    @Test
    fun theNewestRatingComesFirst() = runTest {
        repository.rate(outfit, Rating.LIKED)
        now = now.plusSeconds(60)
        repository.rate(other, Rating.LIKED)
        assertEquals(listOf(other, outfit), ratings().map { it.itemIds })
    }

    @Test
    fun theOrderOfTheItemsDoesNotMatter() = runTest {
        repository.rate(setOf("b", "a", "c"), Rating.LIKED)
        repository.clear(setOf("c", "b", "a"))
        assertEquals(emptyList<OutfitRating>(), ratings())
    }
}
