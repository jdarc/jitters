package zynaps.graphics

import zynaps.math.Matrix4

class Scene {

    val root = Node(Matrix4.IDENTITY)

    fun update(seconds: Float) {
        root.traverseDown { node ->
            node.update(seconds)
            node.updateTransform()
            true
        }

        root.traverseUp { node ->
            node.updateBounds()
            true
        }
    }

    fun render(frustum: Frustum, device: Device) = root.traverseDown { node ->
        when (node.isContainedBy(frustum)) {
            Containment.INSIDE -> node.render(device.apply { clip = false })
            Containment.PARTIAL -> node.render(device.apply { clip = true })
            Containment.OUTSIDE -> false
        }
    }
}
