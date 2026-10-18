package app.wardrobe.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import app.wardrobe.domain.model.Warmth
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AddItemStep {
    /** Choose between the camera and the gallery. */
    data object ChooseSource : AddItemStep

    /** Recognition is running on the photo. */
    data object Analyzing : AddItemStep

    /** The user confirms or corrects the suggestion and fills in the rest. */
    data class Details(val form: ItemForm) : AddItemStep
}

/**
 * @param recognize returns what the photo shows. Milestone 1 uses a stand-in with sample answers.
 * @param defaultName builds a name such as "Green sweater" until the user types one.
 */
class AddItemViewModel(
    private val repository: WardrobeRepository,
    private val recognize: suspend () -> ItemSuggestion,
    private val defaultName: (Garment, ItemColor) -> String,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val _step = MutableStateFlow<AddItemStep>(AddItemStep.ChooseSource)
    val step: StateFlow<AddItemStep> = _step.asStateFlow()

    /** Recognizes the item in [photo], a file name in the photo folder, or null when there is no photo. */
    fun recognizePhoto(photo: String?) {
        _step.value = AddItemStep.Analyzing
        viewModelScope.launch {
            val suggestion = recognize()
            _step.value = AddItemStep.Details(
                ItemForm(
                    name = defaultName(suggestion.garment, suggestion.color),
                    nameEditedByUser = false,
                    garment = suggestion.garment,
                    color = suggestion.color,
                    material = suggestion.garment.defaultMaterial,
                    materialEditedByUser = false,
                    fit = Fit.REGULAR,
                    warmth = Warmth.MEDIUM,
                    seasons = emptySet(),
                    photo = photo,
                    suggestion = suggestion,
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
