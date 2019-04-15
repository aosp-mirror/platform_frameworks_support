/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.benchmark

import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
class ThermalBenchmark(val myInt: Int) {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val executors = List(6) {
        Executors.newSingleThreadExecutor()
    }

    private var endTimeNs: Long = 0

    private val runnable = Runnable {
        while (System.nanoTime() < endTimeNs) { /* spin! */ }
    }

    @Test
    fun spin() {
        benchmarkRule.measureRepeated {
            endTimeNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100)
            executors.map {
                it.execute(runnable)
            }
            Thread.sleep(101)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        @Parameterized.Parameters(name = "number={0}")
        fun data(): List<Array<Any>> {
            return List(100) { arrayOf(it) as Array<Any> }
        }
    }
}
