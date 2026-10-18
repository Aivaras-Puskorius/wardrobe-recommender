package app.wardrobe.data

import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleRecognizerTest {

    private val green = ItemSuggestion(Garment.SWEATER, ItemColor.GREEN)
    private val red = ItemSuggestion(Garment.SNEAKERS, ItemColor.RED)

    @Test
    fun returnsSuggestionsInTurnAndStartsOver() = runTest {
        val recognizer = SampleRecognizer(listOf(green, red), delayMillis = 0)
        assertEquals(listOf(green, red, green), List(3) { recognizer.recognize(File("photo.jpg")) })
    }
}
