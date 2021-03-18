package com.zynaps.graphics

import com.zynaps.math.Aabb

interface Geometry {
    val bounds: Aabb
    fun render(device: Device)
}
