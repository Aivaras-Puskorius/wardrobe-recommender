package app.wardrobe.data

import app.wardrobe.domain.model.ClothingItem

class InMemoryWardrobeRepositoryTest : WardrobeRepositoryContract() {
    override fun createRepository(initialItems: List<ClothingItem>) = InMemoryWardrobeRepository(initialItems)
}
