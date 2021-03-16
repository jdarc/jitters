package zynaps.graphics

class TextureMaterial(private val texture: Texture) : Material {
    override fun sample(u: Float, v: Float) = texture.sample(u, v)
}
