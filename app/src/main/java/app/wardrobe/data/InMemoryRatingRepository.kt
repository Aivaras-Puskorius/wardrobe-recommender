package app.wardrobe.data

import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Keeps ratings in memory. They are lost when the app process ends. */
class InMemoryRatingRepository(private val now: () -> Instant = Instant::now) : RatingRepository {

    private val state = MutableStateFlow(emptyList<OutfitRating>())

    override val ratings: StateFlow<List<OutfitRating>> = state.asStateFlow()

    override suspend fun rate(itemIds: Set<String>, rating: Rating) {
        state.update { ratings -> listOf(OutfitRating(itemIds, rating, now())) + ratings.filterNot { it.itemIds == itemIds } }
    }

    override suspend fun clear(itemIds: Set<String>) {
        state.update { ratings -> ratings.filterNot { it.itemIds == itemIds } }
    }
}
