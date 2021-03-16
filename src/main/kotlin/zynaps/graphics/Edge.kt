package zynaps.graphics

import zynaps.math.Scalar.ceil

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

        z = a.z + yPrestep * gradients.zOverZdY + xPrestep * gradients.zOverZdX
        zStep = xStep * gradients.zOverZdX + gradients.zOverZdY

        oneOverZ = a.w + yPrestep * gradients.oneOverZdY + xPrestep * gradients.oneOverZdX
        oneOverZStep = xStep * gradients.oneOverZdX + gradients.oneOverZdY

        uOverZ = a.u + yPrestep * gradients.uOverZdY + xPrestep * gradients.uOverZdX
        uOverZStep = xStep * gradients.uOverZdX + gradients.uOverZdY

        vOverZ = a.v + yPrestep * gradients.vOverZdY + xPrestep * gradients.vOverZdX
        vOverZStep = xStep * gradients.vOverZdX + gradients.vOverZdY

        lOverZ = a.l + yPrestep * gradients.lOverZdY + xPrestep * gradients.lOverZdX
        lOverZStep = xStep * gradients.lOverZdX + gradients.lOverZdY

        return this.height
    }
}
