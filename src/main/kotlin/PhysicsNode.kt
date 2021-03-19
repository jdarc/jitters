import com.zynaps.graphics.Node
import com.zynaps.math.Matrix4
import com.zynaps.physics.dynamics.RigidBody

class PhysicsNode(private val body: RigidBody) : Node() {
    override fun update(seconds: Float) {
        transform = Matrix4.createTranslation(body.position) * body.orientation
    }
}
