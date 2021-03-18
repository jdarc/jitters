package com.zynaps.graphics

interface Material {
    fun sample(u: Float, v: Float): Int

    companion object {
        val DEFAULT = ColorMaterial(0xFFFFFF)
    }
}

class ColorMaterial(private val color: Int) : Material {
    override fun sample(u: Float, v: Float) = color
}

class CheckerMaterial(private val rgb1: Int, private val rgb2: Int, private val scale: Float = 16.0F) : Material {
    override fun sample(u: Float, v: Float): Int {
        val i = (u * scale).toInt() + (v * scale).toInt() and 1
        return i * rgb1 or (1 - i) * rgb2
    }
}
