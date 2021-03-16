package zynaps.graphics

internal class Clipper {
    private val ping = Array(6) { Vertex() }
    private val pong = Array(6) { Vertex() }
    private var result = ping

    operator fun get(index: Int) = result[index]

    fun clip(a: Vertex, b: Vertex, c: Vertex): Int {
        val clipMask = computeMask(a, b, c)
        if (clipMask and 0x7 == 0x7 || clipMask and 0x38 == 0x38) return 0
        ping[0].copyFrom(a)
        ping[1].copyFrom(b)
        ping[2].copyFrom(c)
        ping[3].copyFrom(a)
        result = pong
        return when {
            clipMask and (0x07 or 0x38) != 0 -> clip(1F, pong, ping, clip(-1F, ping, pong, 4) + 1).apply { result = ping }
            clipMask and 0x07 != 0 -> clip(-1F, ping, pong, 4)
            else -> clip(1F, ping, pong, 4)
        }
    }

    private companion object {

        fun computeMask(a: Vertex, b: Vertex, c: Vertex) = if (a.z < -a.w) 1 else 0 or if (b.z < -b.w) 2 else 0 or
                if (c.z < -c.w) 4 else 0 or if (a.z > a.w) 8 else 0 or if (b.z > b.w) 16 else 0 or if (c.z > c.w) 32 else 0

        fun clip(side: Float, src: Array<Vertex>, dst: Array<Vertex>, count: Int): Int {
            var srcIdx = 0
            var dstIdx = 0
            val a1 = src[srcIdx++]
            var a2 = src[srcIdx++]
            var compA = a1.w - side * a1.z
            for (v in 1 until count) {
                val compB = a2.w - side * a2.z
                if (compA > 0) {
                    if (compB > 0) {
                        dst[dstIdx++].copyFrom(a2)
                    } else {
                        dst[dstIdx++].lerp(a1, a2, compA / (compA - compB))
                    }
                } else if (compB > 0) {
                    dst[dstIdx++].lerp(a1, a2, compA / (compA - compB))
                    dst[dstIdx++].copyFrom(a2)
                }
                a1.copyFrom(a2)
                a2 = src[srcIdx++]
                compA = compB
            }
            dst[dstIdx].copyFrom(dst[0])
            return dstIdx
        }
    }
}
