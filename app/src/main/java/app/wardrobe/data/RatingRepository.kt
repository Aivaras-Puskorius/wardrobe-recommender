package app.wardrobe.data

import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** The outfits the user rated, each known by its item ids. The app stores them in Room, tests keep them in memory. */
interface RatingRepository {
    /** Every rated outfit, the newest rating first. */
    val ratings: Flow<List<OutfitRating>>

    /** Rates the outfit made of [itemIds], replacing an earlier rating of it. */
    suspend fun rate(itemIds: Set<String>, rating: Rating)

    /** Removes the rating of the outfit made of [itemIds], if it has one. */
    suspend fun clear(itemIds: Set<String>)
}

/** Rates the outfit made of [itemIds], or clears its rating if it already has this one, as a toggle button does. */
suspend fun RatingRepository.toggle(itemIds: Set<String>, rating: Rating) {
    val current = ratings.first().firstOrNull { it.itemIds == itemIds }?.rating
    if (current == rating) clear(itemIds) else rate(itemIds, rating)
}
