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
import com.zynaps.tools.ObjectPool

@Suppress("SpellCheckingInspection")
internal class Epa {
    private val mkvPool = ObjectPool { Mkv() }
    private val facePool = ObjectPool { Face() }
    private var root: Face? = null
    private var faceCount = 0
    private var depth = 0F
    private val baseMkv = Array(4) { Mkv() }
    private val baseFaces = Array(6) { Face() }
    private var pfIdxPtr = emptyArray<IntArray>()
    private var peIdxPtr = emptyArray<IntArray>()
    private val cf = arrayOf<Face?>(null)
    private val ff = arrayOf<Face?>(null)

    val nearest = arrayOf(Vector3.ZERO, Vector3.ZERO)
    var normal = Vector3.ZERO
    var iterations = 0
    var failed = false

    fun evaluate(gjk: Gjk, accuracy: Float = EPA_ACCURACY): Float {
        if (gjk.order !in 3..4) return depth
        mkvPool.reset()
        facePool.reset()

        root = null
        depth = -INF
        normal = Vector3.ZERO
        failed = false
        faceCount = 0
        iterations = 0

        val nfIdx: Int
        val neIdx: Int
        if (gjk.order == 3) {
            pfIdxPtr = TETRAHEDRON_FIDX
            peIdxPtr = TETRAHEDRON_EIDX
            nfIdx = 4
            neIdx = 6
        } else {
            pfIdxPtr = HEXAHEDRON_FIDX
            peIdxPtr = HEXAHEDRON_EIDX
            nfIdx = 6
            neIdx = 9
        }

        (0..gjk.order).forEach { baseMkv[it].set(gjk.simplex[it]) }

        repeat((0 until nfIdx).count()) {
            newFace(baseFaces[it], baseMkv[pfIdxPtr[it][0]], baseMkv[pfIdxPtr[it][1]], baseMkv[pfIdxPtr[it][2]])
        }

        repeat((0 until neIdx).count()) {
            link(baseFaces[peIdxPtr[it][0]], peIdxPtr[it][1], baseFaces[peIdxPtr[it][2]], peIdxPtr[it][3])
        }

        if (faceCount == 0) return depth

        var markId = 1
        var bestFace: Face? = null
        while (iterations++ < EPA_MAX_ITERATIONS) {
            val bf = findBest(root) ?: break
            val mkv = mkvPool.next()
            mkv.w = gjk.support(-bf.n, mkv)
            bestFace = bf
            if (Vector3.dot(bf.n, mkv.w) + bf.d >= -accuracy) break
            detach(bf)
            bf.mark = ++markId
            cf[0] = null
            ff[0] = null
            if (buildHorizon(markId, mkv, bf.f[0]!!, bf.e[0], cf, ff) +
                buildHorizon(markId, mkv, bf.f[1]!!, bf.e[1], cf, ff) +
                buildHorizon(markId, mkv, bf.f[2]!!, bf.e[2], cf, ff) < 3
            ) break
            link(cf[0]!!, 1, ff[0]!!, 2)
        }

        if (bestFace == null) {
            failed = true
        } else {
            normal = bestFace.n
            depth = bestFace.d.coerceAtLeast(0F)
            val features00 = gjk.localSupport(bestFace.v[0].r, 0)
            val features01 = gjk.localSupport(bestFace.v[1].r, 0)
            val features02 = gjk.localSupport(bestFace.v[2].r, 0)
            val features10 = gjk.localSupport(-bestFace.v[0].r, 1)
            val features11 = gjk.localSupport(-bestFace.v[1].r, 1)
            val features12 = gjk.localSupport(-bestFace.v[2].r, 1)
            val w0 = bestFace.v[0].w + bestFace.n * bestFace.d
            val w1 = bestFace.v[1].w + bestFace.n * bestFace.d
            val w2 = bestFace.v[2].w + bestFace.n * bestFace.d
            val x = Vector3.crossLength(w0, w1)
            val y = Vector3.crossLength(w1, w2)
            val z = Vector3.crossLength(w2, w0)
            val dn = 1F / (x + y + z).coerceAtLeast(Globals.TINY)
            nearest[0] = features00 * (y * dn) + features01 * (z * dn) + features02 * (x * dn)
            nearest[1] = features10 * (y * dn) + features11 * (z * dn) + features12 * (x * dn)
        }
        return depth
    }

    private fun newFace(pf: Face, a: Mkv, b: Mkv, c: Mkv): Face {
        if (pf.set(a, b, c, EPA_IN_FACE_EPS)) {
            if (root != null) root!!.prev = pf
            pf.prev = null
            pf.next = root
            root = pf
            faceCount++
        } else {
            pf.next = null
            pf.prev = pf.next
        }
        return pf
    }

    private fun detach(face: Face) {
        if (face.prev == null && face.next == null) return
        faceCount--
        when (face) {
            root -> {
                root = face.next
                root?.prev = null
            }
            else -> when (face.next) {
                null -> face.prev?.next = null
                else -> {
                    face.prev?.next = face.next
                    face.next?.prev = face.prev
                }
            }
        }
        face.next = null
        face.prev = face.next
    }

    private fun buildHorizon(markId: Int, w: Mkv, f: Face, e: Int, cf: Array<Face?>, ff: Array<Face?>): Int {
        var ne = 0
        if (f.mark != markId) {
            val e1 = MOD3[e + 1]
            if (Vector3.dot(f.n, w.w) + f.d > 0F) {
                val nf = newFace(facePool.next(), f.v[e1], f.v[e], w)
                link(nf, 0, f, e)
                if (cf[0] != null) {
                    link(cf[0]!!, 1, nf, 2)
                } else {
                    ff[0] = nf
                }
                cf[0] = nf
                ne = 1
            } else {
                val e2 = MOD3[e + 2]
                detach(f)
                f.mark = markId
                ne += buildHorizon(markId, w, f.f[e1]!!, f.e[e1], cf, ff)
                ne += buildHorizon(markId, w, f.f[e2]!!, f.e[e2], cf, ff)
            }
        }
        return ne
    }

    private companion object {
        const val INF = Float.MAX_VALUE
        const val EPA_MAX_ITERATIONS = 256
        const val EPA_IN_FACE_EPS = 0.01F
        const val EPA_ACCURACY = 0.001F

        fun findBest(root: Face?): Face? {
            if (root == null) return null
            var cf = root
            var bd = INF
            var bf: Face? = null
            do {
                if (cf!!.d < bd) {
                    bd = cf.d
                    bf = cf
                }
                cf = cf.next
            } while (cf != null)
            return bf
        }

        fun link(f0: Face, e0: Int, f1: Face, e1: Int) {
            f0.f[e0] = f1
            f1.e[e1] = e0
            f1.f[e1] = f0
            f0.e[e0] = e1
        }

        val MOD3 = intArrayOf(0, 1, 2, 0, 1)

        val TETRAHEDRON_FIDX = arrayOf(
            intArrayOf(2, 1, 0),
            intArrayOf(3, 0, 1),
            intArrayOf(3, 1, 2),
            intArrayOf(3, 2, 0)
        )

        val TETRAHEDRON_EIDX = arrayOf(
            intArrayOf(0, 0, 2, 1),
            intArrayOf(0, 1, 1, 1),
            intArrayOf(0, 2, 3, 1),
            intArrayOf(1, 0, 3, 2),
            intArrayOf(2, 0, 1, 2),
            intArrayOf(3, 0, 2, 2)
        )

        val HEXAHEDRON_FIDX = arrayOf(
            intArrayOf(2, 0, 4),
            intArrayOf(4, 1, 2),
            intArrayOf(1, 4, 0),
            intArrayOf(0, 3, 1),
            intArrayOf(0, 2, 3),
            intArrayOf(1, 3, 2)
        )

        val HEXAHEDRON_EIDX = arrayOf(
            intArrayOf(0, 0, 4, 0),
            intArrayOf(0, 1, 2, 1),
            intArrayOf(0, 2, 1, 2),
            intArrayOf(1, 1, 5, 2),
            intArrayOf(1, 0, 2, 0),
            intArrayOf(2, 2, 3, 2),
            intArrayOf(3, 1, 5, 0),
            intArrayOf(3, 0, 4, 2),
            intArrayOf(5, 1, 4, 1)
        )
    }
}
