package app.wardrobe.data

import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Item photos in the app's private storage, under `photos` in [filesDir]. Items keep only the file name.
 *
 * A photo is written here as soon as it is taken or picked, before its item is saved, so a discarded
 * add leaves an unused file. [deleteUnused] clears those on the next app start.
 * INFO(limit): photos are kept at full camera size, a few megabytes each. Downscale on import if storage matters.
 */
class PhotoStore(filesDir: File) {
    private val dir = File(filesDir, "photos")

    /** An empty file with a new unique name, for the camera app to write into. */
    fun newFile(): File {
        dir.mkdirs()
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun file(name: String): File = File(dir, name)

    /** Saves a picked photo as a new file and returns its name. */
    fun copy(input: InputStream): String {
        val file = newFile()
        input.use { source -> file.outputStream().use { source.copyTo(it) } }
        return file.name
    }

    /**
     * Deletes photos that no item in [used] names, and their cut-outs, when they were last changed before [olderThan].
     * Photos from the current run are newer, so an add in progress keeps its photo.
     */
    fun deleteUnused(used: Set<String>, olderThan: Long) {
        val keep = used + used.map { cutoutOf(file(it)).name }
        dir.listFiles()
            ?.filter { it.name !in keep && it.lastModified() < olderThan }
            ?.forEach { it.delete() }
    }
}

/** Where the cut-out of [photo] goes: the item without its background, as a PNG next to the photo. */
fun cutoutOf(photo: File): File = File(photo.parentFile, "${photo.nameWithoutExtension}.cutout.png")
