package com.zynaps.graphics

import com.zynaps.math.Aabb
import com.zynaps.math.Matrix4

@Suppress("MemberVisibilityCanBePrivate")
open class Node(var transform: Matrix4 = Matrix4.IDENTITY, private val geometry: Geometry? = null) {
    private val nodes = mutableSetOf<Node>()
    private val combinedBounds = Aabb()
    private var combinedTransform = Matrix4.IDENTITY

    private var parent: Node? = null
        set(value) {
            if (value == parent) return
            field?.removeNode(this)
            field = value
            field?.addNode(this)
        }

    fun addNode(node: Node): Node {
        if (node != this && node !in nodes) {
            nodes.add(node)
            node.parent = this
        }
        return this
    }

    fun removeNode(node: Node): Node {
        if (node in nodes) {
            nodes.remove(node)
            node.parent = null
        }
        return this
    }

    fun traverseUp(visitor: (Node) -> Boolean) {
        nodes.forEach { it.traverseUp(visitor) }
        visitor(this)
    }

    fun traverseDown(visitor: (Node) -> Boolean) {
        if (visitor(this)) nodes.forEach { it.traverseDown(visitor) }
    }

    open fun update(seconds: Float) = Unit

    open fun render(device: Device): Boolean {
        geometry?.apply {
            device.world = combinedTransform
            render(device)
        }
        return true
    }

    fun updateTransform() {
        combinedTransform = (parent?.combinedTransform ?: Matrix4.IDENTITY) * transform
    }

    fun updateBounds() {
        combinedBounds.reset()
        nodes.forEach { combinedBounds.aggregate(it.combinedBounds) }
        geometry?.apply { combinedBounds.aggregate(this.bounds, combinedTransform) }
    }

    fun isContainedBy(container: Frustum) = container.test(combinedBounds)
}
