package zynaps.graphics

import zynaps.math.Scalar.ceil
import zynaps.math.Scalar.min
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

object Parallel {

    private val CPUS = Runtime.getRuntime().availableProcessors()

    fun execute(total: Int, callback: (next: Int) -> Unit) {
        when {
            total < 1 -> return
            else -> ForkJoinPool.commonPool().invokeAll((0 until total).map { Executors.callable { callback(it) } })
        }
    }

    fun partition(total: Int, callback: (index: Int, from: Int, to: Int) -> Unit) {
        when {
            total < 1 -> return
            else -> {
                val size = ceil(total.toFloat() / CPUS)
                ForkJoinPool.commonPool().invokeAll((0 until total step size).mapIndexed { index, it ->
                    Executors.callable { callback(index, it, it + min(size, total - it)) }
                })
            }
        }
    }
}
