package app.wardrobe.data

import app.wardrobe.domain.model.ItemSuggestion
import java.io.File
import kotlinx.coroutines.delay

/**
 * Stands in for image recognition in milestone 1, before the camera and the model exist.
 * Returns [suggestions] in turn after [delayMillis], so the add item flow can be demonstrated.
 */
class SampleRecognizer(
    private val suggestions: List<ItemSuggestion>,
    private val delayMillis: Long = 1_200,
) {
    private var next = 0

    suspend fun recognize(photo: File): ItemSuggestion {
        delay(delayMillis)
        return suggestions[next++ % suggestions.size]
    }
}
