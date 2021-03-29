/*
 * Copyright (c) 2021 Jean d'Arc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.zynaps.tools

import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import kotlin.math.ceil
import kotlin.math.min

object Parallel {

    val CPUS = Runtime.getRuntime().availableProcessors()

    fun invoke(vararg callbacks: () -> Unit) {
        when {
            callbacks.isEmpty() -> return
            else -> ForkJoinPool.commonPool().invokeAll(callbacks.map {
                Callable {
                    it()
                    null
                }
            })
        }
    }

    fun execute(total: Int, callback: (next: Int) -> Unit) {
        when {
            total <= 0 -> return
            else -> ForkJoinPool.commonPool().invokeAll((0 until total).map {
                Callable {
                    callback(it)
                    null
                }
            })
        }
    }

    fun partition(total: Int, callback: (index: Int, from: Int, to: Int) -> Unit) {
        when {
            total <= 0 -> return
            else -> {
                val size = ceil(total.toFloat() / CPUS).toInt()
                ForkJoinPool.commonPool().invokeAll((0 until total step size).map {
                    Callable {
                        callback(it / size, it, it + min(size, total - it))
                        null
                    }
                })
            }
        }
    }
}
