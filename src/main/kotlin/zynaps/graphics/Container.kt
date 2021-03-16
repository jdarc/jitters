package zynaps.graphics

interface Container {
    fun contains(box: Aabb): Containment
}
