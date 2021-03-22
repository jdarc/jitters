package com.bulletphysics.convexhull

internal class Int4(var x: Int = 0, var y: Int = 0, var z: Int = 0, var w: Int = 0) {

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IllegalArgumentException()
    }

    fun set(x: Int, y: Int, z: Int, w: Int) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }
}
