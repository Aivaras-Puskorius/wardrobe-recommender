package app.wardrobe

import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.Recognizer
import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleRecognizer
import app.wardrobe.data.SampleWeather
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.data.Weather
import app.wardrobe.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Creates the app's shared objects. Screens get their dependencies from here.
 * The defaults suit tests. WardrobeApplication passes Room, the app's own files folder, ML Kit and Open-Meteo.
 */
class AppContainer(
    val wardrobeRepository: WardrobeRepository = InMemoryWardrobeRepository(SampleData.items),
    val recognizer: Recognizer = SampleRecognizer(SampleData.photoSuggestions),
    val photoStore: PhotoStore = PhotoStore(File(System.getProperty("java.io.tmpdir"), "wardrobe-test-files")),
    val weather: Weather = SampleWeather(),
) {
    /**
     * The theme picked in Settings.
     * INFO(limit): kept in memory, so it resets to SYSTEM when the app restarts. Store it with DataStore when that matters.
     */
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
}
