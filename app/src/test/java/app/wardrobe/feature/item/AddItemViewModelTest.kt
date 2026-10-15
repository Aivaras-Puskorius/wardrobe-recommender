package app.wardrobe.feature.item

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
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
class AddItemViewModelTest {

    private val suggestion = ItemSuggestion(Garment.SWEATER, ItemColor.GREEN)
    private lateinit var repository: InMemoryWardrobeRepository
    private lateinit var viewModel: AddItemViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = InMemoryWardrobeRepository(emptyList())
        viewModel = AddItemViewModel(
            repository = repository,
            recognize = { suggestion },
            defaultName = { garment, color -> "$color $garment" },
            newId = { "new-id" },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun form() = (viewModel.step.value as AddItemStep.Details).form

    @Test
    fun startsAtSourceChoice() {
        assertEquals(AddItemStep.ChooseSource, viewModel.step.value)
    }

    @Test
    fun recognitionPrefillsTheSuggestionAndADefaultName() {
        viewModel.recognizePhoto()
        val form = form()
        assertEquals(Garment.SWEATER, form.garment)
        assertEquals(ItemColor.GREEN, form.color)
        assertEquals(suggestion, form.suggestion)
        assertEquals("GREEN SWEATER", form.name)
        assertTrue(form.seasons.isEmpty())
    }

    @Test
    fun changingTheColorRenamesWhileTheNameIsUntouched() {
        viewModel.recognizePhoto()
        viewModel.update(ItemFormChange.SetColor(ItemColor.NAVY))
        assertEquals("NAVY SWEATER", form().name)
    }

    @Test
    fun aTypedNameIsKept() {
        viewModel.recognizePhoto()
        viewModel.update(ItemFormChange.SetName("My jumper"))
        viewModel.update(ItemFormChange.SetGarment(Garment.COAT))
        assertEquals("My jumper", form().name)
    }

    @Test
    fun theMaterialFollowsTheGarmentUntilTheUserPicksOne() {
        viewModel.recognizePhoto()
        assertEquals(Material.WOOL, form().material)
        viewModel.update(ItemFormChange.SetGarment(Garment.JEANS))
        assertEquals(Material.DENIM, form().material)

        viewModel.update(ItemFormChange.SetMaterial(Material.LINEN))
        viewModel.update(ItemFormChange.SetGarment(Garment.SHIRT))
        assertEquals(Material.LINEN, form().material)
    }

    @Test
    fun saveRejectsABlankNameAndShowsErrors() = runTest {
        viewModel.recognizePhoto()
        viewModel.update(ItemFormChange.SetName("   "))
        viewModel.update(ItemFormChange.ToggleSeason(Season.WINTER))
        assertNull(viewModel.save())
        assertTrue(form().showErrors)
        assertTrue(repository.items.first().isEmpty())
    }

    @Test
    fun saveRejectsAnItemWithoutSeasons() = runTest {
        viewModel.recognizePhoto()
        assertNull(viewModel.save())
        assertTrue(form().seasonsError)
    }

    @Test
    fun saveAddsTheItemWithATrimmedName() = runTest {
        viewModel.recognizePhoto()
        viewModel.update(ItemFormChange.SetName("  Green jumper "))
        viewModel.update(ItemFormChange.SetWarmth(Warmth.WARM))
        viewModel.update(ItemFormChange.SetMaterial(Material.SYNTHETIC))
        viewModel.update(ItemFormChange.ToggleSeason(Season.AUTUMN))
        viewModel.update(ItemFormChange.ToggleSeason(Season.WINTER))

        val saved = viewModel.save()

        assertEquals("new-id", saved?.id)
        assertEquals("Green jumper", saved?.name)
        assertEquals(setOf(Season.AUTUMN, Season.WINTER), saved?.seasons)
        assertEquals(Warmth.WARM, saved?.warmth)
        assertEquals(Material.SYNTHETIC, saved?.material)
        assertEquals(listOf(saved), repository.items.first())
    }
}
