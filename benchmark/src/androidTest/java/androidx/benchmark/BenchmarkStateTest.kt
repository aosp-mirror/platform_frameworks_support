/*
 * Copyright 2018 The Android Open Source Project
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

import android.Manifest
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
=======
import org.junit.Assert.fail
import org.junit.Rule
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(JUnit4::class)
class BenchmarkStateTest {
    private fun ms2ns(ms: Long): Long = TimeUnit.MILLISECONDS.toNanos(ms)

    @get:Rule
    val writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    fun simple() {
        // would be better to mock the clock, but going with minimal changes for now
        val state = BenchmarkState()
        while (state.keepRunning()) {
            Thread.sleep(3)
            state.pauseTiming()
            Thread.sleep(5)
            state.resumeTiming()
        }
        val median = state.stats.median
        assertTrue("median $median should be between 2ms and 4ms",
                ms2ns(2) < median && median < ms2ns(4))
    }

    @Test
    fun ideSummary() {
        val summary1 = BenchmarkState().apply {
            while (keepRunning()) {
                Thread.sleep(1)
            }
        }.ideSummaryLine("foo")
        val summary2 = BenchmarkState().apply {
            while (keepRunning()) {
                // nothing
            }
        }.ideSummaryLine("fooBarLongerKey")

<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
        assertEquals(summary1.indexOf("foo"),
            summary2.indexOf("foo"))
=======
    @Test
    fun reportResult() {
        BenchmarkState.reportData("className", "testName", 100, listOf(100), 1, 1)
        val expectedReport = BenchmarkState.Report(
            className = "className",
            testName = "testName",
            nanos = 100,
            data = listOf(100),
            repeatIterations = 1,
            warmupIterations = 1
        )
        assertEquals(expectedReport, ResultWriter.reports.last())
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
    }
}
