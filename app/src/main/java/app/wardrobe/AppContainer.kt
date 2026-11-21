package app.wardrobe

import app.wardrobe.data.InMemoryRatingRepository
import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.RatingRepository
import app.wardrobe.data.Recognizer
import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleRecognizer
import app.wardrobe.data.SampleWeather
import app.wardrobe.data.ThemeSetting
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.data.Weather
import java.io.File

/**
 * Creates the app's shared objects. Screens get their dependencies from here.
 * The defaults suit tests. WardrobeApplication passes Room, the app's own files folder, ML Kit, Open-Meteo and the
 * saved settings.
 */
class AppContainer(
    val wardrobeRepository: WardrobeRepository = InMemoryWardrobeRepository(SampleData.items),
    val recognizer: Recognizer = SampleRecognizer(SampleData.photoSuggestions),
    val photoStore: PhotoStore = PhotoStore(File(System.getProperty("java.io.tmpdir"), "wardrobe-test-files")),
    val weather: Weather = SampleWeather(),
    val ratingRepository: RatingRepository = InMemoryRatingRepository(),
    /** The theme picked in Settings. */
    val theme: ThemeSetting = ThemeSetting(),
)
