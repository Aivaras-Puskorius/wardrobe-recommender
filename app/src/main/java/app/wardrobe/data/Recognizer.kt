package app.wardrobe.data

import app.wardrobe.domain.model.ItemSuggestion
import java.io.File

/** Looks at a photo of one garment and suggests what it is. */
fun interface Recognizer {
    suspend fun recognize(photo: File): ItemSuggestion
}
