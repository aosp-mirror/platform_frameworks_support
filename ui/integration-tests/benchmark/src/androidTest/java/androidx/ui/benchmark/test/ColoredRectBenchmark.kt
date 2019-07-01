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

package androidx.ui.benchmark.test

import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.compose.composer
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.benchmark.DefaultBenchmarkActivity
import androidx.ui.test.ComposeChoreographerRule
import androidx.ui.test.DisableTransitions
import androidx.ui.test.RectanglesInColumnTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


/**
 * Benchmark that runs [RectanglesInColumnTestCase]. Currently we test recomposition time.
 */
@LargeTest
@RunWith(Parameterized::class)
class ColoredRectBenchmark(private val amountOfCheckboxes: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Collection<Array<Any>> {
            return listOf(
                arrayListOf(1).toArray(),
                arrayListOf(10).toArray())
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(DefaultBenchmarkActivity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    // TODO(pavlis): Fix
    //@get:Rule
    //val choreographerRule = ComposeChoreographerRule()


    @Test
    @SuppressWarnings
    fun toggleRectangleColor_recompose() {
        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                val testCase = RectanglesInColumnTestCase(activityRule.activity, amountOfCheckboxes)
                    .apply { runSetup() }
                testCase.compositionContext.compose()

                val exec: BenchmarkRule.Scope.() -> Unit = {
                    runWithTimingDisabled {
                        testCase.toggleState()
                    }

                    testCase.compositionContext.compose()
                }

                benchmarkRule.measureRepeated(exec)
            }
        })
    }

}
