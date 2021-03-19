package com.zynaps.math

data class Plane(val normal: Vector3, val distance: Float) {

    constructor(x: Float, y: Float, z: Float, d: Float) : this(Vector3(x, y, z), d)

    fun dot(x: Float, y: Float, z: Float) = normal.x * x + normal.y * y + normal.z * z + distance

    companion object {
        val ZERO = Plane(Vector3.ZERO, 0F)
    }
}
