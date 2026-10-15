package app.wardrobe.data

import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What every [WardrobeRepository] must do. Each implementation's test extends this class.
 * [repository] starts with one item, [shirt].
 */
abstract class WardrobeRepositoryContract {

    /** A new repository holding [initialItems], in that order. */
    protected abstract fun createRepository(initialItems: List<ClothingItem>): WardrobeRepository


    protected val shirt = ClothingItem(
        id = "shirt",
        name = "Oxford shirt",
        garment = Garment.SHIRT,
        color = ItemColor.LIGHT_BLUE,
        warmth = Warmth.MEDIUM,
        seasons = setOf(Season.SPRING, Season.AUTUMN),
    )
    protected val jeans = ClothingItem(
        id = "jeans",
        name = "Dark jeans",
        garment = Garment.JEANS,
        color = ItemColor.DENIM,
        warmth = Warmth.MEDIUM,
        seasons = Season.entries.toSet(),
    )
    protected val repository: WardrobeRepository by lazy { createRepository(listOf(shirt)) }

    @Test
    fun startsWithInitialItems() = runTest {
        assertEquals(listOf(shirt), repository.items.first())
    }

    @Test
    fun addAppendsItem() = runTest {
        repository.add(jeans)
        assertEquals(listOf(shirt, jeans), repository.items.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRejectsDuplicateId() = runTest {
        repository.add(shirt.copy(name = "Another shirt"))
    }

    @Test
    fun updateReplacesItemWithSameId() = runTest {
        val renamed = shirt.copy(name = "Blue oxford shirt")
        repository.update(renamed)
        assertEquals(listOf(renamed), repository.items.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateRejectsUnknownId() = runTest {
        repository.update(jeans)
    }

    @Test
    fun deleteRemovesItem() = runTest {
        repository.add(jeans)
        repository.delete(shirt.id)
        assertEquals(listOf(jeans), repository.items.first())
    }

    @Test
    fun itemFindsById() = runTest {
        assertEquals(shirt, repository.item(shirt.id).first())
    }

    @Test
    fun itemIsNullForUnknownId() = runTest {
        assertNull(repository.item("missing").first())
    }
}
