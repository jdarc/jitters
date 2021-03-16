package zynaps.jitters.geometry

import zynaps.math.Vector3

class Plane : Shape() {

    override fun getSupport(direction: Vector3): Vector3 {
        var best: Vector3? = null
        var dist = Float.NEGATIVE_INFINITY
        val dir = Vector3.normalize(direction)
        points.map { origin + it }.forEach {
            val dot = Vector3.dot(it, dir)
            if (dot >= dist) {
                dist = dot
                best = it
            }
        }
        return best ?: Vector3.ZERO
    }

    private companion object {
        const val SCALE = 1000F
        const val MARGIN = 0.01F

        val points = arrayOf(
            Vector3(-SCALE, -MARGIN, SCALE), Vector3(SCALE, -MARGIN, SCALE),
            Vector3(SCALE, -MARGIN, -SCALE), Vector3(-SCALE, -MARGIN, -SCALE),
            Vector3(-SCALE, MARGIN, SCALE), Vector3(SCALE, MARGIN, SCALE),
            Vector3(SCALE, MARGIN, -SCALE), Vector3(-SCALE, MARGIN, -SCALE)
        )
    }
}
