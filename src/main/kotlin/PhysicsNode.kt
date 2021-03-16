import zynaps.graphics.Node
import zynaps.jitters.physics.RigidBody
import zynaps.math.Matrix4

class PhysicsNode(private val body: RigidBody) : Node() {
    override fun update(seconds: Float) {
        transform = body.orientation * Matrix4.createTranslation(body.position)
    }
}
