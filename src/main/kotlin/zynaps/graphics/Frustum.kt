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

        left.x = comb.m03 + comb.m00
        left.y = comb.m13 + comb.m10
        left.z = comb.m23 + comb.m20
        left.d = -(comb.m33 + comb.m30)

        right.x = comb.m03 - comb.m00
        right.y = comb.m13 - comb.m10
        right.z = comb.m23 - comb.m20
        right.d = -(comb.m33 - comb.m30)

        bottom.x = comb.m03 + comb.m01
        bottom.y = comb.m13 + comb.m11
        bottom.z = comb.m23 + comb.m21
        bottom.d = -(comb.m33 + comb.m31)

        top.x = comb.m03 - comb.m01
        top.y = comb.m13 - comb.m11
        top.z = comb.m23 - comb.m21
        top.d = -(comb.m33 - comb.m31)

        near.x = comb.m03 + comb.m02
        near.y = comb.m13 + comb.m12
        near.z = comb.m23 + comb.m22
        near.d = -(comb.m33 + comb.m32)

        far.x = comb.m03 - comb.m02
        far.y = comb.m13 - comb.m12
        far.z = comb.m23 - comb.m22
        far.d = -(comb.m33 - comb.m32)
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
            if (Plane.dotNormal(plane, Vector3(min.x, max.y, min.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(max.x, max.y, min.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(max.x, min.y, min.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(min.x, min.y, min.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(min.x, max.y, max.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(max.x, max.y, max.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(max.x, min.y, max.z)) < plane.d) ++l
            if (Plane.dotNormal(plane, Vector3(min.x, min.y, max.z)) < plane.d) ++l
            return l
        }
    }
}
