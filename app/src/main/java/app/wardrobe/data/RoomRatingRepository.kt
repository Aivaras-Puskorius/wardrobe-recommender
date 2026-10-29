package app.wardrobe.data

import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Keeps ratings in the phone's [WardrobeDatabase], so they survive restarts. */
class RoomRatingRepository(
    private val dao: RatingDao,
    private val now: () -> Instant = Instant::now,
) : RatingRepository {

    override val ratings: Flow<List<OutfitRating>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun rate(itemIds: Set<String>, rating: Rating) {
        dao.upsert(RatingEntity(outfitKey(itemIds), rating, now().toEpochMilli()))
    }

    override suspend fun clear(itemIds: Set<String>) {
        dao.delete(outfitKey(itemIds))
    }
}
