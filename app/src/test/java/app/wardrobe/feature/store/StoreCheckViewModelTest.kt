package app.wardrobe.feature.store

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
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
class StoreCheckViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val navyCoat = ClothingItem(
        "shop", "", Garment.COAT, ItemColor.NAVY, Garment.COAT.defaultMaterial, Fit.REGULAR, Garment.COAT.defaultWarmth,
        emptySet(), photo = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checksTheItemAgainstTheWardrobe() = runTest {
        val viewModel = StoreCheckViewModel(InMemoryWardrobeRepository(SampleData.items), navyCoat, dispatcher)
        val result = viewModel.uiState.filterIsInstance<StoreCheckUiState.Ready>().first().result
        assertEquals(11, result.goesWith)
        assertEquals(17, result.itemCount)
    }
}
