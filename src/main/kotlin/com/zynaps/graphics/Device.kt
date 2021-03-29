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
import com.zynaps.math.Scalar.ceil
import com.zynaps.math.Scalar.max
import com.zynaps.math.Scalar.min
import com.zynaps.math.Scalar.sqrt
import com.zynaps.math.Vector3
import com.zynaps.tools.Parallel
import java.lang.Math.fma

class Device(private val colorBuffer: IntArray, private val depthBuffer: FloatArray, private val width: Int, private val height: Int) {
    private var transformed = Array(4096) { Vertex() }
    private val clipper = Clipper()
    private val gradients = Gradients()
    private val topToBottom = Edge()
    private val topToMiddle = Edge()
    private val midToBottom = Edge()
    private var lightDir = Vector3.ZERO
    private var cullFn = ::renderFront
    private var renderFn = ::clipRender
    private val shader = if (colorBuffer.isEmpty()) ::noShade else ::shade

    var material: Material = Material.DEFAULT

    var world = Matrix4.IDENTITY
    var view = Matrix4.IDENTITY
    var proj = Matrix4.IDENTITY

    var cull
        get() = if (cullFn == ::renderFront) CullMode.BACK else CullMode.FRONT
        set(value) {
            cullFn = if (value == CullMode.BACK) ::renderFront else ::renderBack
        }

    var clip
        get() = renderFn == ::clipRender
        set(value) {
            renderFn = if (value) ::clipRender else ::fastRender
        }

    fun moveLight(x: Float, y: Float, z: Float) {
        lightDir = Vector3.normalize(Vector3(-x, -y, -z))
    }

    fun clear(color: Int) {
        colorBuffer.fill(color)
        depthBuffer.fill(1F)
    }

    fun draw(vertexBuffer: FloatArray, indexBuffer: IntArray, elementCount: Int) {
        val normalMatrix = Matrix4.transpose(Matrix4.invert(world))
        val transform = proj * view * world

        if (transformed.size < vertexBuffer.size / 8) {
            transformed += Array(transformed.size) { Vertex() }
        }

        Parallel.partition(vertexBuffer.size / 8) { _, from, to ->
            for (i in from * 8 until to * 8 step 8) {
                val t = transformed[i shr 3]
                val vx = vertexBuffer[i + 0]
                val vy = vertexBuffer[i + 1]
                val vz = vertexBuffer[i + 2]
                t.x = fma(transform.m00, vx, fma(transform.m01, vy, fma(transform.m02, vz, transform.m03)))
                t.y = fma(transform.m10, vx, fma(transform.m11, vy, fma(transform.m12, vz, transform.m13)))
                t.z = fma(transform.m20, vx, fma(transform.m21, vy, fma(transform.m22, vz, transform.m23)))
                t.w = fma(transform.m30, vx, fma(transform.m31, vy, fma(transform.m32, vz, transform.m33)))
                val nx = vertexBuffer[i + 3]
                val ny = vertexBuffer[i + 4]
                val nz = vertexBuffer[i + 5]
                val tnx = fma(normalMatrix.m00, nx, fma(normalMatrix.m01, ny, normalMatrix.m02 * nz))
                val tny = fma(normalMatrix.m10, nx, fma(normalMatrix.m11, ny, normalMatrix.m12 * nz))
                val tnz = fma(normalMatrix.m20, nx, fma(normalMatrix.m21, ny, normalMatrix.m22 * nz))
                t.l = ((-tnx * lightDir.x - tny * lightDir.y - tnz * lightDir.z) / sqrt(tnx * tnx + tny * tny + tnz * tnz)).coerceIn(0F, 1F)
                t.u = vertexBuffer[i + 6]
                t.v = vertexBuffer[i + 7]
            }
        }

        val a = Vertex()
        val b = Vertex()
        val c = Vertex()
        for (i in 0 until elementCount step 3) renderFn(
            a.set(transformed[indexBuffer[i + 0]]),
            b.set(transformed[indexBuffer[i + 1]]),
            c.set(transformed[indexBuffer[i + 2]])
        )
    }

    private fun fastRender(a: Vertex, b: Vertex, c: Vertex) = cullFn(3, a, b, c, c, c)

    private fun clipRender(a: Vertex, b: Vertex, c: Vertex) =
        cullFn(clipper.clip(a, b, c), clipper[0], clipper[1], clipper[2], clipper[3], clipper[4])

    private fun renderBack(delta: Int, a: Vertex, b: Vertex, c: Vertex, d: Vertex, e: Vertex) = when (delta) {
        3 -> renderFront(delta, c, b, a, a, a)
        4 -> renderFront(delta, d, c, b, a, a)
        else -> renderFront(delta, e, d, c, b, a)
    }

    private fun renderFront(delta: Int, a: Vertex, b: Vertex, c: Vertex, d: Vertex, e: Vertex) {
        if (delta < 3 || isBackFacing(a, b, c)) return

        a.project(width, height)
        b.project(width, height)
        c.project(width, height)

        gradients.computeGradients(a, b, c)
        scanOrder(a, b, c)

        if (delta < 4) return
        scanOrder(a, c, d.project(width, height))

        if (delta < 5) return
        scanOrder(a, d, e.project(width, height))
    }

    private fun scanOrder(a: Vertex, b: Vertex, c: Vertex) = when {
        a.y < b.y -> when {
            c.y < a.y -> scanConvert(c, a, b, topToMiddle, topToBottom, midToBottom, topToBottom)
            b.y < c.y -> scanConvert(a, b, c, topToMiddle, topToBottom, midToBottom, topToBottom)
            else -> scanConvert(a, c, b, topToBottom, topToMiddle, topToBottom, midToBottom)
        }
        else -> when {
            c.y < b.y -> scanConvert(c, b, a, topToBottom, topToMiddle, topToBottom, midToBottom)
            a.y < c.y -> scanConvert(b, a, c, topToBottom, topToMiddle, topToBottom, midToBottom)
            else -> scanConvert(b, c, a, topToMiddle, topToBottom, midToBottom, topToBottom)
        }
    }

    private fun scanConvert(s0: Vertex, s1: Vertex, s2: Vertex, left1: Edge, right1: Edge, left2: Edge, right2: Edge) {
        if (topToBottom.compute(s0, s2, gradients, height) < 1) return
        if (topToMiddle.compute(s0, s1, gradients, height) > 0) shader(left1, right1, topToMiddle.height, width)
        if (midToBottom.compute(s1, s2, gradients, height) > 0) shader(left2, right2, midToBottom.height, width)
    }

    private fun shade(pLeft: Edge, pRight: Edge, height: Int, screenWidth: Int) {
        var offset = pLeft.y * screenWidth
        for (it in 0 until height) {
            val xStart = max(0, ceil(pLeft.x))
            var width = min(screenWidth, ceil(pRight.x)) - xStart
            var mem = offset + xStart
            val xPreStep = xStart - pLeft.x
            var z = fma(xPreStep, gradients.zOverZdX, pLeft.z)
            var oneOverZ = fma(xPreStep, gradients.oneOverZdX, pLeft.oneOverZ)
            var uOverZ = fma(xPreStep, gradients.uOverZdX, pLeft.uOverZ)
            var vOverZ = fma(xPreStep, gradients.vOverZdX, pLeft.vOverZ)
            var lOverZ = fma(xPreStep, gradients.lOverZdX, pLeft.lOverZ)
            while (width-- > 0) {
                if (z < depthBuffer[mem]) {
                    depthBuffer[mem] = z
                    val w = 1F / oneOverZ
                    val alphaChannel = (255 * lOverZ * w).toInt() shl 24
                    val colorChannel = material.sample(uOverZ * w, vOverZ * w) and 0xFFFFFF
                    this.colorBuffer[mem] = alphaChannel or colorChannel
                }
                z += gradients.zOverZdX
                oneOverZ += gradients.oneOverZdX
                uOverZ += gradients.uOverZdX
                vOverZ += gradients.vOverZdX
                lOverZ += gradients.lOverZdX
                ++mem
            }
            pRight.x += pRight.xStep
            pLeft.x += pLeft.xStep
            pLeft.z += pLeft.zStep
            pLeft.oneOverZ += pLeft.oneOverZStep
            pLeft.uOverZ += pLeft.uOverZStep
            pLeft.vOverZ += pLeft.vOverZStep
            pLeft.lOverZ += pLeft.lOverZStep
            offset += screenWidth
        }
        pLeft.y += height
    }

    private fun noShade(pLeft: Edge, pRight: Edge, height: Int, screenWidth: Int) {
        var offset = pLeft.y * screenWidth
        for (it in 0 until height) {
            val xStart = max(0, ceil(pLeft.x))
            var width = min(screenWidth, ceil(pRight.x)) - xStart
            var mem = offset + xStart
            var z = fma(xStart - pLeft.x, gradients.zOverZdX, pLeft.z)
            while (width-- > 0) {
                if (z < depthBuffer[mem]) depthBuffer[mem] = z
                z += gradients.zOverZdX
                ++mem
            }
            pRight.x += pRight.xStep
            pLeft.x += pLeft.xStep
            pLeft.z += pLeft.zStep
            offset += screenWidth
        }
        pLeft.y += height
    }

    private companion object {
        fun isBackFacing(a: Vertex, b: Vertex, c: Vertex): Boolean {
            val bw = 1F / b.w
            val cw = 1F / c.w
            val ay = a.y / a.w
            val ax = a.x / a.w
            return (ay - b.y * bw) * (c.x * cw - ax) <= (ay - c.y * cw) * (b.x * bw - ax)
        }
    }
}
