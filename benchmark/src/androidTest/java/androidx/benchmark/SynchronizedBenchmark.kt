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
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger

@LargeTest
@RunWith(JUnit4::class)
class SynchronizedBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Volatile
    private var volatileInt = 0

    private val atomicInt = AtomicInteger(0)

    @Test
    fun atomicIncrementBenchmark() {
        benchmarkRule.measureRepeated {
            atomicInt.getAndIncrement()
        }
    }

    @Test
    fun synchronizedIncrementBenchmark() {
        benchmarkRule.measureRepeated {
            synchronized(volatileInt) {
                volatileInt++
            }
        }
    }
}
