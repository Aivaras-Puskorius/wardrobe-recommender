package app.wardrobe.data

import android.content.Context
import androidx.room.Room
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The rating contract against a real SQLite database, kept in memory, and real SharedPreferences. */
@RunWith(RobolectricTestRunner::class)
class RoomRatingRepositoryTest : RatingRepositoryContract() {

    private val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), WardrobeDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("taste", Context.MODE_PRIVATE)

    @After
    fun closeDatabase() {
        database.close()
    }

    override fun createRepository(now: () -> Instant): RatingRepository =
        RoomRatingRepository(database.ratings(), prefs, now)

    @Test
    fun theStartOverTimeSurvivesARestart() = runTest {
        val startedOver = Instant.parse("2026-11-21T10:00:00Z")
        RoomRatingRepository(database.ratings(), prefs) { startedOver }.startTasteOver()
        assertEquals(startedOver, RoomRatingRepository(database.ratings(), prefs).tasteSince.first())
    }
}
