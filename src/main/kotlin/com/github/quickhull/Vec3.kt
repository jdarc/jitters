package com.github.quickhull

import kotlin.math.pow
import kotlin.math.sqrt

internal class Vec3 {
    var x = 0F
    var y = 0F
    var z = 0F

    operator fun get(i: Int) = floatArrayOf(x, y, z)[i]

    fun lengthSquared() = dot(this)

    fun length() = sqrt(lengthSquared())

    fun set(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun set(v1: Vec3) = set(v1.x, v1.y, v1.z)

    fun add(x: Float, y: Float, z: Float) = set(this.x + x, this.y + y, this.z + z)

    fun sub(v1: Vec3, v2: Vec3) = set(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z)

    fun scale(s: Float) = set(x * s, y * s, z * s)

    fun distanceSquared(v: Vec3) = (x - v.x).pow(2) + (y - v.y).pow(2) + (z - v.z).pow(2)

    fun normalize() = scale(1 / sqrt(lengthSquared()))

    fun dot(v1: Vec3): Float = x * v1.x + y * v1.y + z * v1.z

    fun cross(v1: Vec3, v2: Vec3) = set(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x)

    override fun toString() = "Vector3{x=$x, y=$y, z=$z}"
}
