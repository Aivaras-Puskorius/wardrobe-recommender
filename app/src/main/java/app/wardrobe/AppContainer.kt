package app.wardrobe

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleRecognizer
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.domain.model.Forecast
import app.wardrobe.ui.theme.ThemeMode
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Creates the app's shared objects. Screens get their dependencies from here.
 * The defaults suit tests. WardrobeApplication passes Room and the app's own files folder.
 */
class AppContainer(
    val wardrobeRepository: WardrobeRepository = InMemoryWardrobeRepository(SampleData.items),
    val recognizer: SampleRecognizer = SampleRecognizer(SampleData.photoSuggestions),
    val photoStore: PhotoStore = PhotoStore(File(System.getProperty("java.io.tmpdir"), "wardrobe-test-files")),
) {
    /**
     * The theme picked in Settings.
     * INFO(limit): kept in memory, so it resets to SYSTEM when the app restarts. Store it with DataStore when that matters.
     */
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    /** The week ahead, starting today. Milestone 1 uses the sample Kaunas week. */
    fun forecast(): Forecast = SampleData.forecast(LocalDate.now())
}
