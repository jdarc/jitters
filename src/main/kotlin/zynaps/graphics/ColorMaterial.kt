package zynaps.graphics

class ColorMaterial(private val color: Int) : Material {
    override fun sample(u: Float, v: Float) = color
}
