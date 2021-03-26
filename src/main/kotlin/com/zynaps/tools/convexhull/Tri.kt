package com.zynaps.tools.convexhull

internal class Tri(a: Int, b: Int, c: Int, var id: Int = 0) : Int3(a, b, c) {
    var n = Int3(-1, -1, -1)
    var vmax = -1
    var rise = 0.0

    fun with(a: Int, b: Int, c: Int): Tri {
        n.set(a, b, c)
        return this
    }

    fun neib(a: Int, b: Int): IntRef {
        for (i in 0..2) {
            if (this[i] == a && this[(i + 1) % 3] == b) return n.getRef((i + 2) % 3)
            if (this[i] == b && this[(i + 1) % 3] == a) return n.getRef((i + 2) % 3)
        }
        return ER_REF
    }

    companion object {
        private val ER_REF = object : IntRef {
            private var er = -1
            override fun get() = er
            override fun set(value: Int) {
                er = value
            }
        }
    }
}
