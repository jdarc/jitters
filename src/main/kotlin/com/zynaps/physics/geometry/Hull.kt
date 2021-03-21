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

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.Settings
import kotlin.math.max

class Hull(private val points: Array<Vector3>, scale: Float = 1F) : Shape() {
    private val scale = scale + Settings.COLLISION_TOLERANCE
    private var transpose = Matrix4.IDENTITY

    override var origin = Vector3.ZERO
    override var basis = Matrix4.IDENTITY
        set(value) {
            field = value
            transpose = Matrix4.transpose(value)
        }

    override val boundingSphere = calculateBoundingSphere()

    override fun getSupport(direction: Vector3) = basis * localGetSupporting(Vector3.normalize(transpose * direction)) + origin

    override fun calculateBodyInertia(mass: Float) = Matrix4.createScale(0.4F * mass * boundingSphere * boundingSphere)

    private fun localGetSupporting(v: Vector3): Vector3 {
        var out = Vector3.ZERO
        var dist = Float.NEGATIVE_INFINITY
        for (p in points) {
            val dot = Vector3.dot(v, p)
            if (dot > dist) {
                dist = dot
                out = p
            }
        }
        return out * scale
    }

    private fun calculateBoundingSphere(): Float {
        var min = Vector3.POSITIVE_INFINITY
        var max = Vector3.NEGATIVE_INFINITY
        points.forEach {
            min = Vector3.min(it, min)
            max = Vector3.max(it, max)
        }
        val size = max - min
        return max(size.x, max(size.y, size.z)) * 0.5F * scale
    }
}
