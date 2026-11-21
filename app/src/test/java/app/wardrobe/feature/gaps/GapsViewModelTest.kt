package app.wardrobe.feature.gaps

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Garment
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
class GapsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun GapsViewModel.ready() = uiState.filterIsInstance<GapsUiState.Ready>().first()

    @Test
    fun theSampleWardrobeGetsFourRows() = runTest {
        val state = GapsViewModel(InMemoryWardrobeRepository(SampleData.items), dispatcher).ready()
        assertEquals(17, state.itemCount)
        assertEquals(
            listOf(Garment.SNEAKERS, Garment.COAT, Garment.LONG_SLEEVE, Garment.JEANS),
            state.gaps.map { it.garment },
        )
    }

    @Test
    fun topsAloneGetNoRows() = runTest {
        val repository = InMemoryWardrobeRepository(SampleData.items.filter { it.id in setOf("white-tee", "black-tee") })
        val viewModel = GapsViewModel(repository, dispatcher)
        assertEquals(GapsUiState.Ready(2, emptyList()), viewModel.ready())
    }
}
