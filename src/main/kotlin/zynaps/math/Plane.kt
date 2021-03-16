package zynaps.math

data class Plane(var x: Float, var y: Float, var z: Float, var d: Float) {

    companion object {
        val ZERO = Plane(0F, 0F, 0F, 0F)

        fun dotNormal(p: Plane, v: Vector3) = p.x * v.x + p.y * v.y + p.z * v.z
    }
}
