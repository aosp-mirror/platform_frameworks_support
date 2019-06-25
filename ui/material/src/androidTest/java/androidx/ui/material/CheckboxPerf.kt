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

package androidx.ui.material

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.LargeTest
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.Text
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.FlexRow
import androidx.ui.test.android.AndroidComposeTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class CheckboxPerf {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val composeTestRule = AndroidComposeTestRule()

    @Test
    fun layoutPerformance() {
        composeTestRule.setMaterialContent { CheckboxRow() }

        composeTestRule.runOnUiThreadSync {
            val view = findAndroidCraneView()
            val width = view.measuredWidth
            val height = view.measuredHeight
            var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            view.layout(view.left, view.top, view.right, view.bottom)

            var lastWidth = width
            var lastHeight: Int
            val exec: BenchmarkRule.Scope.() -> Unit = {
                runWithTimingDisabled {
                    if (lastWidth == width) {
                        lastWidth = width - 10
                        lastHeight = height - 10
                    } else {

                        lastWidth = width
                        lastHeight = height
                    }
                    widthSpec =
                        View.MeasureSpec.makeMeasureSpec(lastWidth, View.MeasureSpec.EXACTLY)
                    heightSpec =
                        View.MeasureSpec.makeMeasureSpec(lastHeight, View.MeasureSpec.EXACTLY)
                    view.measure(widthSpec, heightSpec)
                }
                view.layout(view.left, view.top, view.right, view.bottom)
            }
            benchmarkRule.measureRepeated(exec)
        }
    }

    internal fun findAndroidCraneView(): AndroidCraneView {
        return findAndroidCraneView(composeTestRule.activityTestRule.activity)
    }

    internal fun findAndroidCraneView(activity: Activity): AndroidCraneView {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findAndroidCraneView(contentViewGroup)!!
    }

    internal fun findAndroidCraneView(parent: ViewGroup): AndroidCraneView? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is AndroidCraneView) {
                return child
            } else if (child is ViewGroup) {
                val craneView = findAndroidCraneView(child)
                if (craneView != null) {
                    return craneView
                }
            }
        }
        return null
    }
}

@Composable
private fun CheckboxRow() {
    Column {
        for (i in 0..9) {
            FlexRow {
                val checked = +state { false }
                inflexible {
                    Text(text = "Check Me!")
                }
                expanded(1f) {
                    Align(alignment = Alignment.CenterRight) {
                        Checkbox(
                            checked = checked.value,
                            onCheckedChange = { checked.value = !checked.value }
                        )
                    }
                }
            }
        }
    }
}
