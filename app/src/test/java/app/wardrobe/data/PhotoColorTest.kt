package app.wardrobe.data

import app.wardrobe.domain.model.ItemColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Synthetic 40 by 40 photos: a plain background with a narrow item in the middle. */
class PhotoColorTest {

    private val size = 40

    private fun photo(background: Int, item: Int, speckle: Int? = null): IntArray =
        IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            // Narrower than the middle of the photo, so background shows on both sides of it there.
            val inItem = x in 16 until 24 && y in 8 until 34
            when {
                !inItem -> background
                speckle != null && (x + y) % 7 == 0 -> speckle
                else -> item
            }
        }

    private fun colorOf(pixels: IntArray) = dominantColor(pixels, size, size)

    @Test
    fun aNavyItemOnWhite() {
        assertEquals(ItemColor.NAVY, colorOf(photo(background = 0xFFFAFAFA.toInt(), item = 0xFF1F2A44.toInt())))
    }

    @Test
    fun aRedItemOnBeige() {
        assertEquals(ItemColor.RED, colorOf(photo(background = 0xFFE8DCC8.toInt(), item = 0xFFC0392B.toInt())))
    }

    @Test
    fun aLightGreyItemReadsAsGreyNotLightBlue() {
        assertEquals(ItemColor.GREY, colorOf(photo(background = 0xFFFAFAFA.toInt(), item = 0xFFBDBDBD.toInt())))
    }

    @Test
    fun aWhiteItemOnWhiteStillGetsAColor() {
        assertEquals(ItemColor.WHITE, colorOf(photo(background = 0xFFFAFAFA.toInt(), item = 0xFFF6F6F6.toInt())))
    }

    @Test
    fun aPatternInTheMinorityDoesNotWin() {
        val navyWithRedDots = photo(background = 0xFFFAFAFA.toInt(), item = 0xFF1F2A44.toInt(), speckle = 0xFFC0392B.toInt())
        assertEquals(ItemColor.NAVY, colorOf(navyWithRedDots))
    }

    @Test
    fun anItemThatFillsThePhoto() {
        val olive = 0xFF556B2F.toInt()
        assertEquals(ItemColor.OLIVE, colorOf(photo(background = olive, item = olive)))
    }

    /** A cut-out: the item's pixels are opaque, the rest is transparent and may hold any color. */
    private fun cutout(item: Int, hidden: Int = 0x00FAFAFA, edge: Int? = null): IntArray =
        photo(background = hidden, item = item, speckle = edge)

    @Test
    fun aCutOutsColorComesFromItsOpaquePixels() {
        assertEquals(ItemColor.NAVY, cutoutColor(cutout(item = 0xFF1F2A44.toInt())))
        // Transparent pixels don't count, whatever color they hold.
        assertEquals(ItemColor.RED, cutoutColor(cutout(item = 0xFFC0392B.toInt(), hidden = 0x001F2A44)))
    }

    @Test
    fun halfTransparentEdgesDoNotCount() {
        val olive = 0xFF556B2F.toInt()
        assertEquals(ItemColor.OLIVE, cutoutColor(cutout(item = olive, edge = 0x40FAFAFA)))
    }

    @Test
    fun aCutOutWithNothingInItHasNoColor() {
        assertNull(cutoutColor(IntArray(size * size)))
    }

    @Test
    fun onWhitePutsTheItemOnAWhiteBackground() {
        val pixels = intArrayOf(0x00000000, 0xFF1F2A44.toInt(), 0x80000000.toInt())
        val white = onWhite(pixels)
        assertEquals(0xFFFFFFFF.toInt(), white[0])
        assertEquals(0xFF1F2A44.toInt(), white[1])
        // Half-transparent black on white is mid grey.
        assertEquals(0xFF7F7F7F.toInt(), white[2])
    }

    @Test
    fun anEmptyPhotoHasNoColor() {
        assertNull(dominantColor(IntArray(0), 0, 0))
    }
}
