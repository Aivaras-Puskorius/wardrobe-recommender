package app.wardrobe.feature.item

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.PhotoStore
import app.wardrobe.domain.model.Fit
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val suggestion = ItemSuggestion(Garment.SWEATER, ItemColor.GREEN)
    private var nextSuggestion = suggestion
    private val recognizedFiles = mutableListOf<File>()
    private lateinit var photoStore: PhotoStore
    private lateinit var repository: InMemoryWardrobeRepository
    private lateinit var viewModel: AddItemViewModel

    @Before
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        photoStore = PhotoStore(folder.root)
        repository = InMemoryWardrobeRepository(emptyList())
        viewModel = AddItemViewModel(
            repository = repository,
            recognize = { file -> recognizedFiles += file; nextSuggestion },
            defaultName = { garment, color -> "$color $garment" },
            photoStore = photoStore,
            newId = { "new-id" },
            ioDispatcher = dispatcher,
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
    fun aPhotoFromTheCameraIsRecognized() {
        val file = viewModel.startCamera()
        file.writeBytes(byteArrayOf(1, 2, 3))
        viewModel.cameraFinished(saved = true)
        assertEquals(file.name, form().photo)
        assertEquals(photoStore.file(file.name), file)
    }

    @Test
    fun aCancelledCameraDeletesTheFileAndStaysOnSourceChoice() {
        val file = viewModel.startCamera()
        viewModel.cameraFinished(saved = false)
        assertEquals(AddItemStep.ChooseSource, viewModel.step.value)
        assertTrue(!file.exists())
    }

    @Test
    fun aPickedPhotoIsCopiedAndRecognized() {
        viewModel.usePickedPhoto { byteArrayOf(4, 5, 6).inputStream() }
        val photo = form().photo!!
        assertEquals(listOf<Byte>(4, 5, 6), photoStore.file(photo).readBytes().toList())
    }

    @Test
    fun aPhotoThatCannotBeReadShowsAnErrorAndStaysOnSourceChoice() {
        viewModel.usePickedPhoto { throw IOException("gone") }
        assertEquals(AddItemStep.ChooseSource, viewModel.step.value)
        assertTrue(viewModel.photoError.value)
        viewModel.photoErrorShown()
        assertTrue(!viewModel.photoError.value)
    }

    @Test
    fun recognitionPrefillsTheSuggestionAndADefaultName() {
        viewModel.recognizePhoto(PHOTO)
        val form = form()
        assertEquals(Garment.SWEATER, form.garment)
        assertEquals(ItemColor.GREEN, form.color)
        assertEquals(suggestion, form.suggestion)
        assertEquals("GREEN SWEATER", form.name)
        assertTrue(form.seasons.isEmpty())
    }

    @Test
    fun changingTheColorRenamesWhileTheNameIsUntouched() {
        viewModel.recognizePhoto(PHOTO)
        viewModel.update(ItemFormChange.SetColor(ItemColor.NAVY))
        assertEquals("NAVY SWEATER", form().name)
    }

    @Test
    fun aTypedNameIsKept() {
        viewModel.recognizePhoto(PHOTO)
        viewModel.update(ItemFormChange.SetName("My jumper"))
        viewModel.update(ItemFormChange.SetGarment(Garment.COAT))
        assertEquals("My jumper", form().name)
    }

    @Test
    fun theMaterialFollowsTheGarmentUntilTheUserPicksOne() {
        viewModel.recognizePhoto(PHOTO)
        assertEquals(Material.WOOL, form().material)
        viewModel.update(ItemFormChange.SetGarment(Garment.JEANS))
        assertEquals(Material.DENIM, form().material)

        viewModel.update(ItemFormChange.SetMaterial(Material.LINEN))
        viewModel.update(ItemFormChange.SetGarment(Garment.SHIRT))
        assertEquals(Material.LINEN, form().material)
    }

    @Test
    fun recognitionLooksAtThePhotoFile() {
        viewModel.recognizePhoto(PHOTO)
        assertEquals(listOf(photoStore.file(PHOTO)), recognizedFiles)
    }

    @Test
    fun aColorOnlySuggestionLeavesTheTypeToTheUser() {
        nextSuggestion = ItemSuggestion(garment = null, color = ItemColor.NAVY)
        viewModel.recognizePhoto(PHOTO)
        assertEquals(Garment.T_SHIRT, form().garment)
        assertEquals(ItemColor.NAVY, form().color)
        assertEquals("NAVY T_SHIRT", form().name)
        assertEquals(nextSuggestion, form().suggestion)
    }

    @Test
    fun anEmptySuggestionShowsNoHint() {
        nextSuggestion = ItemSuggestion(garment = null, color = null)
        viewModel.recognizePhoto(PHOTO)
        assertNull(form().suggestion)
    }

    @Test
    fun aSuggestedMaterialReplacesTheGarmentDefault() {
        nextSuggestion = ItemSuggestion(Garment.JACKET, ItemColor.BLUE, Material.DENIM)
        viewModel.recognizePhoto(PHOTO)
        assertEquals(Material.DENIM, form().material)
        assertTrue(!form().materialEditedByUser)
    }

    @Test
    fun theFormKeepsThePhoto() {
        viewModel.recognizePhoto(PHOTO)
        assertEquals(PHOTO, form().photo)
    }

    @Test
    fun aNewItemStartsWithRegularFit() {
        viewModel.recognizePhoto(PHOTO)
        assertEquals(Fit.REGULAR, form().fit)
    }

    @Test
    fun footwearIsSavedAsRegularFit() = runTest {
        viewModel.recognizePhoto(PHOTO)
        viewModel.update(ItemFormChange.SetFit(Fit.OVERSIZED))
        viewModel.update(ItemFormChange.SetGarment(Garment.BOOTS))
        viewModel.update(ItemFormChange.ToggleSeason(Season.WINTER))
        assertEquals(Fit.REGULAR, viewModel.save()?.fit)
    }

    @Test
    fun saveRejectsABlankNameAndShowsErrors() = runTest {
        viewModel.recognizePhoto(PHOTO)
        viewModel.update(ItemFormChange.SetName("   "))
        viewModel.update(ItemFormChange.ToggleSeason(Season.WINTER))
        assertNull(viewModel.save())
        assertTrue(form().showErrors)
        assertTrue(repository.items.first().isEmpty())
    }

    @Test
    fun saveRejectsAnItemWithoutSeasons() = runTest {
        viewModel.recognizePhoto(PHOTO)
        assertNull(viewModel.save())
        assertTrue(form().seasonsError)
    }

    @Test
    fun saveAddsTheItemWithATrimmedName() = runTest {
        viewModel.recognizePhoto(PHOTO)
        viewModel.update(ItemFormChange.SetName("  Green jumper "))
        viewModel.update(ItemFormChange.SetWarmth(Warmth.WARM))
        viewModel.update(ItemFormChange.SetMaterial(Material.SYNTHETIC))
        viewModel.update(ItemFormChange.SetFit(Fit.OVERSIZED))
        viewModel.update(ItemFormChange.ToggleSeason(Season.AUTUMN))
        viewModel.update(ItemFormChange.ToggleSeason(Season.WINTER))

        val saved = viewModel.save()

        assertEquals("new-id", saved?.id)
        assertEquals("Green jumper", saved?.name)
        assertEquals(setOf(Season.AUTUMN, Season.WINTER), saved?.seasons)
        assertEquals(Warmth.WARM, saved?.warmth)
        assertEquals(Material.SYNTHETIC, saved?.material)
        assertEquals(Fit.OVERSIZED, saved?.fit)
        assertEquals(PHOTO, saved?.photo)
        assertEquals(listOf(saved), repository.items.first())
    }

    private companion object {
        const val PHOTO = "green-sweater.jpg"
    }
}
