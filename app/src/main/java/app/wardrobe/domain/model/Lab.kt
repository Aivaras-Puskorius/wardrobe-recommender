package app.wardrobe.domain.model

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/** A color in CIELAB under the D65 white point. [l] is lightness, 0 for black and 100 for white. */
data class Lab(val l: Double, val a: Double, val b: Double) {

    /** The CIE76 color difference: the straight-line distance in CIELAB. */
    fun distanceTo(other: Lab): Double = sqrt((l - other.l).pow(2) + (a - other.a).pow(2) + (b - other.b).pow(2))

    companion object {
        /** Converts an sRGB color given as ARGB. Alpha is ignored. */
        fun of(argb: Long): Lab {
            fun linear(shift: Int): Double {
                val c = ((argb shr shift) and 0xFF) / 255.0
                return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            }
            val r = linear(16)
            val g = linear(8)
            val b = linear(0)
            fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116
            val fx = f((0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047)
            val fy = f(0.2126 * r + 0.7152 * g + 0.0722 * b)
            val fz = f((0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883)
            return Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
        }
    }
}
