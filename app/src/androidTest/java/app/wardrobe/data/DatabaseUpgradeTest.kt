package app.wardrobe.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A database from the first release, upgraded in one go to the current version on the device's own SQLite.
 * WardrobeMigrationTest checks each step on the computer.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseUpgradeTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), WardrobeDatabase::class.java)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun version1ItemsSurviveEveryUpgradeAndRatingsCanBeSaved() {
        context.deleteDatabase(NAME)
        helper.createDatabase(NAME, 1).apply {
            execSQL(
                "INSERT INTO items (id, name, garment, color, warmth, seasons) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf("knit", "Grey knit", "SWEATER", "GREY", "WARM", "AUTUMN,WINTER"),
            )
            close()
        }
        helper.runMigrationsAndValidate(
            NAME,
            5,
            true,
            WardrobeDatabase.MIGRATION_1_2,
            WardrobeDatabase.MIGRATION_2_3,
            WardrobeDatabase.MIGRATION_3_4,
            WardrobeDatabase.MIGRATION_4_5,
        ).close()

        val database = WardrobeDatabase.build(context, NAME)
        val prefs = context.getSharedPreferences("upgrade-test", 0)
        val ratings = RoomRatingRepository(database.ratings(), prefs)
        val (item, saved) = runBlocking {
            ratings.rate(setOf("knit"), Rating.LIKED)
            RoomWardrobeRepository(database.items()).items.first().single() to ratings.ratings.first()
        }
        database.close()
        context.deleteDatabase(NAME)

        assertEquals("Grey knit", item.name)
        assertEquals(Material.WOOL, item.material)
        assertEquals(Fit.REGULAR, item.fit)
        assertEquals(null, item.photo)
        assertEquals(listOf(setOf("knit")), saved.map { it.itemIds })
    }

    private companion object {
        const val NAME = "upgrade-test.db"
    }
}
