package app.wardrobe.feature.item

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditItemViewModelTest {

    private val tee = SampleData.items.first { it.id == "black-tee" }
    private lateinit var repository: InMemoryWardrobeRepository
    private lateinit var viewModel: EditItemViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = InMemoryWardrobeRepository(SampleData.items)
        viewModel = EditItemViewModel(tee.id, repository, defaultName = { garment, color -> "$color $garment" })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun editing() = viewModel.uiState.value as EditItemUiState.Editing

    @Test
    fun loadsTheItemIntoTheForm() {
        val form = editing().form
        assertEquals(tee, form.toItem(tee.id))
        assertTrue(form.nameEditedByUser)
        assertNull(form.suggestion)
        assertFalse(editing().hasChanges)
    }

    @Test
    fun unknownIdIsNotFound() {
        val missing = EditItemViewModel("missing", repository, defaultName = { _, _ -> "" })
        assertEquals(EditItemUiState.NotFound, missing.uiState.value)
    }

    @Test
    fun hasChangesOnlyWhileTheFormDiffersFromTheItem() {
        viewModel.update(ItemFormChange.SetColor(ItemColor.NAVY))
        assertTrue(editing().hasChanges)
        viewModel.update(ItemFormChange.SetColor(tee.color))
        assertFalse(editing().hasChanges)
    }

    @Test
    fun changingTheColorKeepsTheName() {
        viewModel.update(ItemFormChange.SetColor(ItemColor.NAVY))
        assertEquals(tee.name, editing().form.name)
    }

    @Test
    fun changingTheGarmentKeepsTheSavedMaterial() {
        val jacket = SampleData.items.first { it.id == "olive-rain-jacket" }
        val editJacket = EditItemViewModel(jacket.id, repository, defaultName = { _, _ -> "" })
        editJacket.update(ItemFormChange.SetGarment(Garment.COAT))
        assertEquals(Material.WATERPROOF, (editJacket.uiState.value as EditItemUiState.Editing).form.material)
    }

    @Test
    fun saveStoresANewMaterial() = runTest {
        viewModel.update(ItemFormChange.SetMaterial(Material.LINEN))
        assertEquals(tee.copy(material = Material.LINEN), viewModel.save())
    }

    @Test
    fun saveUpdatesTheItemUnderTheSameId() = runTest {
        viewModel.update(ItemFormChange.SetName("Favorite black tee"))
        viewModel.update(ItemFormChange.ToggleSeason(Season.WINTER))

        val saved = viewModel.save()

        assertEquals(tee.copy(name = "Favorite black tee", seasons = tee.seasons + Season.WINTER), saved)
        assertEquals(saved, repository.item(tee.id).first())
    }

    @Test
    fun saveRejectsAnInvalidForm() = runTest {
        viewModel.update(ItemFormChange.SetName(""))
        assertNull(viewModel.save())
        assertTrue(editing().form.showErrors)
        assertEquals(tee, repository.item(tee.id).first())
    }
}
