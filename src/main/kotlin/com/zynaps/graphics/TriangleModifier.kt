package com.zynaps.graphics

interface TriangleModifier {
    fun normals(n0: Int, n1: Int, n2: Int): TriangleModifier
    fun uvs(t0: Int, t1: Int, t2: Int): TriangleModifier
    fun useNormals(normalType: NormalType): TriangleModifier
    fun changeMaterial(material: Material): TriangleModifier
}
