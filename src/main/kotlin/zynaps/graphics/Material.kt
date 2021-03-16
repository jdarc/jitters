package zynaps.graphics

interface Material {
    fun sample(u: Float, v: Float): Int

    companion object {
        val DEFAULT = ColorMaterial(0xFFFFFF)
    }
}
