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

package com.zynaps.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Vector3(val x: Float, val y: Float, val z: Float) {

    operator fun unaryMinus() = Vector3(-x, -y, -z)

    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)

    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)

    operator fun div(s: Float) = Vector3(x / s, y / s, z / s)

    operator fun div(v: Vector3) = Vector3(x / v.x, y / v.y, z / v.z)

    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)

    operator fun times(v: Vector3) = Vector3(x * v.x, y * v.y, z * v.z)

    operator fun times(m: Matrix4) = Vector3(
        x * m.m00 + y * m.m10 + z * m.m20 + m.m30,
        x * m.m01 + y * m.m11 + z * m.m21 + m.m31,
        x * m.m02 + y * m.m12 + z * m.m22 + m.m32
    )

    fun length() = sqrt(lengthSquared())

    fun lengthSquared() = dot(this, this)

    companion object {
        val ONE = Vector3(1F, 1F, 1F)
        val ZERO = Vector3(0F, 0F, 0F)

        val UNIT_X = Vector3(1F, 0F, 0F)
        val UNIT_Y = Vector3(0F, 1F, 0F)
        val UNIT_Z = Vector3(0F, 0F, 1F)

        fun abs(value: Vector3) = Vector3(abs(value.x), abs(value.y), abs(value.z))

        fun min(a: Vector3, b: Vector3) = Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))

        fun max(a: Vector3, b: Vector3) = Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

        fun clamp(value: Vector3, min: Vector3, max: Vector3) = min(max, max(value, min))

        fun dot(a: Vector3, b: Vector3) = a.x * b.x + a.y * b.y + a.z * b.z

        fun cross(a: Vector3, b: Vector3) = Vector3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

        fun normalize(v: Vector3) = v / sqrt(dot(v, v))
    }
}
