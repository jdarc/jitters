package com.zynaps.graphics

import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

class Bitmap(val width: Int, val height: Int) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val pixels = (image.raster.dataBuffer as DataBufferInt).data as IntArray
}
