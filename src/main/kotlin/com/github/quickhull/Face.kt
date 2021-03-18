package com.github.quickhull

import kotlin.math.abs
import kotlin.math.sqrt

internal class Face {
    var area = 0F
    var mark = VISIBLE
    var firstEdge: HalfEdge? = null
    var outside: Vertex? = null
    val centroid = Vec3()

    private var numVerts = 0
    private var planeOffset = 0F
    private val normal = Vec3()

    val numVertices get() = numVerts

    private fun computeCentroid(centroid: Vec3) {
        centroid.set(0F, 0F, 0F)
        var he = firstEdge
        do {
            he?.vertex?.point?.apply { centroid.add(x, y, z) }
            he = he?.next
        } while (he != firstEdge)
        centroid.scale(1F / numVerts)
    }

    private fun computeNormal(normal: Vec3) {
        val he1 = firstEdge!!.next
        var he2 = he1!!.next
        val p0 = firstEdge!!.vertex.point
        var p2 = he1.vertex.point
        var d2x = p2.x - p0.x
        var d2y = p2.y - p0.y
        var d2z = p2.z - p0.z
        var x = 0F
        var y = 0F
        var z = 0F
        numVerts = 2
        while (he2 != firstEdge) {
            val d1x = d2x
            val d1y = d2y
            val d1z = d2z
            p2 = he2!!.vertex.point
            d2x = p2.x - p0.x
            d2y = p2.y - p0.y
            d2z = p2.z - p0.z
            x += d1y * d2z - d1z * d2y
            y += d1z * d2x - d1x * d2z
            z += d1x * d2y - d1y * d2x
            he2 = he2.next
            numVerts++
        }
        normal.set(x, y, z)
        area = normal.length()
        normal.scale(1F / area)
    }

    private fun computeNormal(normal: Vec3, minArea: Float) {
        computeNormal(normal)
        if (area < minArea) {
            // make the normal more robust by removing components parallel to the longest edge
            var hedgeMax: HalfEdge? = null
            var lenSqrMax = 0F
            var hedge = firstEdge
            do {
                val lenSqr = hedge!!.lengthSquared
                if (lenSqr > lenSqrMax) {
                    hedgeMax = hedge
                    lenSqrMax = lenSqr
                }
                hedge = hedge.next
            } while (hedge != firstEdge)
            val p2 = hedgeMax!!.vertex.point
            val p1 = hedgeMax.tail!!.point
            val lenMax = sqrt(lenSqrMax)
            val ux = (p2.x - p1.x) / lenMax
            val uy = (p2.y - p1.y) / lenMax
            val uz = (p2.z - p1.z) / lenMax
            val dot = normal.x * ux + normal.y * uy + normal.z * uz
            normal.x -= dot * ux
            normal.y -= dot * uy
            normal.z -= dot * uz
            normal.normalize()
        }
    }

    fun distanceToPlane(p: Vec3) = normal.x * p.x + normal.y * p.y + normal.z * p.z - planeOffset

    fun getEdge(io: Int): HalfEdge? {
        var i = io
        var he = firstEdge
        while (i > 0) {
            he = he?.next
            i--
        }
        while (i < 0) {
            he = he?.prev
            i++
        }
        return he
    }

    private val vertexString: String
        get() {
            val s = StringBuilder("")
            var he = firstEdge
            do {
                he?.vertex?.index.apply { s.append(this).append(" ") }
                he = he?.next
            } while (he != firstEdge)
            return s.trim().toString()
        }

    fun mergeAdjacentFace(hedgeAdj: HalfEdge, discarded: Array<Face?>): Int {
        val oppFace = hedgeAdj.oppositeFace!!
        var numDiscarded = 0
        discarded[numDiscarded++] = oppFace
        oppFace.mark = DELETED
        val hedgeOpp = hedgeAdj.opposite
        var hedgeAdjPrev = hedgeAdj.prev
        var hedgeAdjNext = hedgeAdj.next
        var hedgeOppPrev = hedgeOpp!!.prev
        var hedgeOppNext = hedgeOpp.next
        while (hedgeAdjPrev!!.oppositeFace === oppFace) {
            hedgeAdjPrev = hedgeAdjPrev!!.prev
            hedgeOppNext = hedgeOppNext!!.next
        }
        while (hedgeAdjNext!!.oppositeFace === oppFace) {
            hedgeOppPrev = hedgeOppPrev!!.prev
            hedgeAdjNext = hedgeAdjNext!!.next
        }
        var hedge = hedgeOppNext
        while (hedge != hedgeOppPrev?.next) {
            hedge?.face = this
            hedge = hedge?.next
        }

        if (hedgeAdj == firstEdge) firstEdge = hedgeAdjNext

        // handle the half edges at the head
        var discardedFace = connectHalfEdges(hedgeOppPrev, hedgeAdjNext)
        if (discardedFace != null) {
            discarded[numDiscarded++] = discardedFace
        }

        // handle the half edges at the tail
        discardedFace = connectHalfEdges(hedgeAdjPrev, hedgeOppNext)
        if (discardedFace != null) {
            discarded[numDiscarded++] = discardedFace
        }

        computeNormalAndCentroid()
        checkConsistency()

        return numDiscarded
    }

    private fun computeNormalAndCentroid() {
        computeNormal(normal)
        computeCentroid(centroid)
        planeOffset = normal.dot(centroid)
        var numv = 0
        var he = firstEdge
        do {
            numv++
            he = he!!.next
        } while (he != firstEdge)
        if (numv != numVerts) {
            throw InternalErrorException("face %s vertex count is %d should be %d".format(vertexString, numVerts, numv))
        }
    }

    private fun computeNormalAndCentroid(minArea: Float) {
        computeNormal(normal, minArea)
        computeCentroid(centroid)
        planeOffset = normal.dot(centroid)
    }

    private fun connectHalfEdges(hedgePrev: HalfEdge?, hedge: HalfEdge?): Face? {
        var discardedFace: Face? = null
        if (hedgePrev!!.oppositeFace === hedge!!.oppositeFace) {
            val oppFace = hedge!!.oppositeFace
            val hedgeOpp: HalfEdge?
            if (hedgePrev == firstEdge) {
                firstEdge = hedge
            }
            if (oppFace?.numVertices == 3) {
                hedgeOpp = hedge.opposite!!.prev!!.opposite
                oppFace.mark = DELETED
                discardedFace = oppFace
            } else {
                hedgeOpp = hedge.opposite!!.next
                if (oppFace?.firstEdge == hedgeOpp!!.prev) {
                    oppFace?.firstEdge = hedgeOpp
                }
                hedgeOpp.prev = hedgeOpp.prev!!.prev
                hedgeOpp.prev!!.next = hedgeOpp
            }
            hedge.prev = hedgePrev!!.prev
            hedge.prev!!.next = hedge
            hedge.opposite = hedgeOpp
            hedgeOpp!!.opposite = hedge
            oppFace?.computeNormalAndCentroid()
        } else {
            hedgePrev!!.next = hedge
            hedge!!.prev = hedgePrev
        }
        return discardedFace
    }

    private fun checkConsistency() {
        if (numVerts < 3) throw InternalErrorException("degenerate face: $vertexString")

        var numv = 0
        var maxd = 0F
        var hedge = firstEdge

        do {
            val hedgeOpp = hedge!!.opposite
            when {
                hedgeOpp == null -> throw InternalErrorException(
                    "face %s: unreflected half edge %s".format(
                        vertexString,
                        hedge.vertexString
                    )
                )
                hedgeOpp.opposite != hedge -> throw InternalErrorException(
                    "face %s: opposite half edge %s has opposite %s".format(
                        vertexString,
                        hedgeOpp.vertexString,
                        hedgeOpp.opposite!!.vertexString
                    )
                )
                hedgeOpp.vertex != hedge.tail || hedge.vertex != hedgeOpp.tail -> throw InternalErrorException(
                    "face %s: half edge %s reflected by %s".format(
                        vertexString,
                        hedge.vertexString,
                        hedgeOpp.vertexString
                    )
                )
                else -> {
                    val oppFace = hedgeOpp.face
                    if (oppFace.mark == DELETED) throw InternalErrorException(
                        "face %s: opposite face %s not on hull".format(
                            vertexString,
                            oppFace.vertexString
                        )
                    )
                    val d = abs(distanceToPlane(hedge.vertex.point))
                    if (d > maxd) maxd = d
                    numv++
                    hedge = hedge.next
                }
            }
        } while (hedge != firstEdge)

        if (numv != numVerts) throw InternalErrorException("face %s vertex count is %d should be %d".format(vertexString, numVerts, numv))
    }

    companion object {
        const val DELETED = 3
        const val NON_CONVEX = 2
        const val VISIBLE = 1

        fun createTriangle(v0: Vertex, v1: Vertex, v2: Vertex, minArea: Float = 0F): Face {
            val face = Face()
            val he0 = HalfEdge(v0, face)
            val he1 = HalfEdge(v1, face)
            val he2 = HalfEdge(v2, face)
            he0.prev = he2
            he0.next = he1
            he1.prev = he0
            he1.next = he2
            he2.prev = he1
            he2.next = he0
            face.firstEdge = he0
            face.computeNormalAndCentroid(minArea)
            return face
        }
    }
}
