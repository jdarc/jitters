package zynaps.jitters.collision

class MaterialProperties(restitution: Float = 0.2F, friction: Float = 0.5F) {

    var restitution = 0F
        set(value) {
            field = value.coerceIn(0F, 1F)
        }

    var friction = 0F
        set(value) {
            field = value.coerceIn(0F, 1F)
        }

    init {
        this.restitution = restitution
        this.friction = friction
    }
}
