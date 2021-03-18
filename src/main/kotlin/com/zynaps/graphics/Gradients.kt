package com.zynaps.graphics

internal class Gradients {
    var zOverZdX = 0F
    var zOverZdY = 0F
    var oneOverZdX = 0F
    var oneOverZdY = 0F
    var uOverZdX = 0F
    var uOverZdY = 0F
    var vOverZdX = 0F
    var vOverZdY = 0F
    var lOverZdX = 0F
    var lOverZdY = 0F

    fun computeGradients(s0: Vertex, s1: Vertex, s2: Vertex) {
        val acx = s0.x - s2.x
        val bcx = s1.x - s2.x
        val acy = s0.y - s2.y
        val bcy = s1.y - s2.y

        val oneOverDX = 1F / (bcx * acy - acx * bcy)

        var tmp02 = s0.z - s2.z
        var tmp12 = s1.z - s2.z
        zOverZdX = oneOverDX * (tmp12 * acy - tmp02 * bcy)
        zOverZdY = oneOverDX * (tmp02 * bcx - tmp12 * acx)

        tmp02 = s0.w - s2.w
        tmp12 = s1.w - s2.w
        oneOverZdX = oneOverDX * (tmp12 * acy - tmp02 * bcy)
        oneOverZdY = oneOverDX * (tmp02 * bcx - tmp12 * acx)

        tmp02 = s0.u - s2.u
        tmp12 = s1.u - s2.u
        uOverZdX = oneOverDX * (tmp12 * acy - tmp02 * bcy)
        uOverZdY = oneOverDX * (tmp02 * bcx - tmp12 * acx)

        tmp02 = s0.v - s2.v
        tmp12 = s1.v - s2.v
        vOverZdX = oneOverDX * (tmp12 * acy - tmp02 * bcy)
        vOverZdY = oneOverDX * (tmp02 * bcx - tmp12 * acx)

        tmp02 = s0.l - s2.l
        tmp12 = s1.l - s2.l
        lOverZdX = oneOverDX * (tmp12 * acy - tmp02 * bcy)
        lOverZdY = oneOverDX * (tmp02 * bcx - tmp12 * acx)
    }
}