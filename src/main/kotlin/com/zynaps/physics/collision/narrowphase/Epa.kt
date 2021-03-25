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

@Suppress("SpellCheckingInspection")
internal class Epa {
    private var root: Face? = null
    private val features = Array(2) { Array(3) { Vector3.ZERO } }
    private var faceCount = 0
    private var depth = 0F
    private val baseMkv = Array(5) { Mkv() }
    private val baseFaces = Array(6) { Face() }
    private var pfIdxPtr = emptyArray<IntArray>()
    private var peIdxPtr = emptyArray<IntArray>()

    val nearest = arrayOf(Vector3.ZERO, Vector3.ZERO)
    var normal = Vector3.ZERO
    var iterations = 0
    var failed = false

    fun evaluatePD(gjk: Gjk, accuracy: Float = EPA_ACCURACY): Float {
        var bestFace: Face? = null
        var markId = 1
        depth = -INF
        normal = Vector3.ZERO
        root = null
        faceCount = 0
        iterations = 0
        failed = false
        if (gjk.encloseOrigin()) {
            var pfIdxIndex = 0
            var peIdxIndex = 0
            var nfIdx = 0
            var neIdx = 0
            when (gjk.order) {
                3 -> {
                    pfIdxPtr = TETRAHEDRON_FIDX
                    peIdxPtr = TETRAHEDRON_EIDX
                    nfIdx = 4
                    neIdx = 6
                }
                4 -> {
                    pfIdxPtr = HEXAHEDRON_FIDX
                    peIdxPtr = HEXAHEDRON_EIDX
                    nfIdx = 6
                    neIdx = 9
                }
            }
            (0..gjk.order).forEach { baseMkv[it].set(gjk.simplex[it]) }
            (0 until nfIdx).forEach {
                newFace(baseFaces[it], baseMkv[pfIdxPtr[pfIdxIndex][0]], baseMkv[pfIdxPtr[pfIdxIndex][1]], baseMkv[pfIdxPtr[pfIdxIndex][2]])
                ++pfIdxIndex
            }
            repeat((0 until neIdx).count()) {
                link(baseFaces[peIdxPtr[peIdxIndex][0]], peIdxPtr[peIdxIndex][1], baseFaces[peIdxPtr[peIdxIndex][2]], peIdxPtr[peIdxIndex][3])
                ++peIdxIndex
            }
        }

        if (0 == faceCount) return depth

        while (iterations < EPA_MAX_ITERATIONS) {
            val bf = findBest() ?: break
            val tmp = -bf.n
            val v = Mkv()
            v.w = gjk.support(tmp, v)
            val d = Vector3.dot(bf.n, v.w) + bf.d
            bestFace = bf
            if (d >= -accuracy) break
            val cf = arrayOf<Face?>(null)
            val ff = arrayOf<Face?>(null)
            var nf = 0
            detach(bf)
            bf.mark = ++markId
            for (i in 0..2) nf += buildHorizon(markId, v, bf.f[i]!!, bf.e[i], cf, ff)
            if (nf <= 2) break
            link(cf[0]!!, 1, ff[0]!!, 2)
            ++iterations
        }

        if (bestFace != null) {
            val b = getCoordinates(bestFace)
            normal = bestFace.n
            depth = max(0F, bestFace.d)
            for (i in 0..1) {
                val s = if (i != 0) -1F else 1F
                for (j in 0..2) {
                    val tmp = bestFace.v[j].r * s
                    features[i][j] = gjk.localSupport(tmp, i)
                }
            }
            var tmp1 = features[0][0] * b.x
            var tmp2 = features[0][1] * b.y
            var tmp3 = features[0][2] * b.z
            nearest[0] = Vector3(tmp1.x + tmp2.x + tmp3.x, tmp1.y + tmp2.y + tmp3.y, tmp1.z + tmp2.z + tmp3.z)
            tmp1 = features[1][0] * b.x
            tmp2 = features[1][1] * b.y
            tmp3 = features[1][2] * b.z
            nearest[1] = Vector3(tmp1.x + tmp2.x + tmp3.x, tmp1.y + tmp2.y + tmp3.y, tmp1.z + tmp2.z + tmp3.z)
        } else {
            failed = true
        }
        return depth
    }

    private fun getCoordinates(face: Face): Vector3 {
        var tmp: Vector3
        val o = face.n * -face.d
        val a = FloatArray(3)
        var tmp1 = face.v[0].w - o
        var tmp2 = face.v[1].w - o
        tmp = Vector3.cross(tmp1, tmp2)
        a[0] = tmp.length()
        tmp1 = face.v[1].w - o
        tmp2 = face.v[2].w - o
        tmp = Vector3.cross(tmp1, tmp2)
        a[1] = tmp.length()
        tmp1 = face.v[2].w - o
        tmp2 = face.v[0].w - o
        tmp = Vector3.cross(tmp1, tmp2)
        a[2] = tmp.length()
        val sm = a[0] + a[1] + a[2]
        return Vector3(a[1], a[2], a[0]) * (1F / if (sm > 0F) sm else 1F)
    }

    private fun findBest(): Face? {
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


    private fun newFace(pf: Face, a: Mkv, b: Mkv, c: Mkv): Face {
        if (pf.set(a, b, c, EPA_IN_FACE_EPS)) {
            if (root != null) root!!.prev = pf
            pf.prev = null
            pf.next = root
            root = pf
            ++faceCount
        } else {
            pf.next = null
            pf.prev = pf.next
        }
        return pf
    }

    private fun link(f0: Face, e0: Int, f1: Face, e1: Int) {
        f0.f[e0] = f1
        f1.e[e1] = e0
        f1.f[e1] = f0
        f0.e[e0] = e1
    }

    private fun detach(face: Face) {
        if (face.prev != null || face.next != null) {
            --faceCount
            if (face == root) {
                root = face.next
                root?.prev = null
            } else {
                if (face.next == null) {
                    face.prev?.next = null
                } else {
                    face.prev?.next = face.next
                    face.next?.prev = face.prev
                }
            }
            face.next = null
            face.prev = face.next
        }
    }

    private fun buildHorizon(markId: Int, w: Mkv, f: Face, e: Int, cf: Array<Face?>, ff: Array<Face?>): Int {
        var ne = 0
        if (f.mark != markId) {
            val e1 = MOD3[e + 1]
            if (Vector3.dot(f.n, w.w) + f.d > 0F) {
                val nf = newFace(Face(), f.v[e1], f.v[e], w)
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

    companion object {
        private const val INF = Float.MAX_VALUE
        private const val EPA_MAX_ITERATIONS = 256
        private const val EPA_IN_FACE_EPS = 0.01F
        private const val EPA_ACCURACY = 0.001F

        private val MOD3 = intArrayOf(0, 1, 2, 0, 1)

        private val TETRAHEDRON_FIDX = arrayOf(
            intArrayOf(2, 1, 0),
            intArrayOf(3, 0, 1),
            intArrayOf(3, 1, 2),
            intArrayOf(3, 2, 0)
        )

        private val TETRAHEDRON_EIDX = arrayOf(
            intArrayOf(0, 0, 2, 1),
            intArrayOf(0, 1, 1, 1),
            intArrayOf(0, 2, 3, 1),
            intArrayOf(1, 0, 3, 2),
            intArrayOf(2, 0, 1, 2),
            intArrayOf(3, 0, 2, 2)
        )

        private val HEXAHEDRON_FIDX = arrayOf(
            intArrayOf(2, 0, 4),
            intArrayOf(4, 1, 2),
            intArrayOf(1, 4, 0),
            intArrayOf(0, 3, 1),
            intArrayOf(0, 2, 3),
            intArrayOf(1, 3, 2)
        )

        private val HEXAHEDRON_EIDX = arrayOf(
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
