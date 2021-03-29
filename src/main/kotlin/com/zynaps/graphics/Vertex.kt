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

import java.lang.Math.fma

internal class Vertex {
    var x = 0F
    var y = 0F
    var z = 0F
    var w = 0F
    var u = 0F
    var v = 0F
    var l = 0F

    fun set(other: Vertex): Vertex {
        x = other.x
        y = other.y
        z = other.z
        w = other.w
        u = other.u
        v = other.v
        l = other.l
        return this
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
