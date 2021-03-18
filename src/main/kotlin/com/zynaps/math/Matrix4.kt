package com.zynaps.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class Matrix4(
    val m00: Float, val m01: Float, val m02: Float, val m03: Float,
    val m10: Float, val m11: Float, val m12: Float, val m13: Float,
    val m20: Float, val m21: Float, val m22: Float, val m23: Float,
    val m30: Float, val m31: Float, val m32: Float, val m33: Float
) {

    operator fun times(v: Vector3) = Vector3(
        m00 * v.x + m01 * v.y + m02 * v.z + m03,
        m10 * v.x + m11 * v.y + m12 * v.z + m13,
        m20 * v.x + m21 * v.y + m22 * v.z + m23
    )

    operator fun times(m: Matrix4) = Matrix4(
        m00 * m.m00 + m01 * m.m10 + m02 * m.m20 + m03 * m.m30,
        m00 * m.m01 + m01 * m.m11 + m02 * m.m21 + m03 * m.m31,
        m00 * m.m02 + m01 * m.m12 + m02 * m.m22 + m03 * m.m32,
        m00 * m.m03 + m01 * m.m13 + m02 * m.m23 + m03 * m.m33,
        m10 * m.m00 + m11 * m.m10 + m12 * m.m20 + m13 * m.m30,
        m10 * m.m01 + m11 * m.m11 + m12 * m.m21 + m13 * m.m31,
        m10 * m.m02 + m11 * m.m12 + m12 * m.m22 + m13 * m.m32,
        m10 * m.m03 + m11 * m.m13 + m12 * m.m23 + m13 * m.m33,
        m20 * m.m00 + m21 * m.m10 + m22 * m.m20 + m23 * m.m30,
        m20 * m.m01 + m21 * m.m11 + m22 * m.m21 + m23 * m.m31,
        m20 * m.m02 + m21 * m.m12 + m22 * m.m22 + m23 * m.m32,
        m20 * m.m03 + m21 * m.m13 + m22 * m.m23 + m23 * m.m33,
        m30 * m.m00 + m31 * m.m10 + m32 * m.m20 + m33 * m.m30,
        m30 * m.m01 + m31 * m.m11 + m32 * m.m21 + m33 * m.m31,
        m30 * m.m02 + m31 * m.m12 + m32 * m.m22 + m33 * m.m32,
        m30 * m.m03 + m31 * m.m13 + m32 * m.m23 + m33 * m.m33
    )

    companion object {
        val IDENTITY = Matrix4(1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F)

        fun transpose(m: Matrix4) = Matrix4(m.m00, m.m10, m.m20, m.m30, m.m01, m.m11, m.m21, m.m31, m.m02, m.m12, m.m22, m.m32, m.m03, m.m13, m.m23, m.m33)

        fun createScale(x: Float, y: Float, z: Float) = Matrix4(x, 0F, 0F, 0F, 0F, y, 0F, 0F, 0F, 0F, z, 0F, 0F, 0F, 0F, 1F)
        fun createScale(scale: Float) = createScale(scale, scale, scale)
        fun createScale(scale: Vector3) = createScale(scale.x, scale.y, scale.z)

        fun createTranslation(x: Float, y: Float, z: Float) = Matrix4(1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, x, y, z, 1F)
        fun createTranslation(v: Vector3) = createTranslation(v.x, v.y, v.z)

        fun createRotationX(angle: Float) = createFromAxisAngle(Vector3.UNIT_X, angle)
        fun createRotationY(angle: Float) = createFromAxisAngle(Vector3.UNIT_Y, angle)
        fun createRotationZ(angle: Float) = createFromAxisAngle(Vector3.UNIT_Z, angle)

        fun createFromAxisAngle(axis: Vector3, radians: Float): Matrix4 {
            val c = cos(-radians)
            val s = sin(-radians)
            val t = 1F - c
            return Matrix4(
                c + axis.x * axis.x * t, axis.x * axis.y * t - axis.z * s, axis.x * axis.z * t + axis.y * s, 0F,
                axis.x * axis.y * t + axis.z * s, c + axis.y * axis.y * t, axis.y * axis.z * t - axis.x * s, 0F,
                axis.x * axis.z * t - axis.y * s, axis.y * axis.z * t + axis.x * s, c + axis.z * axis.z * t, 0F,
                0F, 0F, 0F, 1F
            )
        }

        fun invert(src: Matrix4): Matrix4 {
            val b00 = src.m00 * src.m11 - src.m10 * src.m01
            val b01 = src.m00 * src.m21 - src.m20 * src.m01
            val b02 = src.m00 * src.m31 - src.m30 * src.m01
            val b03 = src.m10 * src.m21 - src.m20 * src.m11
            val b04 = src.m10 * src.m31 - src.m30 * src.m11
            val b05 = src.m20 * src.m31 - src.m30 * src.m21
            val b06 = src.m02 * src.m13 - src.m12 * src.m03
            val b07 = src.m02 * src.m23 - src.m22 * src.m03
            val b08 = src.m02 * src.m33 - src.m32 * src.m03
            val b09 = src.m12 * src.m23 - src.m22 * src.m13
            val b10 = src.m12 * src.m33 - src.m32 * src.m13
            val b11 = src.m22 * src.m33 - src.m32 * src.m23
            val det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
            val invDet = 1F / det
            val a = (b11 * src.m11 - b10 * src.m21 + b09 * src.m31) * invDet
            val b = (b10 * src.m20 - b11 * src.m10 - b09 * src.m30) * invDet
            val c = (b05 * src.m13 - b04 * src.m23 + b03 * src.m33) * invDet
            val d = (b04 * src.m22 - b05 * src.m12 - b03 * src.m32) * invDet
            val e = (b08 * src.m21 - b11 * src.m01 - b07 * src.m31) * invDet
            val f = (b11 * src.m00 - b08 * src.m20 + b07 * src.m30) * invDet
            val g = (b02 * src.m23 - b05 * src.m03 - b01 * src.m33) * invDet
            val h = (b05 * src.m02 - b02 * src.m22 + b01 * src.m32) * invDet
            val i = (b10 * src.m01 - b08 * src.m11 + b06 * src.m31) * invDet
            val j = (b08 * src.m10 - b10 * src.m00 - b06 * src.m30) * invDet
            val k = (b04 * src.m03 - b02 * src.m13 + b00 * src.m33) * invDet
            val l = (b02 * src.m12 - b04 * src.m02 - b00 * src.m32) * invDet
            val m = (b07 * src.m11 - b09 * src.m01 - b06 * src.m21) * invDet
            val n = (b09 * src.m00 - b07 * src.m10 + b06 * src.m20) * invDet
            val o = (b01 * src.m13 - b03 * src.m03 - b00 * src.m23) * invDet
            val p = (b03 * src.m02 - b01 * src.m12 + b00 * src.m22) * invDet
            return Matrix4(a, e, i, m, b, f, j, n, c, g, k, o, d, h, l, p)
        }

        fun createLookAt(eye: Vector3, center: Vector3, up: Vector3): Matrix4 {
            val d = Vector3.normalize(eye - center)
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
            val m22 = 1F / (near - far)
            val m30 = (left + right) / (left - right)
            val m31 = (top + bottom) / (bottom - top)
            val m32 = near / (near - far)
            return Matrix4(m00, 0F, 0F, 0F, 0F, m11, 0F, 0F, 0F, 0F, m22, 0F, m30, m31, m32, 1F)
        }

        fun createPerspectiveFov(fov: Float, aspectRatio: Float, near: Float, far: Float): Matrix4 {
            val m11 = tan(Scalar.PI * 0.5F - 0.5F * fov)
            val m00 = m11 / aspectRatio
            val m22 = (near + far) / (near - far)
            val m23 = (near * far) / (near - far) * 2F
            return Matrix4(m00, 0F, 0F, 0F, 0F, m11, 0F, 0F, 0F, 0F, m22, -1F, 0F, 0F, m23, 0F)
        }
    }
}
