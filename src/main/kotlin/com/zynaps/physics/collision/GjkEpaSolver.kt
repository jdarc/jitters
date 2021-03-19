package com.zynaps.physics.collision

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import kotlin.math.abs
import kotlin.math.max

internal object GjkEpaSolver {
    private val gjk = GJK()

    fun collide(shape0: CollisionShape, shape1: CollisionShape, collisionMargin: Float): Results {
        val results = Results()

        gjk.init(shape0, shape1, collisionMargin + EPA_ACCURACY)
        val collide = gjk.searchOrigin()
        results.gjkIterations = gjk.iterations + 1
        if (collide) {
            val epa = EPA(gjk)
            val pd = epa.evaluatePD()
            results.epaIterations = epa.iterations + 1
            if (pd > 0) {
                results.status = ResultsStatus.PENETRATING
                results.normal = epa.normal
                results.depth = pd
                results.pointA = epa.nearest[0]
                results.pointB = epa.nearest[1]
                return results
            } else if (epa.failed) results.status = ResultsStatus.EPA_FAILED
        } else if (gjk.failed) results.status = ResultsStatus.GJK_FAILED
        return results
    }

    enum class ResultsStatus { SEPARATED, PENETRATING, GJK_FAILED, EPA_FAILED }

    class Results {
        var pointA = Vector3.ZERO
        var pointB = Vector3.ZERO
        var normal = Vector3.ZERO
        var depth = 0F
        val hasCollided get() = status == ResultsStatus.PENETRATING

        var status = ResultsStatus.SEPARATED
        var epaIterations = 0
        var gjkIterations = 0
    }

    private const val INF = Float.MAX_VALUE
    private const val PI = Math.PI.toFloat()
    private const val TAU = PI * 2F
    private const val GJK_MAX_ITERATIONS = 128
    private const val GJK_HASH_SIZE = 64
    private const val GJK_HASH_MASK = GJK_HASH_SIZE - 1
    private const val GJK_IN_SIMPLEX_EPS = 0.0001F
    private const val GJK_SQ_IN_SIMPLEX_EPS = GJK_IN_SIMPLEX_EPS * GJK_IN_SIMPLEX_EPS
    private const val EPA_MAX_ITERATIONS = 256
    private const val EPA_IN_FACE_EPS = 0.01F
    private const val EPA_ACCURACY = 0.001F

    private val MOD3 = intArrayOf(0, 1, 2, 0, 1)

    private val TETRAHEDRON_FIDX = arrayOf(intArrayOf(2, 1, 0), intArrayOf(3, 0, 1), intArrayOf(3, 1, 2), intArrayOf(3, 2, 0))
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

    private class He {
        var v = Vector3.ZERO
        var n: He? = null
    }

    private class Mkv {
        var w = Vector3.ZERO
        var r = Vector3.ZERO

        fun set(m: Mkv) {
            w = m.w
            r = m.r
        }
    }

    private class Face {
        val v = Array(3) { Mkv() }
        val f = arrayOfNulls<Face>(3)
        val e = IntArray(3)
        var n = Vector3.ZERO
        var d = 0F
        var mark = 0
        var prev: Face? = null
        var next: Face? = null
    }

    private class GJK {
        val table = arrayOfNulls<He>(GJK_HASH_SIZE)
        lateinit var shapes: Array<CollisionShape>

        val simplex = arrayOf(Mkv(), Mkv(), Mkv(), Mkv(), Mkv())
        var ray = Vector3.ZERO

        var order = 0
        var iterations = 0
        var margin = 0F
        var failed = false

        fun init(shape0: CollisionShape, shape1: CollisionShape, pMargin: Float) {
            shapes = arrayOf(shape0, shape1)
            margin = pMargin
            failed = false
        }

        fun hash(v: Vector3) = ((v.x * 15461).toInt() xor (v.y * 83003).toInt() xor (v.z * 15473).toInt()) * 169639 and GJK_HASH_MASK

        fun localSupport(d: Vector3, i: Int) = shapes[i].getSupport(d)

        fun support(d: Vector3, v: Mkv): Vector3 {
            v.r = d
            val tmp1 = localSupport(d, 0)
            val tmp = -d
            val tmp2 = localSupport(tmp, 1)
            val x = margin * d.x + (tmp1.x - tmp2.x)
            val y = margin * d.y + (tmp1.y - tmp2.y)
            val z = margin * d.z + (tmp1.z - tmp2.z)
            return Vector3(x, y, z)
        }

        fun fetchSupport(): Boolean {
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
            return Vector3.dot(ray, simplex[order].w) > 0
        }

        fun solveSimplex2(ao: Vector3, ab: Vector3): Boolean {
            when {
                Vector3.dot(ab, ao) >= 0 -> {
                    val cabo = Vector3.cross(ab, ao)
                    ray = when {
                        cabo.lengthSquared > GJK_SQ_IN_SIMPLEX_EPS -> Vector3.cross(cabo, ab)
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

        fun solveSimplex3(ao: Vector3, ab: Vector3, ac: Vector3) = solveSimplex3a(ao, ab, ac, Vector3.cross(ab, ac))

        fun solveSimplex3a(ao: Vector3, ab: Vector3, ac: Vector3, cabc: Vector3): Boolean {
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

        fun solveSimplex4(ao: Vector3, ab: Vector3, ac: Vector3, ad: Vector3): Boolean {
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

        fun searchOrigin(initray: Vector3 = Vector3.UNIT_X): Boolean {
            iterations = 0
            order = -1
            failed = false
            ray = Vector3.normalize(initray)
            table.fill(null)
            fetchSupport()
            ray = -simplex[0].w
            while (iterations < GJK_MAX_ITERATIONS) {
                val rl = ray.length
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

        fun encloseOrigin(): Boolean {
            when (order) {
                0 -> {
                }
                1 -> {
                    val ab = simplex[1].w - simplex[0].w
                    val b = arrayOf(Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.UNIT_Z)
                    b[0] = Vector3.cross(ab, b[0])
                    b[1] = Vector3.cross(ab, b[1])
                    b[2] = Vector3.cross(ab, b[2])
                    val m = floatArrayOf(b[0].lengthSquared, b[1].lengthSquared, b[2].lengthSquared)
                    val r = Matrix4.createFromAxisAngle(Vector3.normalize(ab), TAU / 3F)
                    val w = b[if (m[0] > m[1]) if (m[0] > m[2]) 0 else 2 else if (m[1] > m[2]) 1 else 2]
                    simplex[4].w = support(Vector3.normalize(w), simplex[4])
                    simplex[2].w = support(Vector3.normalize(r * w), simplex[2])
                    simplex[3].w = support(Vector3.normalize(r * w), simplex[3])
                    order = 4
                    return true
                }
                2 -> {
                    val n = Vector3.normalize(Vector3.cross(simplex[1].w - simplex[0].w, simplex[2].w - simplex[0].w))
                    simplex[3].w = support(n, simplex[3])
                    simplex[4].w = support(-n, simplex[4])
                    order = 4
                    return true
                }
                3 -> return true
                4 -> return true
            }
            return false
        }
    }

    private class EPA(val gjk: GJK) {
        var root: Face? = null
        var nfaces = 0
        var iterations = 0
        val features = Array(2) { Array(3) { Vector3.ZERO } }
        val nearest = arrayOf(Vector3.ZERO, Vector3.ZERO)
        var normal = Vector3.ZERO
        var depth = 0F
        var failed = false

        fun evaluatePD(accuracy: Float = EPA_ACCURACY): Float {
            var bestface: Face? = null
            var markid = 1
            depth = -INF
            normal = Vector3.ZERO
            root = null
            nfaces = 0
            iterations = 0
            failed = false
            if (gjk.encloseOrigin()) {
                var pfIdxPtr: Array<IntArray>? = null
                var pfIdxIndex = 0
                var nfIdx = 0
                var peIdxPtr: Array<IntArray>? = null
                var peIdxIndex = 0
                var neIdx = 0
                val baseMkv = Array(5) { Mkv() }
                val baseFaces = Array(6) { Face() }
                when (gjk.order) {
                    3 -> {
                        pfIdxPtr = TETRAHEDRON_FIDX
                        pfIdxIndex = 0
                        nfIdx = 4
                        peIdxPtr = TETRAHEDRON_EIDX
                        peIdxIndex = 0
                        neIdx = 6
                    }
                    4 -> {
                        pfIdxPtr = HEXAHEDRON_FIDX
                        pfIdxIndex = 0
                        nfIdx = 6
                        peIdxPtr = HEXAHEDRON_EIDX
                        peIdxIndex = 0
                        neIdx = 9
                    }
                }
                var i = 0
                while (i <= gjk.order) {
                    baseMkv[i] = Mkv()
                    baseMkv[i].set(gjk.simplex[i])
                    ++i
                }
                i = 0
                while (i < nfIdx) {
                    baseFaces[i] = newFace(
                        baseMkv[pfIdxPtr!![pfIdxIndex][0]],
                        baseMkv[pfIdxPtr[pfIdxIndex][1]],
                        baseMkv[pfIdxPtr[pfIdxIndex][2]]
                    )
                    ++i
                    pfIdxIndex++
                }
                i = 0
                while (i < neIdx) {
                    link(
                        baseFaces[peIdxPtr?.get(peIdxIndex)?.get(0)!!],
                        peIdxPtr[peIdxIndex][1],
                        baseFaces[peIdxPtr[peIdxIndex][2]],
                        peIdxPtr[peIdxIndex][3]
                    )
                    ++i
                    peIdxIndex++
                }
            }
            if (0 == nfaces) {
                return depth
            }

            while (iterations < EPA_MAX_ITERATIONS) {
                val bf = findBest()
                if (bf != null) {
                    val tmp = -bf.n
                    val w = support(tmp)
                    val d = Vector3.dot(bf.n, w.w) + bf.d
                    bestface = bf
                    if (d < -accuracy) {
                        val cf = arrayOf<Face?>(null)
                        val ff = arrayOf<Face?>(null)
                        var nf = 0
                        detach(bf)
                        bf.mark = ++markid
                        for (i in 0..2) {
                            nf += buildHorizon(markid, w, bf.f[i]!!, bf.e[i], cf, ff)
                        }
                        if (nf <= 2) break
                        link(cf[0]!!, 1, ff[0]!!, 2)
                    } else break
                } else break
                ++iterations
            }

            if (bestface != null) {
                val b = getCoordinates(bestface)
                normal = bestface.n
                depth = max(0F, bestface.d)
                for (i in 0..1) {
                    val s = if (i != 0) -1F else 1F
                    for (j in 0..2) {
                        val tmp = bestface.v[j].r * s
                        features[i][j] = gjk.localSupport(tmp, i)
                    }
                }
                var tmp1 = features[0][0] * b.x
                var tmp2 = features[0][1] * b.y
                var tmp3 = features[0][2] * b.z
                nearest[0] = Vector3(
                    tmp1.x + tmp2.x + tmp3.x,
                    tmp1.y + tmp2.y + tmp3.y,
                    tmp1.z + tmp2.z + tmp3.z
                )
                tmp1 = features[1][0] * b.x
                tmp2 = features[1][1] * b.y
                tmp3 = features[1][2] * b.z
                nearest[1] = Vector3(
                    tmp1.x + tmp2.x + tmp3.x,
                    tmp1.y + tmp2.y + tmp3.y,
                    tmp1.z + tmp2.z + tmp3.z
                )
            } else {
                failed = true
            }
            return depth
        }

        fun getCoordinates(face: Face): Vector3 {
            var tmp: Vector3
            val o = face.n * -face.d
            val a = FloatArray(3)
            var tmp1 = face.v[0].w - o
            var tmp2 = face.v[1].w - o
            tmp = Vector3.cross(tmp1, tmp2)
            a[0] = tmp.length
            tmp1 = face.v[1].w - o
            tmp2 = face.v[2].w - o
            tmp = Vector3.cross(tmp1, tmp2)
            a[1] = tmp.length
            tmp1 = face.v[2].w - o
            tmp2 = face.v[0].w - o
            tmp = Vector3.cross(tmp1, tmp2)
            a[2] = tmp.length
            val sm = a[0] + a[1] + a[2]
            return Vector3(a[1], a[2], a[0]) * (1F / if (sm > 0F) sm else 1F)
        }

        fun findBest(): Face? {
            var bf: Face? = null
            if (root != null) {
                var cf = root
                var bd = INF
                do {
                    if (cf!!.d < bd) {
                        bd = cf.d
                        bf = cf
                    }
                    cf = cf.next
                } while (cf != null)
            }
            return bf
        }

        fun set(f: Face, a: Mkv?, b: Mkv?, c: Mkv?): Boolean {
            var tmp1 = b!!.w - a!!.w
            var tmp2 = c!!.w - a.w
            val nrm = Vector3.cross(tmp1, tmp2)
            val len = nrm.length
            tmp1 = Vector3.cross(a.w, b.w)
            tmp2 = Vector3.cross(b.w, c.w)
            val tmp3 = Vector3.cross(c.w, a.w)
            val valid = Vector3.dot(tmp1, nrm) >= -EPA_IN_FACE_EPS &&
                        Vector3.dot(tmp2, nrm) >= -EPA_IN_FACE_EPS &&
                        Vector3.dot(tmp3, nrm) >= -EPA_IN_FACE_EPS
            f.v[0] = a
            f.v[1] = b
            f.v[2] = c
            f.mark = 0
            f.n = nrm * (1F / if (len > 0F) len else INF)
            f.d = max(0F, -Vector3.dot(f.n, a.w))
            return valid
        }

        fun newFace(a: Mkv?, b: Mkv?, c: Mkv?): Face {
            val pf = Face()
            if (set(pf, a, b, c)) {
                if (root != null) root!!.prev = pf
                pf.prev = null
                pf.next = root
                root = pf
                ++nfaces
            } else {
                pf.next = null
                pf.prev = pf.next
            }
            return pf
        }

        fun detach(face: Face) {
            if (face.prev != null || face.next != null) {
                --nfaces
                if (face == root) {
                    root = face.next
                    root!!.prev = null
                } else {
                    if (face.next == null) {
                        face.prev!!.next = null
                    } else {
                        face.prev!!.next = face.next
                        face.next!!.prev = face.prev
                    }
                }
                face.next = null
                face.prev = face.next
            }
        }

        fun link(f0: Face, e0: Int, f1: Face, e1: Int) {
            f0.f[e0] = f1
            f1.e[e1] = e0
            f1.f[e1] = f0
            f0.e[e0] = e1
        }

        fun support(w: Vector3): Mkv {
            val v = Mkv()
            v.w = gjk.support(w, v)
            return v
        }

        fun buildHorizon(markId: Int, w: Mkv, f: Face, e: Int, cf: Array<Face?>, ff: Array<Face?>): Int {
            var ne = 0
            if (f.mark != markId) {
                val e1 = MOD3[e + 1]
                if (Vector3.dot(f.n, w.w) + f.d > 0) {
                    val nf = newFace(f.v[e1], f.v[e], w)
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
    }
}
