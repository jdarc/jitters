package zynaps.graphics

import zynaps.math.Matrix4
import zynaps.math.Plane
import zynaps.math.Vector3

class Frustum : Container {
    private var left = Plane.ZERO
    private var right = Plane.ZERO
    private var top = Plane.ZERO
    private var bottom = Plane.ZERO
    private var near = Plane.ZERO
    private var far = Plane.ZERO

    fun extractPlanes(view: Matrix4, proj: Matrix4) {
        val comb = view * proj
        left = Plane(comb.m03 + comb.m00, comb.m13 + comb.m10, comb.m23 + comb.m20, -(comb.m33 + comb.m30))
        right = Plane(comb.m03 - comb.m00, comb.m13 - comb.m10, comb.m23 - comb.m20, -(comb.m33 - comb.m30))
        bottom = Plane(comb.m03 + comb.m01, comb.m13 + comb.m11, comb.m23 + comb.m21, -(comb.m33 + comb.m31))
        top = Plane(comb.m03 - comb.m01, comb.m13 - comb.m11, comb.m23 - comb.m21, -(comb.m33 - comb.m31))
        near = Plane(comb.m03 + comb.m02, comb.m13 + comb.m12, comb.m23 + comb.m22, -(comb.m33 + comb.m32))
        far = Plane(comb.m03 - comb.m02, comb.m13 - comb.m12, comb.m23 - comb.m22, -(comb.m33 - comb.m32))
    }

    override fun contains(box: Aabb): Containment {
        val vc0 = countPointsOutside(box, near)
        if (vc0 == 8) return Containment.OUTSIDE

        val vc1 = countPointsOutside(box, far)
        if (vc1 == 8) return Containment.OUTSIDE

        val vc2 = countPointsOutside(box, left)
        if (vc2 == 8) return Containment.OUTSIDE

        val vc3 = countPointsOutside(box, right)
        if (vc3 == 8) return Containment.OUTSIDE

        val vc4 = countPointsOutside(box, top)
        if (vc4 == 8) return Containment.OUTSIDE

        val vc5 = countPointsOutside(box, bottom)
        if (vc5 == 8) return Containment.OUTSIDE

        return if (vc0 + vc1 + vc2 + vc3 + vc4 + vc5 == 0) Containment.INSIDE else Containment.PARTIAL
    }

    companion object {
        fun countPointsOutside(box: Aabb, plane: Plane): Int {
            var l = 0
            val min = box.min
            val max = box.max
            if (Vector3.dot(plane.normal, Vector3(min.x, min.y, min.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(min.x, min.y, max.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(min.x, max.y, min.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(min.x, max.y, max.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(max.x, min.y, min.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(max.x, min.y, max.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(max.x, max.y, min.z)) - plane.distance < 0F) ++l
            if (Vector3.dot(plane.normal, Vector3(max.x, max.y, max.z)) - plane.distance < 0F) ++l
            return l
        }
    }
}
