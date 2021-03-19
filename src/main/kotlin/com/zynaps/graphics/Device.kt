package com.zynaps.graphics

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import java.lang.Math.fma
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class Device(private val colorBuffer: IntArray, private val depthBuffer: FloatArray, private val width: Int, private val height: Int) {
    private var lightDir = Vector3.ZERO
    private val clipper = Clipper()
    private val gradients = Gradients()
    private val topToBottom = Edge()
    private val topToMiddle = Edge()
    private val midToBottom = Edge()
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

        val a = Vertex()
        val b = Vertex()
        val c = Vertex()
        for (i in 0 until elementCount step 3) {
            a.transfer(indexBuffer[i + 0] shl 3, vertexBuffer, transform, normalMatrix, lightDir)
            b.transfer(indexBuffer[i + 1] shl 3, vertexBuffer, transform, normalMatrix, lightDir)
            c.transfer(indexBuffer[i + 2] shl 3, vertexBuffer, transform, normalMatrix, lightDir)
            renderFn(a, b, c)
        }
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

    private fun scanOrder(a: Vertex, b: Vertex, c: Vertex) = if (a.y < b.y) {
        when {
            c.y < a.y -> scanConvert(c, a, b, topToMiddle, topToBottom, midToBottom, topToBottom)
            b.y < c.y -> scanConvert(a, b, c, topToMiddle, topToBottom, midToBottom, topToBottom)
            else -> scanConvert(a, c, b, topToBottom, topToMiddle, topToBottom, midToBottom)
        }
    } else {
        when {
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
            val xStart = max(0, ceil(pLeft.x).toInt())
            var width = min(screenWidth, ceil(pRight.x).toInt()) - xStart
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
            val xStart = max(0, ceil(pLeft.x).toInt())
            var width = min(screenWidth, ceil(pRight.x).toInt()) - xStart
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
            val bvw = 1F / b.w
            val cvw = 1F / c.w
            val avy = a.y / a.w
            val avx = a.x / a.w
            return (avy - b.y * bvw) * (c.x * cvw - avx) <= (avy - c.y * cvw) * (b.x * bvw - avx)
        }
    }
}
