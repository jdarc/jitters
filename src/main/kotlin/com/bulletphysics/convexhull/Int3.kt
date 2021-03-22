package com.bulletphysics.convexhull

internal open class Int3(var x: Int = 0, var y: Int = 0, var z: Int = 0) {

    constructor(i: Int3) : this(i.x, i.y, i.z)

    fun set(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun set(i: Int3) = set(i.x, i.y, i.z)

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException()
    }

    operator fun set(index: Int, value: Int) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException()
        }
    }

    fun getRef(index: Int) = object : IntRef {
        override fun get() = this@Int3[index]
        override fun set(value: Int) {
            this@Int3[index] = value
        }
    }
}
