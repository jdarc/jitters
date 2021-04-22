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

package com.zynaps.physics.geometry

import com.zynaps.math.Aabb
import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.Globals
import kotlin.math.max

class ConvexHull(private val points: Array<Vector3>, scale: Float = 1F, tolerance: Float = Globals.COLLISION_TOLERANCE) : CollisionSkin() {
    private val scale = scale + tolerance

    private val bounds = points.fold(Aabb()) { box, vec -> box.aggregate(vec * this.scale) }

    override var origin = Vector3.ZERO

    override var basis = Matrix4.IDENTITY

    override val boundingSphere = max(bounds.width, max(bounds.height, bounds.depth)) * 0.5F

    override fun getSupport(direction: Vector3) = basis * getSupportLocal(direction * basis) + origin

    override fun calculateBodyInertia(mass: Float): Matrix4 {
        val xx = bounds.width * bounds.width
        val yy = bounds.height * bounds.height
        val zz = bounds.depth * bounds.depth
        return Matrix4.createScale(mass / 12F * (zz + yy), mass / 12F * (xx + zz), mass / 12F * (xx + yy))
    }

    private fun getSupportLocal(v: Vector3): Vector3 {
        var out = Vector3.ZERO
        var dist = Float.NEGATIVE_INFINITY
        for (point in points) {
            val dot = v.x * point.x + v.y * point.y + v.z * point.z
            if (dot > dist) {
                dist = dot
                out = point
            }
        }
        return out * scale
    }
}
