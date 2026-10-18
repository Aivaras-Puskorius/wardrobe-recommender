package app.wardrobe.data

import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
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
        material = Material.COTTON,
        fit = Fit.REGULAR,
        warmth = Warmth.MEDIUM,
        seasons = setOf(Season.SPRING, Season.AUTUMN),
        photo = null,
    )
    protected val jeans = ClothingItem(
        id = "jeans",
        name = "Dark jeans",
        garment = Garment.JEANS,
        color = ItemColor.DENIM,
        material = Material.DENIM,
        fit = Fit.FITTED,
        warmth = Warmth.MEDIUM,
        seasons = Season.entries.toSet(),
        photo = "jeans.jpg",
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

    @Test
    fun updateChangesTheMaterial() = runTest {
        val linen = shirt.copy(material = Material.LINEN)
        repository.update(linen)
        assertEquals(linen, repository.item(shirt.id).first())
    }

    @Test
    fun updateChangesTheFit() = runTest {
        val loose = shirt.copy(fit = Fit.LOOSE)
        repository.update(loose)
        assertEquals(loose, repository.item(shirt.id).first())
    }

    @Test
    fun updateChangesThePhoto() = runTest {
        val photographed = shirt.copy(photo = "shirt.jpg")
        repository.update(photographed)
        assertEquals(photographed, repository.item(shirt.id).first())
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
