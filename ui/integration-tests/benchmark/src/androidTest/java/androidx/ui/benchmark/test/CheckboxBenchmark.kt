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

import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.ComponentNode
import androidx.ui.core.Draw
import androidx.ui.core.DrawNode
import androidx.ui.core.Text
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.FlexRow
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.android.AndroidComposeTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class CheckboxBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val composeTestRule = AndroidComposeTestRule()

    @Test
    fun layoutPerformance() {
        setMaterialContent { CheckboxRow() }

        val view = composeTestRule.findAndroidCraneView()
        measureLayoutPerf(view)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun drawPerformance() {
        setMaterialContent { CheckboxRow() }

        val view = composeTestRule.findAndroidCraneView()
        measureDrawPerf(view)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun viewDrawPerformance() {
        createViewCheckBoxes()
        val activity = composeTestRule.activityTestRule.activity
        val view = activity.findViewById<View>(R.id.title)
        measureDrawPerf(view)
    }

    @Test
    fun viewLayoutPerformance() {
        createViewCheckBoxes()
        val activity = composeTestRule.activityTestRule.activity
        val view = activity.findViewById<View>(R.id.title)
        measureLayoutPerf(view)
    }

    private fun measureLayoutPerf(view: View) {
        composeTestRule.runOnUiThreadSync {
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
                    requestLayoutViews(view)
                }
                view.measure(widthSpec, heightSpec)
                view.layout(view.left, view.top, view.right, view.bottom)
            }
            benchmarkRule.measureRepeated(exec)
        }
    }

    private fun measureDrawPerf(view: View) {
        composeTestRule.runOnUiThreadSync {
            measureAndLayoutToCurrentSize(view)
            benchmarkRule.measureRepeated(drawWithRenderNode(view))
        }
    }

    private fun measureAndLayoutToCurrentSize(view: View) {
        val width = view.measuredWidth
        val height = view.measuredHeight
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(view.left, view.top, view.right, view.bottom)
    }

    private fun createViewCheckBoxes() {
        val activity = composeTestRule.activityTestRule.activity
        composeTestRule.runOnUiThreadSync {
            val column = LinearLayout(activity)
            column.orientation = LinearLayout.VERTICAL
            column.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            column.id = R.id.title
            repeat(10) {
                val row = LinearLayout(activity)
                row.orientation = LinearLayout.HORIZONTAL
                row.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val text = TextView(activity)
                text.text = "Check Me!"
                val layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.weight = 1f
                text.layoutParams = layoutParams
                val checkbox = CheckBox(activity)
                checkbox.isChecked = false
                checkbox.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                row.addView(text)
                row.addView(checkbox)
                column.addView(row)
            }
            activity.setContentView(column)
        }
    }

    private fun requestLayoutViews(view: View) {
        view.requestLayout()
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                requestLayoutViews(child)
            }
        }
    }

    fun setMaterialContent(composable: @Composable() () -> Unit) {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    composable()
                }
            }
        }
    }

    @Composable
    private fun CheckboxRow() {
        Column {
            for (i in 0..9) {
                FlexRow {
                    val checked = +state { false }
                    inflexible {
                        Draw { _, _ ->
                            println("Drawing!")
                        }
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

    companion object {
        // Extracted so that RenderNode doesn't cause problems with class validation
        private fun drawWithRenderNode(view: View): BenchmarkRule.Scope.() -> Unit {
            val renderNode = RenderNode("dummy")
            renderNode.setPosition(0, 0, view.width, view.height)
            val exec: BenchmarkRule.Scope.() -> Unit = {
                var canvas: Canvas? = null
                runWithTimingDisabled {
                    invalidateViews(view)
                    canvas = renderNode.beginRecording()
                }
                view.draw(canvas)
                runWithTimingDisabled {
                    renderNode.endRecording()
                }
            }
            return exec
        }

        private fun invalidateViews(view: View) {
            view.invalidate()
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    invalidateViews(child)
                }
            }
            if (view is AndroidCraneView) {
                invalidateComponentNodes(view.root)
            }
        }

        private fun invalidateComponentNodes(node: ComponentNode) {
            if (node is DrawNode) {
                node.invalidate()
            }
            node.visitChildren { child ->
                invalidateComponentNodes(child)
            }
        }
    }
}