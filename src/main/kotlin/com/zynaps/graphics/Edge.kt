package com.zynaps.graphics

import com.zynaps.math.Scalar.ceil
import java.lang.Math.fma

internal class Edge {
    var y = 0
    var height = 0
    var x = 0F
    var xStep = 0F
    var z = 0F
    var zStep = 0F
    var oneOverZ = 0F
    var oneOverZStep = 0F
    var uOverZ = 0F
    var uOverZStep = 0F
    var vOverZ = 0F
    var vOverZStep = 0F
    var lOverZ = 0F
    var lOverZStep = 0F

    fun compute(a: Vertex, b: Vertex, gradients: Gradients, height: Int): Int {
        y = ceil(a.y)
        if (y < 0) y = 0

        this.height = ceil(b.y)
        if (this.height > height) this.height = height

        this.height -= y
        if (this.height < 1) return 0

        val yPrestep = y - a.y
        xStep = (b.x - a.x) / (b.y - a.y)
        x = yPrestep * xStep + a.x
        val xPrestep = x - a.x

        z = fma(yPrestep, gradients.zOverZdY, fma(xPrestep, gradients.zOverZdX, a.z))
        zStep = fma(xStep, gradients.zOverZdX, gradients.zOverZdY)

        oneOverZ = fma(yPrestep, gradients.oneOverZdY, fma(xPrestep, gradients.oneOverZdX, a.w))
        oneOverZStep = fma(xStep, gradients.oneOverZdX, gradients.oneOverZdY)

        uOverZ = fma(yPrestep, gradients.uOverZdY, fma(xPrestep, gradients.uOverZdX, a.u))
        uOverZStep = fma(xStep, gradients.uOverZdX, gradients.uOverZdY)

        vOverZ = fma(yPrestep, gradients.vOverZdY, fma(xPrestep, gradients.vOverZdX, a.v))
        vOverZStep = fma(xStep, gradients.vOverZdX, gradients.vOverZdY)

        lOverZ = fma(yPrestep, gradients.lOverZdY, fma(xPrestep, gradients.lOverZdX, a.l))
        lOverZStep = fma(xStep, gradients.lOverZdX, gradients.lOverZdY)

        return this.height
    }
}
