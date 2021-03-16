package zynaps.graphics

import zynaps.math.Scalar.floor
import zynaps.math.Scalar.min
import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage

object ToolKit {

    fun grabPixels(img: BufferedImage): IntArray = img.getRGB(0, 0, img.width, img.height, IntArray(img.width * img.height), 0, img.width)

    fun buildCheckeredImage(size: Int, count: Int, color1: Color, color2: Color): BufferedImage {
        val bitmap = BufferedImage(size, size, Transparency.OPAQUE)
        if (size <= 0 || count <= 0) return bitmap
        var toggle = 0
        val brushes = arrayOf(color1, color2)
        val delta = floor(min(size / count.toFloat(), (size shr 1).toFloat()))
        val g = bitmap.graphics
        for (y in 0 until size step delta) {
            for (x in 0 until size step delta) {
                g.color = brushes[toggle]
                g.fillRect(x, y, x + delta, y + delta)
                toggle = 1 - toggle
            }
            toggle = 1 - toggle
        }
        g.dispose()
        return bitmap
    }
}
