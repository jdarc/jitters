package zynaps.math

object Scalar {

    const val PI = kotlin.math.PI.toFloat()

    private const val LARGE = 1073741823.0

    fun min(a: Float, b: Float) = if (a < b) a else b

    fun min(a: Int, b: Int) = if (a < b) a else b

    fun max(a: Float, b: Float) = if (a > b) a else b

    fun max(a: Int, b: Int) = if (a > b) a else b

    fun ceil(a: Float) = 0x3FFFFFFF - (LARGE - a).toInt()

    fun floor(a: Float) = (LARGE + a).toInt() - 0x3FFFFFFF

    fun invSqrt(n: Float): Float {
        val x = Float.fromBits(0x5f3759df - (n.toRawBits() shr 1))
        return x * (1.5F - 0.5F * n * x * x)
    }
}
