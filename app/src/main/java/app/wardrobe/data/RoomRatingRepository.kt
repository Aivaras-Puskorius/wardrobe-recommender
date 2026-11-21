package app.wardrobe.data

import android.content.SharedPreferences
import androidx.core.content.edit
import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Keeps ratings in the phone's [WardrobeDatabase] and the Start over time in [prefs], so both survive restarts. */
class RoomRatingRepository(
    private val dao: RatingDao,
    private val prefs: SharedPreferences,
    private val now: () -> Instant = Instant::now,
) : RatingRepository {

    override val ratings: Flow<List<OutfitRating>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun rate(itemIds: Set<String>, rating: Rating) {
        dao.upsert(RatingEntity(outfitKey(itemIds), rating, now().toEpochMilli()))
    }

    override suspend fun clear(itemIds: Set<String>) {
        dao.delete(outfitKey(itemIds))
    }

    private val since = MutableStateFlow(
        if (prefs.contains(KEY_SINCE)) Instant.ofEpochMilli(prefs.getLong(KEY_SINCE, 0)) else null,
    )

    override val tasteSince: StateFlow<Instant?> = since.asStateFlow()

    override suspend fun startTasteOver() {
        val startedOver = now()
        prefs.edit { putLong(KEY_SINCE, startedOver.toEpochMilli()) }
        since.value = startedOver
    }

    private companion object {
        const val KEY_SINCE = "since"
    }
}
