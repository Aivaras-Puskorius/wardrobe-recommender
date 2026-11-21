package app.wardrobe.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleItemsTest {

    private val own = SampleData.items.first().copy(id = "my-coat", name = "My coat")

    @Test
    fun removingSampleItemsKeepsTheUsersOwn() = runTest {
        val repository = InMemoryWardrobeRepository(SampleData.items + own)
        repository.removeSampleItems()
        assertEquals(listOf("my-coat"), repository.items.first().map { it.id })
    }

    /** A sample item the user edited is still a sample item: its id says so. */
    @Test
    fun anEditedSampleItemIsStillASampleItem() {
        assertTrue(SampleData.isSample(SampleData.items.first().copy(name = "My favourite tee")))
        assertFalse(SampleData.isSample(own))
    }

    @Test
    fun removingWhenNoneAreLeftChangesNothing() = runTest {
        val repository = InMemoryWardrobeRepository(listOf(own))
        repository.removeSampleItems()
        assertEquals(listOf("my-coat"), repository.items.first().map { it.id })
    }
}
