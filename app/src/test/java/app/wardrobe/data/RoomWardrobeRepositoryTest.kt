package app.wardrobe.data

import androidx.room.Room
import app.wardrobe.domain.model.ClothingItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The repository contract against a real SQLite database, kept in memory. */
@RunWith(RobolectricTestRunner::class)
class RoomWardrobeRepositoryTest : WardrobeRepositoryContract() {

    private val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), WardrobeDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After
    fun closeDatabase() {
        database.close()
    }

    override fun createRepository(initialItems: List<ClothingItem>): WardrobeRepository {
        val repository = RoomWardrobeRepository(database.items())
        runBlocking { initialItems.forEach { repository.add(it) } }
        return repository
    }
}
