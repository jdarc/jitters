/*
 * Copyright (c) 2021 Jean d'Arc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.zynaps.graphics

import com.zynaps.math.Aabb
import com.zynaps.math.Matrix4
import com.zynaps.math.Plane

@Suppress("DuplicatedCode")
class Frustum {
    private var left = Plane.ZERO
    private var right = Plane.ZERO
    private var top = Plane.ZERO
    private var bottom = Plane.ZERO
    private var near = Plane.ZERO
    private var far = Plane.ZERO

    fun configure(view: Matrix4, proj: Matrix4) {
        val comb = proj * view
        left = Plane(comb.m30 + comb.m00, comb.m31 + comb.m01, comb.m32 + comb.m02, comb.m33 + comb.m03)
        right = Plane(comb.m30 - comb.m00, comb.m31 - comb.m01, comb.m32 - comb.m02, comb.m33 - comb.m03)
        bottom = Plane(comb.m30 + comb.m10, comb.m31 + comb.m11, comb.m32 + comb.m12, comb.m33 + comb.m13)
        top = Plane(comb.m30 - comb.m10, comb.m31 - comb.m11, comb.m32 - comb.m12, comb.m33 - comb.m13)
        near = Plane(comb.m30 + comb.m20, comb.m31 + comb.m21, comb.m32 + comb.m22, comb.m33 + comb.m23)
        far = Plane(comb.m30 - comb.m20, comb.m31 - comb.m21, comb.m32 - comb.m22, comb.m33 - comb.m23)
    }

    fun test(box: Aabb): Containment {
        val vc0 = box.pointsBehind(near)
        if (vc0 == 8) return Containment.OUTSIDE

        val vc1 = box.pointsBehind(far)
        if (vc1 == 8) return Containment.OUTSIDE

        val vc2 = box.pointsBehind(left)
        if (vc2 == 8) return Containment.OUTSIDE

        val vc3 = box.pointsBehind(right)
        if (vc3 == 8) return Containment.OUTSIDE

        val vc4 = box.pointsBehind(top)
        if (vc4 == 8) return Containment.OUTSIDE

        val vc5 = box.pointsBehind(bottom)
        if (vc5 == 8) return Containment.OUTSIDE

        return if (vc0 + vc1 + vc2 + vc3 + vc4 + vc5 == 0) Containment.INSIDE else Containment.PARTIAL
    }
}
