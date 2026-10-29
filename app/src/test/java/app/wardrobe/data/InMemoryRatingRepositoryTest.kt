package app.wardrobe.data

import java.time.Instant

class InMemoryRatingRepositoryTest : RatingRepositoryContract() {
    override fun createRepository(now: () -> Instant) = InMemoryRatingRepository(now)
}
