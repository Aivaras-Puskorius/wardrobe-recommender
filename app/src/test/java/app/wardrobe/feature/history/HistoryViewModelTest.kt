package app.wardrobe.feature.history

import app.wardrobe.data.InMemoryRatingRepository
import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
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

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private var now = Instant.parse("2026-10-29T10:00:00Z")
    private val wardrobe = InMemoryWardrobeRepository(SampleData.items)
    private val ratings = InMemoryRatingRepository { now }
    private lateinit var viewModel: HistoryViewModel

    /** Given out of display order, to check the screen sorts them. */
    private val jeansOutfit = setOf("camel-coat", "white-sneakers", "dark-jeans", "black-tee")
    private val skirtOutfit = setOf("black-tee", "grey-skirt", "white-sneakers")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = HistoryViewModel(wardrobe, ratings) { ZoneId.of("Europe/Vilnius") }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ready() = viewModel.uiState.filterIsInstance<HistoryUiState.Ready>().first()

    @Test
    fun listsRatedOutfitsNewestFirstWithItemsInDisplayOrder() = runTest {
        ratings.rate(jeansOutfit, Rating.LIKED)
        now = Instant.parse("2026-10-29T23:30:00Z")
        ratings.rate(skirtOutfit, Rating.DISLIKED)

        val outfits = ready().outfits
        assertEquals(listOf(Rating.DISLIKED, Rating.LIKED), outfits.map { it.rating })
        assertEquals(
            listOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat"),
            outfits[1].items.map { it.id },
        )
        // 23:30 in UTC is already the next day in Vilnius.
        assertEquals(listOf(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 29)), outfits.map { it.ratedOn })
    }

    @Test
    fun likedShowsOnlyLikedOutfits() = runTest {
        ratings.rate(jeansOutfit, Rating.LIKED)
        ratings.rate(skirtOutfit, Rating.DISLIKED)
        viewModel.setFilter(HistoryFilter.LIKED)

        val state = ready()
        assertEquals(HistoryFilter.LIKED, state.filter)
        assertEquals(listOf(Rating.LIKED), state.outfits.map { it.rating })
    }

    @Test
    fun anOutfitWithADeletedItemDropsOutAndComesBackOnUndo() = runTest {
        ratings.rate(jeansOutfit, Rating.LIKED)
        val coat = SampleData.items.first { it.id == "camel-coat" }
        wardrobe.delete(coat.id)
        assertEquals(emptyList<RatedOutfit>(), ready().outfits)

        wardrobe.add(coat)
        assertEquals(1, ready().outfits.size)
    }

    @Test
    fun theSameRatingAgainClearsIt() = runTest {
        ratings.rate(jeansOutfit, Rating.LIKED)
        val id = ready().outfits.single().id
        viewModel.rate(id, Rating.DISLIKED)
        assertEquals(Rating.DISLIKED, ready().outfits.single().rating)
        viewModel.rate(id, Rating.DISLIKED)
        assertEquals(emptyList<RatedOutfit>(), ready().outfits)
    }
}
