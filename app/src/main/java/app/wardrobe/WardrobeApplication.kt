package app.wardrobe

import android.app.Application
import app.wardrobe.data.MlKitRecognizer
import app.wardrobe.data.OpenMeteoWeather
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.RoomWardrobeRepository
import app.wardrobe.data.WardrobeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WardrobeApplication : Application() {
    private val recognizer by lazy { MlKitRecognizer(this) }

    /** For work that outlives any screen. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val container: AppContainer by lazy {
        AppContainer(
            wardrobeRepository = RoomWardrobeRepository(WardrobeDatabase.build(this).items()),
            recognizer = recognizer,
            photoStore = PhotoStore(filesDir),
            weather = OpenMeteoWeather(getSharedPreferences("weather", MODE_PRIVATE), scope),
        )
    }

    override fun onCreate() {
        super.onCreate()
        recognizer.requestModelDownload()
        val startedAt = System.currentTimeMillis()
        // Photos of deleted items and discarded adds. A delete can still be undone while the app runs, so
        // the files wait for the next start.
        scope.launch {
            val used = container.wardrobeRepository.items.first().mapNotNull { it.photo }.toSet()
            container.photoStore.deleteUnused(used, olderThan = startedAt)
        }
        scope.launch { container.weather.refresh() }
    }
}
