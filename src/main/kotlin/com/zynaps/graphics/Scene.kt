package com.zynaps.graphics

import com.zynaps.math.Matrix4

class Scene {

    val root = Node(Matrix4.IDENTITY)

    fun update(seconds: Float) {
        root.traverseDown {
            it.update(seconds)
            it.updateTransform()
            true
        }

        root.traverseUp {
            it.updateBounds()
            true
        }
    }

    fun render(frustum: Frustum, device: Device) = root.traverseDown {
        when (it.isContainedBy(frustum)) {
            Containment.INSIDE -> it.render(device.apply { clip = false })
            Containment.PARTIAL -> it.render(device.apply { clip = true })
            Containment.OUTSIDE -> false
        }
    }
}
