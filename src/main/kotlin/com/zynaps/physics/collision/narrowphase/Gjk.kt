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
import com.zynaps.physics.geometry.CollisionSkin
import com.zynaps.tools.ObjectPool
import kotlin.math.abs

internal class Gjk {
    private val hePool = ObjectPool { He() }
    private val table = arrayOfNulls<He>(GJK_HASH_SIZE)

    private lateinit var shape0: CollisionSkin
    private lateinit var shape1: CollisionSkin

    private var margin = 0F
    private var ray = Vector3.ZERO

    val simplex = arrayOf(Mkv(), Mkv(), Mkv(), Mkv(), Mkv())
    var order = 0
    var iterations = 0
    var failed = false

    fun init(shape0: CollisionSkin, shape1: CollisionSkin, margin: Float): Gjk {
        this.shape0 = shape0
        this.shape1 = shape1
        this.margin = margin.coerceAtLeast(Globals.TINY)
        failed = false
        iterations = 0
        order = -1
        hePool.reset()
        return this
    }

    fun searchOrigin(): Boolean {
        table.fill(null)
        ray = Vector3.UNIT_X
        fetchSupport()
        ray = -simplex[0].w
        while (iterations++ < GJK_MAX_ITERATIONS) {
            ray = Vector3.normalize(ray)
            if (fetchSupport()) {
                if (when (order) {
                        1 -> solveSimplex2(-simplex[1].w, simplex[0].w - simplex[1].w)
                        2 -> solveSimplex3(-simplex[2].w, simplex[1].w - simplex[2].w, simplex[0].w - simplex[2].w)
                        3 -> solveSimplex4(
                            -simplex[3].w,
                            simplex[2].w - simplex[3].w,
                            simplex[1].w - simplex[3].w,
                            simplex[0].w - simplex[3].w
                        )
                        else -> false
                    }
                ) return true
            } else {
                return false
            }
        }
        failed = true
        return false
    }

    internal fun support(d: Vector3, v: Mkv): Vector3 {
        v.r = d
        return d * margin + (shape0.getSupport(d) - shape1.getSupport(-d))
    }

    internal fun localSupport(d: Vector3, i: Int) = when (i) {
        0 -> shape0.getSupport(d)
        else -> shape1.getSupport(d)
    }

    private fun fetchSupport(): Boolean {
        val h = hash(ray)
        var e = table[h]
        while (e != null) {
            e = if (e.v == ray) {
                order--
                return false
            } else e.n
        }
        e = hePool.next()
        e.v = ray
        e.n = table[h]
        table[h] = e
        simplex[++order].w = support(ray, simplex[order])
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
        if (Vector3.crossDot(cabc, ab, ao) < -GJK_IN_SIMPLEX_EPS) {
            order = 1
            simplex[0].set(simplex[1])
            simplex[1].set(simplex[2])
            return solveSimplex2(ao, ab)
        }

        if (Vector3.crossDot(cabc, ac, ao) > GJK_IN_SIMPLEX_EPS) {
            order = 1
            simplex[1].set(simplex[2])
            return solveSimplex2(ao, ac)
        }

        val d = Vector3.dot(cabc, ao)
        if (abs(d) > GJK_IN_SIMPLEX_EPS) {
            if (d > 0F) {
                ray = cabc
            } else {
                ray = -cabc
                val w = simplex[0].w
                val r = simplex[0].r
                simplex[0].set(simplex[1])
                simplex[1].set(w, r)
            }
            return false
        }

        return true
    }

    private fun solveSimplex4(ao: Vector3, ab: Vector3, ac: Vector3, ad: Vector3): Boolean {
        val tmp1 = Vector3.cross(ab, ac)
        if (Vector3.dot(tmp1, ao) > GJK_IN_SIMPLEX_EPS) {
            order = 2
            simplex[0].set(simplex[1])
            simplex[1].set(simplex[2])
            simplex[2].set(simplex[3])
            return solveSimplex3a(ao, ab, ac, tmp1)
        }

        val tmp2 = Vector3.cross(ac, ad)
        if (Vector3.dot(tmp2, ao) > GJK_IN_SIMPLEX_EPS) {
            order = 2
            simplex[2].set(simplex[3])
            return solveSimplex3a(ao, ac, ad, tmp2)
        }

        val tmp3 = Vector3.cross(ad, ab)
        if (Vector3.dot(tmp3, ao) > GJK_IN_SIMPLEX_EPS) {
            order = 2
            simplex[1].set(simplex[0])
            simplex[0].set(simplex[2])
            simplex[2].set(simplex[3])
            return solveSimplex3a(ao, ad, ab, tmp3)
        }

        return true
    }

    private companion object {
        const val GJK_MAX_ITERATIONS = 128
        const val GJK_HASH_SIZE = 64
        const val GJK_HASH_MASK = GJK_HASH_SIZE - 1
        const val GJK_IN_SIMPLEX_EPS = 0.0001F
        const val GJK_SQ_IN_SIMPLEX_EPS = GJK_IN_SIMPLEX_EPS * GJK_IN_SIMPLEX_EPS

        fun hash(v: Vector3): Int {
            val hx = (v.x * 15461).toInt()
            val hy = (v.y * 83003).toInt()
            val hz = (v.z * 15473).toInt()
            return GJK_HASH_MASK and (hx xor hy xor hz) * 169639
        }
    }
}
