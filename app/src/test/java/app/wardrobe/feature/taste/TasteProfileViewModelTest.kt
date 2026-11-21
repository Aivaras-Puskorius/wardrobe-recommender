package app.wardrobe.feature.taste

import app.wardrobe.data.InMemoryRatingRepository
import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.domain.engine.PairTaste
import app.wardrobe.domain.engine.TastePair
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Rating
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** T42 and T43 of docs/ENGINE.md section 13.10, the view model part. */
@OptIn(ExperimentalCoroutinesApi::class)
class TasteProfileViewModelTest {

    private var now = Instant.parse("2026-11-21T08:00:00Z")
    private val wardrobe = InMemoryWardrobeRepository(SampleData.items)
    private val ratings = InMemoryRatingRepository { now }
    private lateinit var viewModel: TasteProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TasteProfileViewModel(wardrobe, ratings) { ZoneId.of("Europe/Vilnius") }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ready() = viewModel.uiState.filterIsInstance<TasteProfileUiState.Ready>().first()

    /** Example 3 of section 13.9: likes of outfits 3 and 2, a dislike of outfit 1. */
    private suspend fun rateExample3() {
        ratings.rate(setOf("olive-overshirt", "dark-jeans", "black-sneakers", "olive-rain-jacket"), Rating.LIKED)
        ratings.rate(setOf("oxford-shirt", "dark-jeans", "black-sneakers", "denim-jacket"), Rating.LIKED)
        ratings.rate(setOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat"), Rating.DISLIKED)
    }

    @Test
    fun withoutRatingsNothingIsLearned() = runTest {
        assertEquals(TasteProfileUiState.Ready(0, null, emptyList(), emptyList()), ready())
    }

    @Test
    fun showsUpToFiveLikedAndFiveAvoidedPairs() = runTest {
        rateExample3()
        val state = ready()
        assertEquals(3, state.ratingCount)
        assertEquals(null, state.since)
        assertEquals(
            listOf(
                PairTaste(TastePair.Garments(Garment.SHIRT, Garment.JEANS), 2, 0),
                PairTaste(TastePair.Garments(Garment.SHIRT, Garment.SNEAKERS), 2, 0),
                PairTaste(TastePair.Colors(ItemColor.BLACK, ItemColor.LIGHT_BLUE), 1, 0),
                PairTaste(TastePair.Colors(ItemColor.BLACK, ItemColor.OLIVE), 1, 0),
                PairTaste(TastePair.Colors(ItemColor.DENIM, ItemColor.LIGHT_BLUE), 1, 0),
            ),
            state.liked,
        )
        assertEquals(
            listOf(
                PairTaste(TastePair.Colors(ItemColor.WHITE, ItemColor.BLACK), 0, 1),
                PairTaste(TastePair.Colors(ItemColor.WHITE, ItemColor.DENIM), 0, 1),
                PairTaste(TastePair.Garments(Garment.T_SHIRT, Garment.JEANS), 0, 1),
                PairTaste(TastePair.Garments(Garment.T_SHIRT, Garment.SNEAKERS), 0, 1),
            ),
            state.avoided,
        )
    }

    @Test
    fun startOverForgetsTheTasteButKeepsTheRatings() = runTest {
        rateExample3()
        // 23:30 in UTC is already the next day in Vilnius.
        now = Instant.parse("2026-11-21T23:30:00Z")
        viewModel.startOver()
        assertEquals(TasteProfileUiState.Ready(0, LocalDate.of(2026, 11, 22), emptyList(), emptyList()), ready())
        assertEquals(3, ratings.ratings.first().size)
    }

    @Test
    fun ratingsAfterStartOverCount() = runTest {
        rateExample3()
        now = now.plusSeconds(60)
        viewModel.startOver()
        now = now.plusSeconds(60)
        ratings.rate(setOf("olive-overshirt", "dark-jeans", "black-sneakers", "olive-rain-jacket"), Rating.LIKED)
        val state = ready()
        assertEquals(1, state.ratingCount)
        assertEquals(LocalDate.of(2026, 11, 21), state.since)
        // The like teaches six pairs, and the list shows five.
        assertEquals(5, state.liked.size)
        assertEquals(emptyList<PairTaste>(), state.avoided)
    }
}
