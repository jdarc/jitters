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
import com.zynaps.physics.Globals

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
        v[0] = a
        v[1] = b
        v[2] = c
        mark = 0
        val nrm = Vector3.cross(b.w - a.w, c.w - a.w)
        n = Vector3.normalize(nrm)
        d = (-Vector3.dot(n, a.w)).coerceIn(Globals.TINY, Globals.HUGE)
        return Vector3.crossDot(a.w, b.w, nrm) >= -epaInFaceEps &&
               Vector3.crossDot(b.w, c.w, nrm) >= -epaInFaceEps &&
               Vector3.crossDot(c.w, a.w, nrm) >= -epaInFaceEps
    }
}
