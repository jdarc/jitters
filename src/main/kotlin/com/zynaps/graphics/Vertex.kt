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
        val tnx = fma(normalMatrix.m00, snx, fma(normalMatrix.m01, sny, normalMatrix.m02 * snz))
        val tny = fma(normalMatrix.m10, snx, fma(normalMatrix.m11, sny, normalMatrix.m12 * snz))
        val tnz = fma(normalMatrix.m20, snx, fma(normalMatrix.m21, sny, normalMatrix.m22 * snz))
        x = fma(transform.m00, svx, fma(transform.m01, svy, fma(transform.m02, svz, transform.m03)))
        y = fma(transform.m10, svx, fma(transform.m11, svy, fma(transform.m12, svz, transform.m13)))
        z = fma(transform.m20, svx, fma(transform.m21, svy, fma(transform.m22, svz, transform.m23)))
        w = fma(transform.m30, svx, fma(transform.m31, svy, fma(transform.m32, svz, transform.m33)))
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
