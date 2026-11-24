package app.wardrobe.data

import androidx.room.Room
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
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

    /** Room saves the garment by its name, so every type, the newest included, must read back as saved. */
    @Test
    fun everyGarmentIsSavedAndReadBack() = runTest {
        val items = Garment.entries.map { shirt.copy(id = it.name, garment = it) }
        items.forEach { repository.add(it) }
        assertEquals(items, items.map { repository.item(it.id).first() })
    }
}
