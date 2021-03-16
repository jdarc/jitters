package zynaps.graphics

import zynaps.math.Matrix4
import zynaps.math.Vector3
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
        val tnx = normalMatrix.m00 * snx + normalMatrix.m10 * sny + normalMatrix.m20 * snz
        val tny = normalMatrix.m01 * snx + normalMatrix.m11 * sny + normalMatrix.m21 * snz
        val tnz = normalMatrix.m02 * snx + normalMatrix.m12 * sny + normalMatrix.m22 * snz
        x = transform.m00 * svx + transform.m10 * svy + transform.m20 * svz + transform.m30
        y = transform.m01 * svx + transform.m11 * svy + transform.m21 * svz + transform.m31
        z = transform.m02 * svx + transform.m12 * svy + transform.m22 * svz + transform.m32
        w = transform.m03 * svx + transform.m13 * svy + transform.m23 * svz + transform.m33
        u = vertexBuffer[i + 6]
        v = vertexBuffer[i + 7]
        l = ((-tnx * lightDir.x - tny * lightDir.y - tnz * lightDir.z) / sqrt(tnx * tnx + tny * tny + tnz * tnz)).coerceIn(0F, 1F)
    }

    fun lerp(from: Vertex, to: Vertex, t: Float) {
        x = from.x + (to.x - from.x) * t
        y = from.y + (to.y - from.y) * t
        z = from.z + (to.z - from.z) * t
        w = from.w + (to.w - from.w) * t
        u = from.u + (to.u - from.u) * t
        v = from.v + (to.v - from.v) * t
        l = from.l + (to.l - from.l) * t
    }

    fun project(width: Int, height: Int): Vertex {
        w = 1F / w
        x = (1F + x * w) * 0.5F * width
        y = (1F - y * w) * 0.5F * height
        z = (1F + z * w) * 0.5F
        u *= w
        v *= w
        l *= w
        return this
    }
}
