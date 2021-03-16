package zynaps.quickhull

internal class VertexList {
    private var head: Vertex? = null
    private var tail: Vertex? = null

    val first get() = head

    val isEmpty get() = head == null

    fun clear() {
        tail = null
        head = null
    }

    fun add(v: Vertex) {
        when (head) {
            null -> head = v
            else -> tail?.next = v
        }
        v.prev = tail
        v.next = null
        tail = v
    }

    fun addAll(v: Vertex) {
        var vtx: Vertex? = v
        when (head) {
            null -> head = vtx
            else -> tail?.next = vtx
        }
        vtx?.prev = tail
        while (vtx?.next != null) vtx = vtx.next
        tail = vtx
    }

    fun insertBefore(vtx: Vertex, next: Vertex) {
        vtx.prev = next.prev
        when (next.prev) {
            null -> head = vtx
            else -> next.prev?.next = vtx
        }
        vtx.next = next
        next.prev = vtx
    }

    fun delete(vtx: Vertex) {
        when (vtx.prev) {
            null -> head = vtx.next
            else -> vtx.prev?.next = vtx.next
        }
        when (vtx.next) {
            null -> tail = vtx.prev
            else -> vtx.next?.prev = vtx.prev
        }
    }

    fun delete(vtx1: Vertex, vtx2: Vertex) {
        when (vtx1.prev) {
            null -> head = vtx2.next
            else -> vtx1.prev?.next = vtx2.next
        }
        when (vtx2.next) {
            null -> tail = vtx1.prev
            else -> vtx2.next?.prev = vtx1.prev
        }
    }
}
