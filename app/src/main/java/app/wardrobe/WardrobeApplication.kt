package app.wardrobe

import android.app.Application
import app.wardrobe.data.RoomWardrobeRepository
import app.wardrobe.data.WardrobeDatabase

class WardrobeApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(wardrobeRepository = RoomWardrobeRepository(WardrobeDatabase.build(this).items()))
    }
}
