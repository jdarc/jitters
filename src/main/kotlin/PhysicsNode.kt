import com.zynaps.graphics.Node
import com.zynaps.physics.dynamics.RigidBody
import com.zynaps.math.Matrix4

class PhysicsNode(private val body: RigidBody) : Node() {
    override fun update(seconds: Float) {
        transform = body.orientation * Matrix4.createTranslation(body.position)
    }
}
