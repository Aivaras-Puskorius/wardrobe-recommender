package app.wardrobe.domain.model

import java.time.Instant

/** What the user thought of a suggested outfit. */
enum class Rating { LIKED, DISLIKED }

/** The [rating] the user gave the outfit made of [itemIds], at [ratedAt]. */
data class OutfitRating(val itemIds: Set<String>, val rating: Rating, val ratedAt: Instant)
