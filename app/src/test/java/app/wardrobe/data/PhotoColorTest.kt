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

    @Test
    fun anEmptyPhotoHasNoColor() {
        assertNull(dominantColor(IntArray(0), 0, 0))
    }
}
