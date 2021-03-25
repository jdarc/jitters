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

package com.zynaps.physics.collision.narrowphase

import com.zynaps.math.Vector3
import kotlin.math.max

internal class Face {
    val v = Array(3) { Mkv() }
    val f = arrayOfNulls<Face>(3)
    val e = IntArray(3)
    var n = Vector3.ZERO
    var d = 0F
    var mark = 0
    var prev: Face? = null
    var next: Face? = null

    fun set(a: Mkv, b: Mkv, c: Mkv, epaInFaceEps: Float): Boolean {
        var tmp1 = b.w - a.w
        var tmp2 = c.w - a.w
        val nrm = Vector3.cross(tmp1, tmp2)
        val len = nrm.length()
        tmp1 = Vector3.cross(a.w, b.w)
        tmp2 = Vector3.cross(b.w, c.w)
        val tmp3 = Vector3.cross(c.w, a.w)
        v[0] = a
        v[1] = b
        v[2] = c
        mark = 0
        n = nrm * (1F / if (len > 0F) len else Float.MAX_VALUE)
        d = max(0F, -Vector3.dot(n, a.w))
        return Vector3.dot(tmp1, nrm) >= -epaInFaceEps && Vector3.dot(tmp2, nrm) >= -epaInFaceEps && Vector3.dot(tmp3, nrm) >= -epaInFaceEps
    }
}
