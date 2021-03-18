import com.zynaps.graphics.Device
import com.zynaps.graphics.Node

class ToggleNode(val toggled: () -> Boolean) : Node() {
    override fun render(device: Device) = toggled()
}
