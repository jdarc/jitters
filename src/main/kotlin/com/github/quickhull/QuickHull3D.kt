package com.github.quickhull

import com.zynaps.math.Scalar.max
import kotlin.math.abs
import kotlin.math.sqrt

class QuickHull3D(coords: FloatArray) {
    private var pointBuffer = arrayOf<Vertex>()
    private var vertexPointIndices = IntArray(0)
    private val maxVtxs = Array(3) { Vertex() }
    private val minVtxs = Array(3) { Vertex() }
    private val facets = ArrayList<Face?>(16)
    private val claimed = VertexList()
    private val numVertices: Int
    private val tolerance: Float

    val vertices: FloatArray
        get() {
            val vertices = FloatArray(numVertices * 3)
            for (i in 0 until numVertices) {
                pointBuffer[vertexPointIndices[i]].point.apply {
                    vertices[i * 3 + 0] = x
                    vertices[i * 3 + 1] = y
                    vertices[i * 3 + 2] = z
                }
            }
            return vertices
        }

    val faces: Array<IntArray>
        get() = facets.fold(emptyArray()) { acc, cur ->
            val indices = IntArray(cur!!.numVertices)
            val indexedFromOne = 0 and INDEXED_FROM_ONE != 0
            val pointRelative = 0 and POINT_RELATIVE != 0
            var hedge = cur.firstEdge
            var k = 0
            do {
                var idx = hedge!!.vertex.index
                if (pointRelative) idx = vertexPointIndices[idx]
                if (indexedFromOne) idx++
                indices[k++] = idx
                hedge = if (0 and CLOCKWISE == 0) hedge.next else hedge.prev
            } while (hedge != cur.firstEdge)
            acc + indices
        }

    init {
        require(coords.size / 3 >= 4) { "Less than four input points specified" }
        require(coords.size / 3.0 >= coords.size / 3) { "Coordinate array too small for specified number of points" }

        pointBuffer = Array(coords.size / 3) { Vertex() }
        for (i in pointBuffer.indices) {
            pointBuffer[i].point.set(coords[i * 3 + 0], coords[i * 3 + 1], coords[i * 3 + 2])
            pointBuffer[i].index = i
        }

        tolerance = computeMaxAndMin(maxVtxs, minVtxs, pointBuffer)

        createInitialSimplex(maxVtxs, minVtxs, tolerance, pointBuffer, facets, claimed)

        val horizon = mutableListOf<HalfEdge>()
        val unclaimed = VertexList()
        val newFaces = mutableListOf<Face>()
        val discardedFaces = arrayOfNulls<Face>(3)
        var eyeVtx: Vertex?
        while (nextPointToAdd(claimed).also { eyeVtx = it } != null) {
            discardedFaces.fill(null)
            horizon.clear()
            unclaimed.clear()
            newFaces.clear()
            addPointToHull(eyeVtx!!, horizon, claimed, unclaimed, newFaces, tolerance, discardedFaces, facets)
        }

        for (element in pointBuffer) element.index = -1

        // remove inactive faces and mark active vertices
        val it = facets.iterator()
        while (it.hasNext()) {
            val face = it.next()
            when {
                face?.mark != Face.VISIBLE -> it.remove()
                else -> {
                    val he0 = face.firstEdge
                    var he = he0
                    do {
                        he?.vertex?.index = 0
                        he = he?.next
                    } while (he != he0)
                }
            }
        }

        vertexPointIndices = IntArray(pointBuffer.size)
        var total = 0
        for (i in pointBuffer.indices) {
            val vtx = pointBuffer[i]
            if (vtx.index == 0) {
                vertexPointIndices[total] = i
                vtx.index = total++
            }
        }
        numVertices = total
    }

    private companion object {
        const val NON_CONVEX_WRT_LARGER_FACE = 1
        const val NON_CONVEX = 2
        const val CLOCKWISE = 0x1
        const val INDEXED_FROM_ONE = 0x2
        const val POINT_RELATIVE = 0x8
        const val FLOAT_PREC = 1.49011611938e-8F

        fun createInitialSimplex(
            maxVtxs: Array<Vertex>,
            minVtxs: Array<Vertex>,
            tolerance: Float,
            pointBuffer: Array<Vertex>,
            faces: MutableList<Face?>,
            claimed: VertexList
        ) {
            val numPoints: Int = pointBuffer.size
            var max = 0F
            var imax = 0
            for (i in 0..2) {
                val diff = maxVtxs[i].point[i] - minVtxs[i].point[i]
                if (diff > max) {
                    max = diff
                    imax = i
                }
            }
            require(max > tolerance) { "Input points appear to be coincident" }
            val vtx = Array(4) { Vertex() }
            // set first two vertices to be those with the greatest one dimensional separation
            vtx[0] = maxVtxs[imax]
            vtx[1] = minVtxs[imax]

            // set third vertex to be the vertex farthest from the line between vtx0 and vtx1
            val u01 = Vec3()
            val diff02 = Vec3()
            val nrml = Vec3()
            val xprod = Vec3()
            var maxSqr = 0F
            u01.sub(vtx[1].point, vtx[0].point)
            u01.normalize()
            for (i in 0 until numPoints) {
                diff02.sub(pointBuffer[i].point, vtx[0].point)
                xprod.cross(u01, diff02)
                val lenSqr = xprod.lengthSquared()
                if (lenSqr > maxSqr && pointBuffer[i] != vtx[0] && pointBuffer[i] != vtx[1]) {
                    maxSqr = lenSqr
                    vtx[2] = pointBuffer[i]
                    nrml.set(xprod)
                }
            }
            require(sqrt(maxSqr) > 100 * tolerance) { "Input points appear to be co-linear" }
            nrml.normalize()
            var maxDist = 0F
            val d0 = vtx[2].point.dot(nrml)
            for (i in 0 until numPoints) {
                val dist = abs(pointBuffer[i].point.dot(nrml) - d0)
                if (dist > maxDist && pointBuffer[i] != vtx[0] && pointBuffer[i] != vtx[1] && pointBuffer[i] != vtx[2]) {
                    maxDist = dist
                    vtx[3] = pointBuffer[i]
                }
            }
            require(abs(maxDist) > 100 * tolerance) { "Input points appear to be coplanar" }
            val tris = arrayOfNulls<Face>(4)
            if (vtx[3].point.dot(nrml) - d0 < 0) {
                tris[0] = Face.createTriangle(vtx[0], vtx[1], vtx[2])
                tris[1] = Face.createTriangle(vtx[3], vtx[1], vtx[0])
                tris[2] = Face.createTriangle(vtx[3], vtx[2], vtx[1])
                tris[3] = Face.createTriangle(vtx[3], vtx[0], vtx[2])
                for (i in 0..2) {
                    val k = (i + 1) % 3
                    tris[i + 1]?.getEdge(1)?.opposite = tris[k + 1]?.getEdge(0)
                    tris[i + 1]?.getEdge(2)?.opposite = tris[0]?.getEdge(k)
                }
            } else {
                tris[0] = Face.createTriangle(vtx[0], vtx[2], vtx[1])
                tris[1] = Face.createTriangle(vtx[3], vtx[0], vtx[1])
                tris[2] = Face.createTriangle(vtx[3], vtx[1], vtx[2])
                tris[3] = Face.createTriangle(vtx[3], vtx[2], vtx[0])
                for (i in 0..2) {
                    val k = (i + 1) % 3
                    tris[i + 1]?.getEdge(0)?.opposite = tris[k + 1]?.getEdge(1)
                    tris[i + 1]?.getEdge(2)?.opposite = tris[0]?.getEdge((3 - i) % 3)
                }
            }
            faces.add(tris[0])
            faces.add(tris[1])
            faces.add(tris[2])
            faces.add(tris[3])
            for (i in 0 until numPoints) {
                val v = pointBuffer[i]
                if (v == vtx[0] || v == vtx[1] || v == vtx[2] || v == vtx[3]) {
                    continue
                }
                maxDist = tolerance
                var maxFace: Face? = null
                for (k in 0..3) {
                    val dist = tris[k]!!.distanceToPlane(v.point)
                    if (dist > maxDist) {
                        maxFace = tris[k]
                        maxDist = dist
                    }
                }
                if (maxFace != null) {
                    addPointToFace(v, maxFace, claimed)
                }
            }
        }

        fun addPointToHull(
            eyeVtx: Vertex, horizon: MutableList<HalfEdge>,
            claimed: VertexList?, unclaimed: VertexList, newFaces: MutableList<Face>, tolerance: Float,
            discardedFaces: Array<Face?>, faces: MutableList<Face?>
        ) {
            horizon.clear()
            unclaimed.clear()

            removePointFromFace(eyeVtx, eyeVtx.face!!, claimed!!)
            calculateHorizon(eyeVtx.point, null, eyeVtx.face!!, horizon, claimed, unclaimed, tolerance)

            newFaces.clear()
            addNewFaces(newFaces, eyeVtx, horizon, faces)

            // first merge pass ... merge faces which are non-convex as determined by the larger face
            for (face in newFaces) {
                if (face.mark == Face.VISIBLE) {
                    var loop = true
                    while (loop) {
                        loop = doAdjacentMerge(
                            face, NON_CONVEX_WRT_LARGER_FACE, tolerance, discardedFaces,
                            claimed, unclaimed
                        )
                    }
                }
            }
            // second merge pass ... merge faces which are non-convex wrt either face
            for (face in newFaces) {
                if (face.mark == Face.NON_CONVEX) {
                    face.mark = Face.VISIBLE
                    var loop = true
                    while (loop) {
                        loop = doAdjacentMerge(face, NON_CONVEX, tolerance, discardedFaces, claimed, unclaimed)
                    }
                }
            }
            resolveUnclaimedPoints(newFaces, claimed, unclaimed, tolerance)
        }

        fun doAdjacentMerge(
            face: Face,
            mergeType: Int,
            tolerance: Float,
            discardedFaces: Array<Face?>,
            claimed: VertexList,
            unclaimed: VertexList
        ): Boolean {
            var hedge = face.firstEdge
            var convex = true
            do {
                val oppFace = hedge!!.oppositeFace
                var merge = false
                if (mergeType == NON_CONVEX) { // then merge faces if they are
                    // definitively non-convex
                    if (hedge.oppFaceDistance > -tolerance || hedge.opposite!!.oppFaceDistance > -tolerance) {
                        merge = true
                    }
                } else {
                    if (face.area > oppFace!!.area) {
                        if (hedge.oppFaceDistance > -tolerance) {
                            merge = true
                        } else if (hedge.opposite!!.oppFaceDistance > -tolerance) {
                            convex = false
                        }
                    } else {
                        if (hedge.opposite!!.oppFaceDistance > -tolerance) {
                            merge = true
                        } else if (hedge.oppFaceDistance > -tolerance) {
                            convex = false
                        }
                    }
                }
                if (merge) {
                    val numd = face.mergeAdjacentFace(hedge, discardedFaces)
                    for (i in 0 until numd) {
                        deleteFacePoints(discardedFaces[i]!!, face, claimed, unclaimed, tolerance)
                    }
                    return true
                }
                hedge = hedge.next
            } while (hedge != face.firstEdge)

            if (!convex) face.mark = Face.NON_CONVEX
            return false
        }

        fun calculateHorizon(
            eyePnt: Vec3,
            edge_: HalfEdge?,
            face: Face,
            horizon: MutableList<HalfEdge>,
            claimed: VertexList,
            unclaimed: VertexList,
            tolerance: Float
        ) {
            var edge0 = edge_
            deleteFacePoints(face, null, claimed, unclaimed, tolerance)
            face.mark = Face.DELETED
            var edge: HalfEdge?
            if (edge0 == null) {
                edge0 = face.getEdge(0)
                edge = edge0
            } else {
                edge = edge0.next
            }
            do {
                val oppFace = edge!!.oppositeFace!!
                if (oppFace.mark == Face.VISIBLE) {
                    if (oppFace.distanceToPlane(eyePnt) > tolerance) {
                        calculateHorizon(eyePnt, edge.opposite, oppFace, horizon, claimed, unclaimed, tolerance)
                    } else {
                        horizon.add(edge)
                    }
                }
                edge = edge.next
            } while (edge != edge0)
        }

        fun resolveUnclaimedPoints(newFaces: MutableList<Face>, claimed: VertexList, unclaimed: VertexList, tolerance: Float) {
            var vtxNext = unclaimed.first
            var vtx = vtxNext
            while (vtx != null) {
                vtxNext = vtx.next
                var maxDist = tolerance
                var maxFace: Face? = null
                for (newFace in newFaces) {
                    if (newFace.mark == Face.VISIBLE) {
                        val dist = newFace.distanceToPlane(vtx.point)
                        if (dist > maxDist) {
                            maxDist = dist
                            maxFace = newFace
                        }
                        if (maxDist > 1000 * tolerance) {
                            break
                        }
                    }
                }
                if (maxFace != null) {
                    addPointToFace(vtx, maxFace, claimed)
                }
                vtx = vtxNext
            }
        }


        fun addNewFaces(newFaces: MutableList<Face>, eyeVtx: Vertex, horizon: List<HalfEdge>, faces: MutableList<Face?>) {
            newFaces.clear()
            var hedgeSidePrev: HalfEdge? = null
            var hedgeSideBegin: HalfEdge? = null
            for (o in horizon) {
                val hedgeSide = addAdjoiningFace(eyeVtx, o, faces)
                if (hedgeSidePrev != null) {
                    hedgeSide?.next?.opposite = hedgeSidePrev
                } else {
                    hedgeSideBegin = hedgeSide
                }
                newFaces.add(hedgeSide!!.face)
                hedgeSidePrev = hedgeSide
            }
            hedgeSideBegin?.next?.opposite = hedgeSidePrev
        }

        fun deleteFacePoints(face: Face, absorbingFace: Face?, claimed: VertexList, unclaimed: VertexList, tolerance: Float) {
            val faceVtxs = removeAllPointsFromFace(face, claimed)
            if (faceVtxs != null) {
                if (absorbingFace == null) {
                    unclaimed.addAll(faceVtxs)
                } else {
                    var vtxNext = faceVtxs
                    var vtx = vtxNext
                    while (vtx != null) {
                        vtxNext = vtx.next
                        val dist = absorbingFace.distanceToPlane(vtx.point)
                        if (dist > tolerance) {
                            addPointToFace(vtx, absorbingFace, claimed)
                        } else {
                            unclaimed.add(vtx)
                        }
                        vtx = vtxNext
                    }
                }
            }
        }

        fun addPointToFace(vtx: Vertex, face: Face, claimed: VertexList) {
            vtx.face = face
            if (face.outside == null) {
                claimed.add(vtx)
            } else {
                face.outside?.apply { claimed.insertBefore(vtx, this) }
            }
            face.outside = vtx
        }

        fun removeAllPointsFromFace(face: Face, claimed: VertexList): Vertex? {
            return if (face.outside != null) {
                var end = face.outside
                while (end?.next != null && end.next?.face == face) {
                    end = end.next
                }
                end?.apply { claimed.delete(face.outside!!, this) }
                end?.next = null
                face.outside
            } else {
                null
            }
        }


        fun addAdjoiningFace(eyeVtx: Vertex, he: HalfEdge, faces: MutableList<Face?>): HalfEdge? {
            val face = Face.createTriangle(eyeVtx, he.tail!!, he.vertex)
            faces.add(face)
            he.opposite?.apply { face.getEdge(-1)?.opposite = this }
            return face.getEdge(0)
        }

        fun computeMaxAndMin(maxVtxs: Array<Vertex>, minVtxs: Array<Vertex>, points: Array<Vertex>): Float {
            val max = Vec3()
            val min = Vec3()
            for (i in 0..2) {
                minVtxs[i] = points[0]
                maxVtxs[i] = minVtxs[i]
            }
            max.set(points[0].point)
            min.set(points[0].point)
            for (i in 1 until points.size) {
                val pnt = points[i].point
                when {
                    pnt.x > max.x -> {
                        max.x = pnt.x
                        maxVtxs[0] = points[i]
                    }
                    pnt.x < min.x -> {
                        min.x = pnt.x
                        minVtxs[0] = points[i]
                    }
                }
                when {
                    pnt.y > max.y -> {
                        max.y = pnt.y
                        maxVtxs[1] = points[i]
                    }
                    pnt.y < min.y -> {
                        min.y = pnt.y
                        minVtxs[1] = points[i]
                    }
                }
                when {
                    pnt.z > max.z -> {
                        max.z = pnt.z
                        maxVtxs[2] = points[i]
                    }
                    pnt.z < min.z -> {
                        min.z = pnt.z
                        minVtxs[2] = points[i]
                    }
                }
            }

            // this epsilon formula comes from QuickHull, and I'm not about to quibble.
            return 3F * FLOAT_PREC * (max(abs(max.x), abs(min.x)) + max(abs(max.y), abs(min.y)) + max(abs(max.z), abs(min.z)))
        }

        fun nextPointToAdd(claimed: VertexList) = if (claimed.isEmpty) null else {
            val eyeFace = claimed.first!!.face!!
            var eyeVtx: Vertex? = null
            var maxDist = 0F
            var vtx = eyeFace.outside
            while (vtx != null && vtx.face == eyeFace) {
                val dist = eyeFace.distanceToPlane(vtx.point)
                if (dist > maxDist) {
                    maxDist = dist
                    eyeVtx = vtx
                }
                vtx = vtx.next
            }
            eyeVtx
        }

        fun removePointFromFace(vertex: Vertex, face: Face, claimed: VertexList) {
            if (vertex == face.outside) {
                when {
                    vertex.next != null && vertex.next?.face == face -> face.outside = vertex.next
                    else -> face.outside = null
                }
            }
            claimed.delete(vertex)
        }
    }
}
