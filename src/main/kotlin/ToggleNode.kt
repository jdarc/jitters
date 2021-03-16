import zynaps.graphics.Device
import zynaps.graphics.Node

class ToggleNode(val toggled: () -> Boolean) : Node() {
    override fun render(device: Device) = toggled()
}
