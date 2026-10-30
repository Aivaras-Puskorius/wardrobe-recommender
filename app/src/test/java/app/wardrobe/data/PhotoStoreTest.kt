package app.wardrobe.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val store by lazy { PhotoStore(folder.root) }

    @Test
    fun newFilesAreUniqueAndInThePhotoFolder() {
        val first = store.newFile()
        val second = store.newFile()
        assertNotEquals(first.name, second.name)
        assertEquals(store.file(first.name), first)
        assertTrue(first.parentFile!!.isDirectory)
    }

    @Test
    fun copySavesTheStreamAndReturnsItsName() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val name = store.copy(bytes.inputStream())
        assertArrayEquals(bytes, store.file(name).readBytes())
    }

    @Test
    fun deleteUnusedKeepsPhotosInUseAndPhotosFromThisRun() {
        val used = store.newFile().apply { writeText("used"); setLastModified(1_000) }
        val abandoned = store.newFile().apply { writeText("abandoned"); setLastModified(1_000) }
        val fresh = store.newFile().apply { writeText("fresh"); setLastModified(3_000) }

        store.deleteUnused(used = setOf(used.name), olderThan = 2_000)

        assertTrue(used.exists())
        assertTrue(!abandoned.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun aCutOutStaysWithItsPhoto() {
        val used = store.newFile().apply { writeText("used"); setLastModified(1_000) }
        val unused = store.newFile().apply { writeText("unused"); setLastModified(1_000) }
        val usedCutout = cutoutOf(used).apply { writeText("cut"); setLastModified(1_000) }
        val unusedCutout = cutoutOf(unused).apply { writeText("cut"); setLastModified(1_000) }

        store.deleteUnused(used = setOf(used.name), olderThan = 2_000)

        assertTrue(usedCutout.exists())
        assertTrue(!unusedCutout.exists())
        assertEquals(used.parentFile, usedCutout.parentFile)
        assertNotEquals(used.name, usedCutout.name)
    }

    @Test
    fun deleteUnusedWithoutAPhotoFolderDoesNothing() {
        store.deleteUnused(used = emptySet(), olderThan = Long.MAX_VALUE)
    }
}
