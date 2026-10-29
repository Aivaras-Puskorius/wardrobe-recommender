package app.wardrobe.data

import androidx.room.Room
import java.time.Instant
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The rating contract against a real SQLite database, kept in memory. */
@RunWith(RobolectricTestRunner::class)
class RoomRatingRepositoryTest : RatingRepositoryContract() {

    private val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), WardrobeDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After
    fun closeDatabase() {
        database.close()
    }

    override fun createRepository(now: () -> Instant): RatingRepository = RoomRatingRepository(database.ratings(), now)
}
