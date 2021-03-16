package zynaps.graphics

interface Geometry {
    val bounds: Aabb
    fun render(device: Device)
}
