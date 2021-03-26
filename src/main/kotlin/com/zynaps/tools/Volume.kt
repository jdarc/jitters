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

package com.zynaps.tools

// https://geometrictools.com/Documentation/PolyhedralMassProperties.pdf
object Volume {

//    val vertices = doubleArrayOf(-1.0, -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0, -1.0, -1.0, 1.0, -1.0, -1.0, 1.0, 1.0, -1.0, -1.0, 1.0, -1.0)
//    val tris = intArrayOf(0, 1, 2, 2, 3, 0, 5, 4, 7, 7, 6, 5, 4, 5, 1, 1, 0, 4, 3, 2, 6, 6, 7, 3, 1, 5, 6, 6, 2, 1, 0, 3, 7, 7, 4, 0)
//    val cm = DoubleArray(3)
//    val inertia = DoubleArray(9)
//    val mass = Volume.compute(vertices, tris, cm, inertia)
//    println(mass)
//    println(cm.contentToString())
//    println(inertia.contentToString())

    fun compute(points: DoubleArray, triangles: IntArray, cm: DoubleArray, inertia: DoubleArray): Double {
        val integral = DoubleArray(10) { 0.0 }
        val f = DoubleArray(3)
        val g = DoubleArray(3)

        for (i in triangles.indices step 3) {
            // get vertices of triangle
            val i0 = triangles[i + 0] * 3
            val i1 = triangles[i + 1] * 3
            val i2 = triangles[i + 2] * 3
            val p0x = points[i0 + 0]
            val p0y = points[i0 + 1]
            val p0z = points[i0 + 2]
            val p1x = points[i1 + 0]
            val p1y = points[i1 + 1]
            val p1z = points[i1 + 2]
            val p2x = points[i2 + 0]
            val p2y = points[i2 + 1]
            val p2z = points[i2 + 2]

            // get edges and cross product of edges
            val ax = p1x - p0x
            val ay = p1y - p0y
            val az = p1z - p0z
            val bx = p2x - p0x
            val by = p2y - p0y
            val bz = p2z - p0z
            val cx = ay * bz - by * az
            val cy = bx * az - ax * bz
            val cz = ax * by - bx * ay

            // compute integral terms
            subexpressions(p0x, p1x, p2x, f, g)
            val f1x = f[0]
            val f2x = f[1]
            val f3x = f[2]
            val g0x = g[0]
            val g1x = g[1]
            val g2x = g[2]
            subexpressions(p0y, p1y, p2y, f, g)
            val f1y = f[0]
            val f2y = f[1]
            val f3y = f[2]
            val g0y = g[0]
            val g1y = g[1]
            val g2y = g[2]
            subexpressions(p0z, p1z, p2z, f, g)
            val f1z = f[0]
            val f2z = f[1]
            val f3z = f[2]
            val g0z = g[0]
            val g1z = g[1]
            val g2z = g[2]

            // update integrals
            integral[0] += cx * f1x
            integral[1] += cx * f2x
            integral[2] += cy * f2y
            integral[3] += cz * f2z
            integral[4] += cx * f3x
            integral[5] += cy * f3y
            integral[6] += cz * f3z
            integral[7] += cx * (p0y * g0x + p1y * g1x + p2y * g2x)
            integral[8] += cy * (p0z * g0y + p1z * g1y + p2z * g2y)
            integral[9] += cz * (p0x * g0z + p1x * g1z + p2x * g2z)
        }

        for (i in integral.indices) integral[i] *= MULT[i]

        val mass = integral[0]

        // center of mass
        cm[0] = integral[1] / mass
        cm[1] = integral[2] / mass
        cm[2] = integral[3] / mass

        // inertia tensor relative to center of mass
        val xy = -(integral[7] - mass * cm[0] * cm[1])
        val yz = -(integral[8] - mass * cm[1] * cm[2])
        val xz = -(integral[9] - mass * cm[2] * cm[0])
        inertia[0] = integral[5] + integral[6] - mass * (cm[1] * cm[1] + cm[2] * cm[2])
        inertia[1] = xy
        inertia[2] = xz
        inertia[3] = xy
        inertia[4] = integral[4] + integral[6] - mass * (cm[2] * cm[2] + cm[0] * cm[0])
        inertia[5] = yz
        inertia[6] = xz
        inertia[7] = yz
        inertia[8] = integral[4] + integral[5] - mass * (cm[0] * cm[0] + cm[1] * cm[1])

        return mass
    }

    private fun subexpressions(w0: Double, w1: Double, w2: Double, f: DoubleArray, g: DoubleArray) {
        val temp0 = w0 + w1
        val temp1 = w0 * w0
        val temp2 = temp1 + w1 * temp0
        f[0] = temp0 + w2
        f[1] = temp2 + w2 * f[0]
        f[2] = w0 * temp1 + w1 * temp2 + w2 * f[1]
        g[0] = f[1] + w0 * (f[0] + w0)
        g[1] = f[1] + w1 * (f[0] + w1)
        g[2] = f[1] + w2 * (f[0] + w2)
    }

    private val MULT = doubleArrayOf(1 / 6.0, 1 / 24.0, 1 / 24.0, 1 / 24.0, 1 / 60.0, 1 / 60.0, 1 / 60.0, 1 / 120.0, 1 / 120.0, 1 / 120.0)
}

