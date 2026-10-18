package app.wardrobe.data

import app.wardrobe.domain.model.ItemSuggestion
import java.io.File
import kotlinx.coroutines.delay

/**
 * Stands in for image recognition in tests, where ML Kit can't run.
 * Returns [suggestions] in turn after [delayMillis], whatever the photo shows.
 */
class SampleRecognizer(
    private val suggestions: List<ItemSuggestion>,
    private val delayMillis: Long = 1_200,
) : Recognizer {
    private var next = 0

    override suspend fun recognize(photo: File): ItemSuggestion {
        delay(delayMillis)
        return suggestions[next++ % suggestions.size]
    }
}
