package com.bulletphysics.convexhull

internal class Tri(a: Int, b: Int, c: Int) : Int3(a, b, c) {
    var n = Int3(-1, -1, -1)
    var id = 0
    var vmax = -1
    var rise = 0f

    fun neib(a: Int, b: Int): IntRef {
        for (i in 0..2) {
            if (this[i] == a && this[(i + 1) % 3] == b) return n.getRef((i + 2) % 3)
            if (this[i] == b && this[(i + 1) % 3] == a) return n.getRef((i + 2) % 3)
        }
        return erRef
    }

    companion object {
        private val erRef = object : IntRef {
            private var er = -1
            override fun get() = er
            override fun set(value: Int) {
                er = value
            }
        }
    }
}
