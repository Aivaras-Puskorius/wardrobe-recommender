package app.wardrobe.feature.outfits

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.domain.engine.Reason
import app.wardrobe.domain.engine.ScoreTerm
import app.wardrobe.domain.engine.ScoredOutfit
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ItemColor
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitResultsViewModelTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val forecast = SampleData.forecast(today)
    private lateinit var repository: InMemoryWardrobeRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = InMemoryWardrobeRepository(SampleData.items)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(itemId: String, date: LocalDate = today) =
        OutfitResultsViewModel(itemId, date, repository, forecast)

    private suspend fun OutfitResultsViewModel.settled() = uiState.first { it != OutfitResultsUiState.Loading }

    private suspend fun OutfitResultsViewModel.ready() = settled() as OutfitResultsUiState.Ready

    @Test
    fun theEngineBuildsTheOutfits() = runTest {
        val state = viewModel("dark-jeans").ready()
        assertEquals("dark-jeans", state.anchor.id)
        assertEquals(
            listOf(
                listOf("camel-coat", "black-tee", "dark-jeans", "white-sneakers"),
                listOf("camel-coat", "oxford-shirt", "dark-jeans", "black-sneakers"),
                listOf("denim-jacket", "cream-knit", "dark-jeans", "black-sneakers"),
            ),
            state.outfits.map { outfit -> outfit.items.map { it.id } },
        )
        assertEquals(
            listOf(
                OutfitTitle(ColorStory.ALL_NEUTRALS),
                OutfitTitle(ColorStory.ACCENT, ItemColor.LIGHT_BLUE),
                OutfitTitle(ColorStory.ACCENT, ItemColor.BLUE),
            ),
            state.outfits.map { it.title },
        )
        assertEquals(
            listOf(Reason.NEUTRAL_BASE, Reason.LAYERS_FIT, Reason.LIGHT_DARK),
            state.outfits.first().why.map { it.reason },
        )
    }

    @Test
    fun anyItemCanGetOutfits() = runTest {
        assertTrue(viewModel("black-tee").ready().outfits.isNotEmpty())
    }

    @Test
    fun anItemThatFitsNoDayGetsNoOutfitsAndNoMissingCategory() = runTest {
        val state = viewModel("khaki-shorts").ready()
        assertTrue(state.outfits.isEmpty())
        assertNull(state.missing)
    }

    @Test
    fun aMissingCategoryIsReported() = runTest {
        SampleData.items.filter { it.category == Category.FOOTWEAR }.forEach { repository.delete(it.id) }
        val state = viewModel("dark-jeans").ready()
        assertTrue(state.outfits.isEmpty())
        assertEquals(Category.FOOTWEAR, state.missing)
    }

    @Test
    fun aMissingItemIsNotFound() = runTest {
        assertEquals(OutfitResultsUiState.NotFound, viewModel("missing").settled())
    }

    @Test
    fun showsTheWeatherOfTheChosenDay() = runTest {
        assertEquals(today.plusDays(1), viewModel("dark-jeans", today.plusDays(1)).ready().day.date)
    }

    @Test
    fun deletingAnItemRebuildsTheOutfitsWithoutIt() = runTest {
        repository.delete("camel-coat")
        val outfits = viewModel("dark-jeans").ready().outfits
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.none { outfit -> outfit.items.any { it.id == "camel-coat" } })
    }

    @Test
    fun ratingAgainClearsItAndTheOtherRatingReplacesIt() = runTest {
        val viewModel = viewModel("dark-jeans")
        val id = viewModel.ready().outfits[1].id
        viewModel.rate(id, Rating.LIKED)
        assertEquals(Rating.LIKED, viewModel.ready().outfits.first { it.id == id }.rating)

        viewModel.rate(id, Rating.DISLIKED)
        assertEquals(Rating.DISLIKED, viewModel.ready().outfits.first { it.id == id }.rating)

        viewModel.rate(id, Rating.DISLIKED)
        assertNull(viewModel.ready().outfits.first { it.id == id }.rating)
    }

    @Test
    fun outfitsWithTheSameTitleAreNumbered() {
        fun accent(color: ItemColor) = ScoredOutfit(
            items = emptyList(),
            terms = listOf(ScoreTerm(Reason.SINGLE_ACCENT, 3, listOf(SampleData.items.first().copy(color = color)))),
        )
        val neutral = ScoredOutfit(emptyList(), listOf(ScoreTerm(Reason.NEUTRAL_BASE, 3)))
        val unrelated = ScoredOutfit(emptyList(), listOf(ScoreTerm(Reason.LAYERS_FIT, 2)))
        assertEquals(
            listOf(
                OutfitTitle(ColorStory.ACCENT, ItemColor.OLIVE),
                OutfitTitle(ColorStory.ALL_NEUTRALS),
                OutfitTitle(ColorStory.ACCENT, ItemColor.OLIVE, number = 2),
                OutfitTitle(ColorStory.MIXED),
            ),
            outfitTitles(listOf(accent(ItemColor.OLIVE), neutral, accent(ItemColor.OLIVE), unrelated)),
        )
    }
}
