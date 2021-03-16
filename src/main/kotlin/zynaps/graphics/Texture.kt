package zynaps.graphics

import java.awt.image.BufferedImage
import kotlin.math.log2

class Texture(image: BufferedImage) {
    private val size: Int
    private val mask: Int
    private val shift: Int
    private val pixels: IntArray

    init {
        if (image.width == image.height && image.height > 0 && (image.height and image.height - 1) == 0) {
            size = image.width
            mask = size * size - 1
            shift = log2(size.toFloat()).toInt()
            pixels = ToolKit.grabPixels(image)
        } else throw RuntimeException("image dimensions not supported")
    }

    fun sample(u: Float, v: Float) = pixels[mask and ((size * v).toInt() shl shift or (size * u).toInt())]
}
