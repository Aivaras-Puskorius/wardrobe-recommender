package app.wardrobe.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AddItemStep {
    /** Choose between the camera and the gallery. */
    data object ChooseSource : AddItemStep

    /** Recognition is running on the photo. */
    data object Analyzing : AddItemStep

    /** The user confirms or corrects the suggestion and fills in the rest. */
    data class Details(val form: ItemForm) : AddItemStep
}

/**
 * @param recognize returns what the photo file shows.
 * @param defaultName builds a name such as "Green sweater" until the user types one.
 */
class AddItemViewModel(
    private val repository: WardrobeRepository,
    private val recognize: suspend (File) -> ItemSuggestion,
    private val defaultName: (Garment, ItemColor) -> String,
    private val photoStore: PhotoStore,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _step = MutableStateFlow<AddItemStep>(AddItemStep.ChooseSource)
    val step: StateFlow<AddItemStep> = _step.asStateFlow()

    private val _photoError = MutableStateFlow(false)

    /** True after a picked photo could not be read, until the screen has told the user. */
    val photoError: StateFlow<Boolean> = _photoError.asStateFlow()

    private var cameraFile: File? = null

    /** A new empty file for the camera app to write the photo into. */
    fun startCamera(): File = photoStore.newFile().also { cameraFile = it }

    /** Recognizes the camera photo, or deletes the empty file when the user backed out of the camera. */
    fun cameraFinished(saved: Boolean) {
        val file = cameraFile ?: return
        cameraFile = null
        if (saved) recognizePhoto(file.name) else file.delete()
    }

    /** Copies the photo that [open] reads, such as one from the gallery, and recognizes it. */
    fun usePickedPhoto(open: () -> InputStream) {
        viewModelScope.launch {
            val photo = try {
                withContext(ioDispatcher) { photoStore.copy(open()) }
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
            if (photo != null) recognizePhoto(photo) else _photoError.value = true
        }
    }

    fun photoErrorShown() {
        _photoError.value = false
    }

    /** Recognizes the item in [photo], a file name in the photo folder. */
    fun recognizePhoto(photo: String) {
        _step.value = AddItemStep.Analyzing
        viewModelScope.launch {
            val suggestion = recognize(photoStore.file(photo))
            // A type picked for the user looked like a guess, so an unknown type stays unpicked. An unknown color,
            // which only a photo that can't be read gives, starts at the first choice with no suggestion mark.
            val garment = suggestion.garment
            val color = suggestion.color ?: ItemColor.entries.first()
            _step.value = AddItemStep.Details(
                ItemForm(
                    name = garment?.let { defaultName(it, color) } ?: "",
                    nameEditedByUser = false,
                    garment = garment,
                    color = color,
                    material = suggestion.material ?: garment?.defaultMaterial,
                    materialEditedByUser = false,
                    fit = Fit.REGULAR,
                    warmth = garment?.defaultWarmth,
                    warmthEditedByUser = false,
                    seasons = emptySet(),
                    photo = photo,
                    suggestion = suggestion.takeIf { it.garment != null || it.color != null || it.material != null },
                ),
            )
        }
    }

    fun update(change: ItemFormChange) = editForm { it.apply(change, defaultName) }

    /** Adds the item and returns it, or shows the form's errors and returns null. */
    suspend fun save(): ClothingItem? {
        val form = (_step.value as? AddItemStep.Details)?.form ?: return null
        if (!form.isValid) {
            editForm { it.copy(showErrors = true) }
            return null
        }
        return form.toItem(newId()).also { repository.add(it) }
    }

    private fun editForm(change: (ItemForm) -> ItemForm) {
        _step.update { step -> if (step is AddItemStep.Details) step.copy(form = change(step.form)) else step }
    }
}
