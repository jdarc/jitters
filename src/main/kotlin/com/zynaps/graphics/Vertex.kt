package com.zynaps.graphics

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import java.lang.Math.fma
import kotlin.math.sqrt

internal class Vertex {
    var x = 0F
    var y = 0F
    var z = 0F
    var w = 0F
    var u = 0F
    var v = 0F
    var l = 0F

    fun copyFrom(other: Vertex) {
        x = other.x
        y = other.y
        z = other.z
        w = other.w
        u = other.u
        v = other.v
        l = other.l
    }

    fun transfer(i: Int, vertexBuffer: FloatArray, transform: Matrix4, normalMatrix: Matrix4, lightDir: Vector3) {
        val svx = vertexBuffer[i + 0]
        val svy = vertexBuffer[i + 1]
        val svz = vertexBuffer[i + 2]
        val snx = vertexBuffer[i + 3]
        val sny = vertexBuffer[i + 4]
        val snz = vertexBuffer[i + 5]
        val tnx = fma(normalMatrix.m00, snx, fma(normalMatrix.m10, sny, normalMatrix.m20 * snz))
        val tny = fma(normalMatrix.m01, snx, fma(normalMatrix.m11, sny, normalMatrix.m21 * snz))
        val tnz = fma(normalMatrix.m02, snx, fma(normalMatrix.m12, sny, normalMatrix.m22 * snz))
        x = fma(transform.m00, svx, fma(transform.m10, svy, fma(transform.m20, svz, transform.m30)))
        y = fma(transform.m01, svx, fma(transform.m11, svy, fma(transform.m21, svz, transform.m31)))
        z = fma(transform.m02, svx, fma(transform.m12, svy, fma(transform.m22, svz, transform.m32)))
        w = fma(transform.m03, svx, fma(transform.m13, svy, fma(transform.m23, svz, transform.m33)))
        u = vertexBuffer[i + 6]
        v = vertexBuffer[i + 7]
        l = ((-tnx * lightDir.x - tny * lightDir.y - tnz * lightDir.z) / sqrt(tnx * tnx + tny * tny + tnz * tnz)).coerceIn(0F, 1F)
    }

    fun lerp(from: Vertex, to: Vertex, t: Float) {
        x = fma(to.x - from.x, t, from.x)
        y = fma(to.y - from.y, t, from.y)
        z = fma(to.z - from.z, t, from.z)
        w = fma(to.w - from.w, t, from.w)
        u = fma(to.u - from.u, t, from.u)
        v = fma(to.v - from.v, t, from.v)
        l = fma(to.l - from.l, t, from.l)
    }

    fun project(width: Int, height: Int): Vertex {
        w = 1F / w
        x = 0.5F * fma(x, w, 1F) * width
        y = 0.5F * fma(-y, w, 1F) * height
        z = 0.5F * fma(z, w, 1F)
        u *= w
        v *= w
        l *= w
        return this
    }
}
