package app.wardrobe.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Seeding of a new database file, as on the app's first launch. */
@RunWith(RobolectricTestRunner::class)
class WardrobeDatabaseTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun open() = WardrobeDatabase.build(context, name = "seed-test.db")

    @Test
    fun aNewDatabaseHoldsTheSampleWardrobeInOrder() = runTest {
        val database = open()
        val items = RoomWardrobeRepository(database.items()).items.first()
        database.close()
        assertEquals(SampleData.items, items)
    }

    @Test
    fun theSampleItemsAreAddedOnlyOnce() = runTest {
        val first = open()
        RoomWardrobeRepository(first.items()).delete("black-tee")
        first.close()

        val reopened = open()
        val items = RoomWardrobeRepository(reopened.items()).items.first()
        reopened.close()
        assertEquals(SampleData.items.size - 1, items.size)
        assertTrue(items.none { it.id == "black-tee" })
    }
}
