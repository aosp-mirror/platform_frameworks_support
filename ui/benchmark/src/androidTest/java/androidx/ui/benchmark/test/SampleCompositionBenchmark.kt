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
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.disposeComposition
import androidx.compose.memo
import androidx.compose.setContent
import androidx.compose.unaryPlus
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.benchmark.DefaultBenchmarkActivity
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.ContextAmbient
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.DensityAmbient
import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.Ref
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.input.FocusManager
import androidx.ui.layout.Column
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized
import java.util.Arrays

/**
 * These are just very simple initial benchmark samples.
 * TODO(pavlis): Figure out and implement more interesting benchmarks. Including comparison
 * to legacy.
 */
@LargeTest
//@RunWith(Parameterized::class)
@RunWith(JUnit4::class)
class SampleCompositionBenchmark {

//    @Parameterized.Parameter(value = 0)
//    var mTestInteger: Int = 0
//
//    @Parameterized.Parameters
//    fun initParameters(): Collection<Array<Any>> {
//        return listOf(listOf(0), listOf(1))
//    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(DefaultBenchmarkActivity::class.java)

    /**
     * Measures how long it take to compose a basic crane wrapper into activity. This also includes
     * the time it takes to do one pass of measuring.
     */
    @Test
    fun composeJustRootIntoActivity() {
        val state = benchmarkRule.getState()

        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                val root = AndroidCraneView(activityRule.activity)
                activityRule.activity.setContentView(root)

                while (state.keepRunning()) {
                    val focusManager = FocusManager()

                    Compose.composeInto(root.root, activityRule.activity) {
                        ContextAmbient.Provider(value = activityRule.activity) {
                            DensityAmbient.Provider(value = Density(activityRule.activity)) {
                                FocusManagerAmbient.Provider(value = focusManager) {
                                    TextInputServiceAmbient.Provider(value = root.textInputService) {
                                        MaterialTheme {
                                            Surface {
                                                Column { }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                    Compose.disposeComposition(root.root, activityRule.activity)
                }
            }
        });



            // This is possible due to the fact that composeInto is synchronous.
//            activityRule.activity.setContent {
//                CraneWrapper {
//                    MaterialTheme {
//                        Surface {
//                            Column { }
//                        }
//                    }
//                }
//            }
//
//            state.pauseTiming()
//            activityRule.launchActivity(null)
//            state.resumeTiming()

            // Every setContent allocates new frame layout and adds composition. This would lead to
            // gradually increasing memory and would skew the results. So we need to decompose.
            //activityRule.activity.disposeComposition()

    }

    /**
     * Measures how long it take to compose and measure 10 checkboxes. This also includes the time
     * of composing the main wrapper, see [composeJustRootIntoActivity] for the overhead.
     */
    //@UiThreadTest
    //@Test
    fun composeCheckboxIntoActivity_10() {
        val state = benchmarkRule.getState()

        while (state.keepRunning()) {
            // This is possible due to the fact that composeInto is synchronous.
            activityRule.activity.setContent {
                CraneWrapper {
                    MaterialTheme {
                        Surface {
                            Column {
                                CreateCheckBox(times = 10)
                            }
                        }
                    }
                }
            }
            //activityRule.activity.disposeComposition()
        }
    }

    /**
     * Measures how long it take to compose and measure 30 checkboxes. This also includes the time
     * of composing the main wrapper, see [composeJustRootIntoActivity] for the overhead.
     */
    //@UiThreadTest
    //@Test
    fun composeCheckboxActivity_50() {
        val state = benchmarkRule.getState()

        while (state.keepRunning()) {
            // This is possible due to the fact that composeInto is synchronous.
            activityRule.activity.setContent {
                CraneWrapper {
                    MaterialTheme {
                        Surface {
                            Column {
                                CreateCheckBox(times = 50)
                            }
                        }
                    }
                }
            }
            //activityRule.activity.disposeComposition()
        }
    }

    @Composable
    fun CreateCheckBox(times: Int) {
        for (i in 0..times) {
            Checkbox(ToggleableState.Unchecked)
        }
    }

//    @Composable
//    fun CraneWrapper(@Children children: @Composable() () -> Unit) {
//        val rootRef = +memo { Ref<AndroidCraneView>() }
//
//        // TODO(nona): Tie the focus manger lifecycle to Window, otherwise FocusManager won't work with
//        //             nested AndroidCraneView case
//
//
////        <AndroidCraneView ref=rootRef>
////            val reference = +compositionReference()
////            val rootLayoutNode = rootRef.value?.root ?: error("Failed to create root platform view")
////            val context = rootRef.value?.context ?: composer.composer.context
////            Compose.composeInto(container = rootLayoutNode, context = context, parent = reference) {
////                ContextAmbient.Provider(value = context) {
////                    DensityAmbient.Provider(value = Density(context)) {
////                        FocusManagerAmbient.Provider(value = focusManager) {
////                            TextInputServiceAmbient.Provider(value = rootRef.value?.textInputService) {
////                                children()
////                            }
////                        }
////                    }
////                }
////            }
////        </AndroidCraneView>
//
//            ContextAmbient.Provider(value = context) {
//                DensityAmbient.Provider(value = Density(context)) {
//                    FocusManagerAmbient.Provider(value = focusManager) {
//                        TextInputServiceAmbient.Provider(value = rootRef.value?.textInputService) {
//                            children()
//                        }
//                    }
//                }
//            }
//    }
}