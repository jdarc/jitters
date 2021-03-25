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

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.geometry.CollisionSkin
import kotlin.math.abs

internal class Gjk {
    private val table = arrayOfNulls<He>(GJK_HASH_SIZE)

    private lateinit var shape0: CollisionSkin
    private lateinit var shape1: CollisionSkin

    private var margin = 0F
    private var ray = Vector3.ZERO

    val simplex = arrayOf(Mkv(), Mkv(), Mkv(), Mkv(), Mkv())
    var order = 0
    var iterations = 0
    var failed = false

    fun init(shape0: CollisionSkin, shape1: CollisionSkin, pMargin: Float) {
        this.shape0 = shape0
        this.shape1 = shape1
        margin = pMargin
        failed = false
    }

    private fun hash(v: Vector3) = ((v.x * 15461).toInt() xor (v.y * 83003).toInt() xor (v.z * 15473).toInt()) * 169639 and GJK_HASH_MASK

    fun localSupport(d: Vector3, i: Int) = when (i) {
        0 -> localSupport(d, shape0)
        else -> localSupport(d, shape1)
    }

    private fun localSupport(d: Vector3, shape: CollisionSkin) = shape.getSupport(d)

    fun support(d: Vector3, v: Mkv): Vector3 {
        v.r = d
        return d * margin + (localSupport(d, shape0) - localSupport(-d, shape1))
    }

    private fun fetchSupport(): Boolean {
        val h = hash(ray)
        var e = table[h]
        while (e != null) {
            e = if (e.v == ray) {
                --order
                return false
            } else e.n
        }
        e = He()
        e.v = ray
        e.n = table[h]
        table[h] = e
        order++
        simplex[order].w = support(ray, simplex[order])
        return Vector3.dot(ray, simplex[order].w) > 0F
    }

    private fun solveSimplex2(ao: Vector3, ab: Vector3): Boolean {
        when {
            Vector3.dot(ab, ao) >= 0F -> {
                val cabo = Vector3.cross(ab, ao)
                ray = when {
                    cabo.lengthSquared() > GJK_SQ_IN_SIMPLEX_EPS -> Vector3.cross(cabo, ab)
                    else -> return true
                }
            }
            else -> {
                order = 0
                simplex[0].set(simplex[1])
                ray = ao
            }
        }
        return false
    }

    private fun solveSimplex3(ao: Vector3, ab: Vector3, ac: Vector3) = solveSimplex3a(ao, ab, ac, Vector3.cross(ab, ac))

    private fun solveSimplex3a(ao: Vector3, ab: Vector3, ac: Vector3, cabc: Vector3): Boolean {
        val tmp = Vector3.cross(cabc, ab)
        val tmp2 = Vector3.cross(cabc, ac)
        return if (Vector3.dot(tmp, ao) < -GJK_IN_SIMPLEX_EPS) {
            order = 1
            simplex[0].set(simplex[1])
            simplex[1].set(simplex[2])
            solveSimplex2(ao, ab)
        } else if (Vector3.dot(tmp2, ao) > +GJK_IN_SIMPLEX_EPS) {
            order = 1
            simplex[1].set(simplex[2])
            solveSimplex2(ao, ac)
        } else {
            val d = Vector3.dot(cabc, ao)
            if (abs(d) > GJK_IN_SIMPLEX_EPS) {
                if (d > 0) {
                    ray = cabc
                } else {
                    ray = -cabc
                    val swapTmp = Mkv()
                    swapTmp.set(simplex[0])
                    simplex[0].set(simplex[1])
                    simplex[1].set(swapTmp)
                }
                false
            } else {
                true
            }
        }
    }

    private fun solveSimplex4(ao: Vector3, ab: Vector3, ac: Vector3, ad: Vector3): Boolean {
        val tmp = Vector3.cross(ab, ac)
        val tmp2 = Vector3.cross(ac, ad)
        val tmp3 = Vector3.cross(ad, ab)
        return when {
            Vector3.dot(tmp, ao) > GJK_IN_SIMPLEX_EPS -> {
                order = 2
                simplex[0].set(simplex[1])
                simplex[1].set(simplex[2])
                simplex[2].set(simplex[3])
                solveSimplex3a(ao, ab, ac, tmp)
            }
            Vector3.dot(tmp2, ao) > GJK_IN_SIMPLEX_EPS -> {
                order = 2
                simplex[2].set(simplex[3])
                solveSimplex3a(ao, ac, ad, tmp2)
            }
            Vector3.dot(tmp3, ao) > GJK_IN_SIMPLEX_EPS -> {
                order = 2
                simplex[1].set(simplex[0])
                simplex[0].set(simplex[2])
                simplex[2].set(simplex[3])
                solveSimplex3a(ao, ad, ab, tmp3)
            }
            else -> true
        }
    }

    fun searchOrigin(initRay: Vector3 = Vector3.UNIT_X): Boolean {
        iterations = 0
        order = -1
        failed = false
        ray = Vector3.normalize(initRay)
        table.fill(null)
        fetchSupport()
        ray = -simplex[0].w
        while (iterations < GJK_MAX_ITERATIONS) {
            val rl = ray.length()
            ray *= 1F / if (rl > 0F) rl else 1F
            if (fetchSupport()) {
                var found = false
                when (order) {
                    1 -> found = solveSimplex2(-simplex[1].w, simplex[0].w - simplex[1].w)
                    2 -> found = solveSimplex3(-simplex[2].w, simplex[1].w - simplex[2].w, simplex[0].w - simplex[2].w)
                    3 -> found = solveSimplex4(
                        -simplex[3].w,
                        simplex[2].w - simplex[3].w,
                        simplex[1].w - simplex[3].w,
                        simplex[0].w - simplex[3].w
                    )
                }
                if (found) return true
            } else return false
            ++iterations
        }
        failed = true
        return false
    }

    fun encloseOrigin() = when (order) {
        0 -> false
        1 -> {
            val ab = simplex[1].w - simplex[0].w
            val b0 = Vector3.cross(ab, Vector3.UNIT_X)
            val b1 = Vector3.cross(ab, Vector3.UNIT_Y)
            val b2 = Vector3.cross(ab, Vector3.UNIT_Z)
            val m0 = b0.lengthSquared()
            val m1 = b1.lengthSquared()
            val m2 = b2.lengthSquared()
            val r = Matrix4.createFromAxisAngle(Vector3.normalize(ab), TAU / 3F)
            val w = if (m0 > m1) if (m0 > m2) b0 else b2 else if (m1 > m2) b1 else b2
            val tmp = Vector3.normalize(r * w)
            simplex[4].w = support(Vector3.normalize(w), simplex[4])
            simplex[2].w = support(tmp, simplex[2])
            simplex[3].w = support(tmp, simplex[3])
            order = 4
            true
        }
        2 -> {
            val n = Vector3.normalize(Vector3.cross(simplex[1].w - simplex[0].w, simplex[2].w - simplex[0].w))
            simplex[3].w = support(n, simplex[3])
            simplex[4].w = support(-n, simplex[4])
            order = 4
            true
        }
        3, 4 -> true
        else -> false
    }

    companion object {
        private const val PI = kotlin.math.PI.toFloat()
        private const val TAU = PI * 2F
        private const val GJK_MAX_ITERATIONS = 128
        private const val GJK_HASH_SIZE = 64
        private const val GJK_HASH_MASK = GJK_HASH_SIZE - 1
        private const val GJK_IN_SIMPLEX_EPS = 0.0001F
        private const val GJK_SQ_IN_SIMPLEX_EPS = GJK_IN_SIMPLEX_EPS * GJK_IN_SIMPLEX_EPS
    }
}
