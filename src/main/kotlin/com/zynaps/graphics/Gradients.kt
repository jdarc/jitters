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
