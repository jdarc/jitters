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
