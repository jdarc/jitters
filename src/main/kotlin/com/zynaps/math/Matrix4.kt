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

import kotlin.math.*

@Suppress("unused", "MemberVisibilityCanBePrivate", "DuplicatedCode")
data class Matrix4(
    @JvmField val m00: Float, @JvmField val m10: Float, @JvmField val m20: Float, @JvmField val m30: Float,
    @JvmField val m01: Float, @JvmField val m11: Float, @JvmField val m21: Float, @JvmField val m31: Float,
    @JvmField val m02: Float, @JvmField val m12: Float, @JvmField val m22: Float, @JvmField val m32: Float,
    @JvmField val m03: Float, @JvmField val m13: Float, @JvmField val m23: Float, @JvmField val m33: Float
) {

    operator fun unaryMinus() = Matrix4(
        -m00, -m10, -m20, -m30,
        -m01, -m11, -m21, -m31,
        -m02, -m12, -m22, -m32,
        -m03, -m13, -m23, -m33
    )

    operator fun plus(rhs: Float) = Matrix4(
        m00 + rhs, m10 + rhs, m20 + rhs, m30 + rhs,
        m01 + rhs, m11 + rhs, m21 + rhs, m31 + rhs,
        m02 + rhs, m12 + rhs, m22 + rhs, m32 + rhs,
        m03 + rhs, m13 + rhs, m23 + rhs, m33 + rhs
    )

    operator fun plus(rhs: Matrix4) = Matrix4(
        m00 + rhs.m00, m10 + rhs.m10, m20 + rhs.m20, m30 + rhs.m30,
        m01 + rhs.m01, m11 + rhs.m11, m21 + rhs.m21, m31 + rhs.m31,
        m02 + rhs.m02, m12 + rhs.m12, m22 + rhs.m22, m32 + rhs.m32,
        m03 + rhs.m03, m13 + rhs.m13, m23 + rhs.m23, m33 + rhs.m33
    )

    operator fun minus(rhs: Float) = Matrix4(
        m00 - rhs, m10 - rhs, m20 - rhs, m30 - rhs,
        m01 - rhs, m11 - rhs, m21 - rhs, m31 - rhs,
        m02 - rhs, m12 - rhs, m22 - rhs, m32 - rhs,
        m03 - rhs, m13 - rhs, m23 - rhs, m33 - rhs
    )

    operator fun minus(rhs: Matrix4) = Matrix4(
        m00 - rhs.m00, m10 - rhs.m10, m20 - rhs.m20, m30 - rhs.m30,
        m01 - rhs.m01, m11 - rhs.m11, m21 - rhs.m21, m31 - rhs.m31,
        m02 - rhs.m02, m12 - rhs.m12, m22 - rhs.m22, m32 - rhs.m32,
        m03 - rhs.m03, m13 - rhs.m13, m23 - rhs.m23, m33 - rhs.m33
    )

    operator fun times(rhs: Float) = Matrix4(
        m00 * rhs, m10 * rhs, m20 * rhs, m30 * rhs,
        m01 * rhs, m11 * rhs, m21 * rhs, m31 * rhs,
        m02 * rhs, m12 * rhs, m22 * rhs, m32 * rhs,
        m03 * rhs, m13 * rhs, m23 * rhs, m33 * rhs
    )

    operator fun times(rhs: Vector3): Vector3 {
        val x = m00 * rhs.x + m01 * rhs.y + m02 * rhs.z + m03
        val y = m10 * rhs.x + m11 * rhs.y + m12 * rhs.z + m13
        val z = m20 * rhs.x + m21 * rhs.y + m22 * rhs.z + m23
        return Vector3(x, y, z)
    }

    operator fun times(rhs: Matrix4) = Matrix4(
        (m00 * rhs.m00) + (m01 * rhs.m10) + (m02 * rhs.m20) + (m03 * rhs.m30),
        (m10 * rhs.m00) + (m11 * rhs.m10) + (m12 * rhs.m20) + (m13 * rhs.m30),
        (m20 * rhs.m00) + (m21 * rhs.m10) + (m22 * rhs.m20) + (m23 * rhs.m30),
        (m30 * rhs.m00) + (m31 * rhs.m10) + (m32 * rhs.m20) + (m33 * rhs.m30),
        (m00 * rhs.m01) + (m01 * rhs.m11) + (m02 * rhs.m21) + (m03 * rhs.m31),
        (m10 * rhs.m01) + (m11 * rhs.m11) + (m12 * rhs.m21) + (m13 * rhs.m31),
        (m20 * rhs.m01) + (m21 * rhs.m11) + (m22 * rhs.m21) + (m23 * rhs.m31),
        (m30 * rhs.m01) + (m31 * rhs.m11) + (m32 * rhs.m21) + (m33 * rhs.m31),
        (m00 * rhs.m02) + (m01 * rhs.m12) + (m02 * rhs.m22) + (m03 * rhs.m32),
        (m10 * rhs.m02) + (m11 * rhs.m12) + (m12 * rhs.m22) + (m13 * rhs.m32),
        (m20 * rhs.m02) + (m21 * rhs.m12) + (m22 * rhs.m22) + (m23 * rhs.m32),
        (m30 * rhs.m02) + (m31 * rhs.m12) + (m32 * rhs.m22) + (m33 * rhs.m32),
        (m00 * rhs.m03) + (m01 * rhs.m13) + (m02 * rhs.m23) + (m03 * rhs.m33),
        (m10 * rhs.m03) + (m11 * rhs.m13) + (m12 * rhs.m23) + (m13 * rhs.m33),
        (m20 * rhs.m03) + (m21 * rhs.m13) + (m22 * rhs.m23) + (m23 * rhs.m33),
        (m30 * rhs.m03) + (m31 * rhs.m13) + (m32 * rhs.m23) + (m33 * rhs.m33)
    )

    companion object {
        val IDENTITY = Matrix4(1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F)

        fun transformNormal(m: Matrix4, v: Vector3): Vector3 {
            val x = m.m00 * v.x + m.m01 * v.y + m.m02 * v.z
            val y = m.m10 * v.x + m.m11 * v.y + m.m12 * v.z
            val z = m.m20 * v.x + m.m21 * v.y + m.m22 * v.z
            return Vector3(x, y, z)
        }

        fun transpose(matrix: Matrix4) = Matrix4(
            matrix.m00, matrix.m01, matrix.m02, matrix.m03,
            matrix.m10, matrix.m11, matrix.m12, matrix.m13,
            matrix.m20, matrix.m21, matrix.m22, matrix.m23,
            matrix.m30, matrix.m31, matrix.m32, matrix.m33
        )

        fun orthonormalise(matrix: Matrix4): Matrix4 {
            val  u1 = Vector3(matrix.m00, matrix.m10, matrix.m20)
            val  u2 = Vector3(matrix.m01, matrix.m11, matrix.m21)
            val  u3 = Vector3(matrix.m02, matrix.m12, matrix.m22)
            val w1 = Vector3.normalize(u1)
            val w2 = Vector3.normalize(u2 - proj(w1, u2))
            val w3 = Vector3.normalize(u3 - proj(w1, u3) - proj(w2, u3))
            return Matrix4(w1.x, w1.y, w1.z, 0F, w2.x, w2.y, w2.z, 0F, w3.x, w3.y, w3.z, 0F, 0F, 0F, 0F, 1F)
        }

        private fun proj(v1: Vector3, v2: Vector3) = v1 * (Vector3.dot(v1, v2) / v1.lengthSquared())

        fun invert(matrix: Matrix4): Matrix4 {
            val b00 = matrix.m00 * matrix.m11 - matrix.m10 * matrix.m01
            val b01 = matrix.m00 * matrix.m21 - matrix.m20 * matrix.m01
            val b02 = matrix.m00 * matrix.m31 - matrix.m30 * matrix.m01
            val b03 = matrix.m10 * matrix.m21 - matrix.m20 * matrix.m11
            val b04 = matrix.m10 * matrix.m31 - matrix.m30 * matrix.m11
            val b05 = matrix.m20 * matrix.m31 - matrix.m30 * matrix.m21
            val b06 = matrix.m02 * matrix.m13 - matrix.m12 * matrix.m03
            val b07 = matrix.m02 * matrix.m23 - matrix.m22 * matrix.m03
            val b08 = matrix.m02 * matrix.m33 - matrix.m32 * matrix.m03
            val b09 = matrix.m12 * matrix.m23 - matrix.m22 * matrix.m13
            val b10 = matrix.m12 * matrix.m33 - matrix.m32 * matrix.m13
            val b11 = matrix.m22 * matrix.m33 - matrix.m32 * matrix.m23
            val invDet = 1F / (b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06)
            val a = (b11 * matrix.m11 - b10 * matrix.m21 + b09 * matrix.m31) * invDet
            val b = (b10 * matrix.m20 - b11 * matrix.m10 - b09 * matrix.m30) * invDet
            val c = (b05 * matrix.m13 - b04 * matrix.m23 + b03 * matrix.m33) * invDet
            val d = (b04 * matrix.m22 - b05 * matrix.m12 - b03 * matrix.m32) * invDet
            val e = (b08 * matrix.m21 - b11 * matrix.m01 - b07 * matrix.m31) * invDet
            val f = (b11 * matrix.m00 - b08 * matrix.m20 + b07 * matrix.m30) * invDet
            val g = (b02 * matrix.m23 - b05 * matrix.m03 - b01 * matrix.m33) * invDet
            val h = (b05 * matrix.m02 - b02 * matrix.m22 + b01 * matrix.m32) * invDet
            val i = (b10 * matrix.m01 - b08 * matrix.m11 + b06 * matrix.m31) * invDet
            val j = (b08 * matrix.m10 - b10 * matrix.m00 - b06 * matrix.m30) * invDet
            val k = (b04 * matrix.m03 - b02 * matrix.m13 + b00 * matrix.m33) * invDet
            val l = (b02 * matrix.m12 - b04 * matrix.m02 - b00 * matrix.m32) * invDet
            val m = (b07 * matrix.m11 - b09 * matrix.m01 - b06 * matrix.m21) * invDet
            val n = (b09 * matrix.m00 - b07 * matrix.m10 + b06 * matrix.m20) * invDet
            val o = (b01 * matrix.m13 - b03 * matrix.m03 - b00 * matrix.m23) * invDet
            val p = (b03 * matrix.m02 - b01 * matrix.m12 + b00 * matrix.m22) * invDet
            return Matrix4(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
        }

        fun createTranslation(vec: Vector3) = createTranslation(vec.x, vec.y, vec.z)
        fun createTranslation(x: Float, y: Float, z: Float) = Matrix4(1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, x, y, z, 1F)

        fun createScale(scale: Float) = createScale(scale, scale, scale)
        fun createScale(vec: Vector3) = createScale(vec.x, vec.y, vec.z)
        fun createScale(x: Float, y: Float, z: Float) = Matrix4(x, 0F, 0F, 0F, 0F, y, 0F, 0F, 0F, 0F, z, 0F, 0F, 0F, 0F, 1F)

        fun createRotationX(radians: Float) = createFromAxisAngle(Vector3.UNIT_X, radians)
        fun createRotationY(radians: Float) = createFromAxisAngle(Vector3.UNIT_Y, radians)
        fun createRotationZ(radians: Float) = createFromAxisAngle(Vector3.UNIT_Z, radians)

        fun createFromAxisAngle(axis: Vector3, radians: Float) = createFromAxisAngle(axis.x, axis.y, axis.z, radians)
        fun createFromAxisAngle(x: Float, y: Float, z: Float, radians: Float): Matrix4 {
            val magnitude = sqrt(x * x + y * y + z * z)
            if (magnitude < 0.00001F) throw RuntimeException("create from axis and angle failed, vector length too small")
            val ax = x / magnitude
            val ay = y / magnitude
            val az = z / magnitude
            val cos = cos(radians)
            val sin = sin(radians)
            val t = 1F - cos
            val xz = ax * az
            val xy = ax * ay
            val yz = ay * az
            val m00 = t * ax * ax + cos
            val m01 = t * xy - az * sin
            val m02 = t * xz + ay * sin
            val m10 = t * xy + az * sin
            val m11 = t * ay * ay + cos
            val m12 = t * yz - ax * sin
            val m20 = t * xz - ay * sin
            val m21 = t * yz + ax * sin
            val m22 = t * az * az + cos
            return Matrix4(m00, m10, m20, 0F, m01, m11, m21, 0F, m02, m12, m22, 0F, 0F, 0F, 0F, 1F)
        }

        fun createLookAt(eye: Vector3, at: Vector3, up: Vector3): Matrix4 {
            val d = Vector3.normalize(eye - at)
            val r = Vector3.normalize(Vector3.cross(up, d))
            val u = Vector3.cross(d, r)
            val x = -Vector3.dot(r, eye)
            val y = -Vector3.dot(u, eye)
            val z = -Vector3.dot(d, eye)
            return Matrix4(r.x, u.x, d.x, 0F, r.y, u.y, d.y, 0F, r.z, u.z, d.z, 0F, x, y, z, 1F)
        }

        fun createOrthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 {
            val m00 = 2F / (right - left)
            val m11 = 2F / (top - bottom)
            val m22 = 2F / (far - near)
            val m03 = (right + left) / (right - left)
            val m13 = (top + bottom) / (top - bottom)
            val m23 = (far + near) / (far - near)
            return Matrix4(m00, 0F, 0F, 0F, 0F, m11, 0F, 0F, 0F, 0F, -m22, 0F, -m03, -m13, -m23, 1.0F)
        }

        fun createPerspectiveFov(fov: Float, aspectRatio: Float, near: Float, far: Float): Matrix4 {
            val m11 = tan(PI * 0.5 - 0.5 * fov).toFloat()
            val m00 = m11 / aspectRatio
            val m22 = (near + far) / (near - far)
            val m23 = (near * far) / (near - far) * 2F
            return Matrix4(m00, 0F, 0F, 0F, 0F, m11, 0F, 0F, 0F, 0F, m22, -1F, 0F, 0F, m23, 0F)
        }
    }
}
