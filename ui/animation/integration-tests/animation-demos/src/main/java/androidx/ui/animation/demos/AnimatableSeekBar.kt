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

package androidx.ui.animation.demos

import android.app.Activity
import android.os.Bundle
import androidx.animation.ExponentialDecay
import androidx.animation.physics
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.animation.AnimatedFloat
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.painting.Paint
import androidx.ui.painting.TextStyle
import kotlin.math.sign

class AnimatableSeekBar : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                Column {
                    Padding(40.dp) {
                        Text("Drag or tap on the slider", style = TextStyle(fontSize = 80f))
                    }

                    Padding(left = 10.dp, right = 10.dp, bottom = 30.dp) {
                        MovingTargetExample()
                    }

                    Padding(40.dp) {
                        Text("Swipe right to dismiss", style = TextStyle(fontSize = 80f))
                    }

                    Padding {
                        SwipeToDismiss()
                    }
                }
            }
        }
    }

    @Composable
    fun MovingTargetExample() {
            val animValue = AnimatedFloat(0f)
            DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
                // TODO: feature request, on drag needs drag position as well
                // TODO: Drag is consistently off by a few pixels, needs to figure out why
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    android.util.Log.w("LTD", "drag delta: ${dragDistance.x.value}")
                    animValue.snapToValue(animValue.target + dragDistance.x.value)
                    return dragDistance
                }

            }) {
                PressGestureDetector(
                    onPress = { position ->
                        animValue.toValue(position.x.value,
                            physics {
                                // Expose the defaults in the companion class
                                dampingRatio = 1.0f
                                stiffness = 500f
                            })
                    }) {
                    val children = @Composable {
                        DrawSeekBar(animValue.value)
                    }
                    Layout(children = children, layoutBlock = { _, constraints ->
                        layout(constraints.maxWidth, IntPx(200)) {}
                    })
                }
            }
        }

    @Composable
    fun DrawSeekBar(x: Float) {
        android.util.Log.w("LTD", "drawing seekbar: $x")
        var paint = +state { Paint() }
        Draw { canvas, parentSize ->
            //            val centerX = parentSize.width.value / 2
            val centerY = parentSize.height.value / 2
            // draw bar
            paint.value.color = Color.Gray
            canvas.drawRect(
                Rect(0f, centerY - 5, parentSize.width.value, centerY + 5),
                paint.value
            )
            paint.value.color = Color.Fuchsia
            canvas.drawRect(
                Rect(0f, centerY - 5, x, centerY + 5),
                paint.value
            )
            // draw ticker
            canvas.drawCircle(
                Offset(x, centerY), 40f, paint.value
            )
        }
    }

    @Composable
    fun SwipeToDismiss() {
        val offset = AnimatedFloat(0f)
        val width = +state { 0f }
        val index = +state { 0 }
        DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
            override fun onStart() {
                if (offset.value >= width.value ||
                    (offset.target >= width.value && width.value - offset.value <= 10f )) {
                    android.util.Log.w("LTD", "Snapping offset to 0 ")
                    offset.snapToValue(0f)
                    index.value++
                }
            }
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                android.util.Log.w("LTD", "drag delta: ${dragDistance.x.value}")
                offset.snapToValue(offset.value + dragDistance.x.value)
                return dragDistance
            }

            fun adjustTarget(velocity: Float) : (Float) -> Float {
                return { target: Float ->
                    if (target <= 0 || target >= width.value) {
                        target
                    } else {
                        var projectedTarget = target + sign(velocity) * 0.2f * width.value
                        if (projectedTarget > 0.5 * width.value) {
                            width.value
                        } else {
                            0f
                        }
                    }
                }
            }
            override fun onStop(velocity: PxPosition) {
                offset.setBounds(0f, width.value)
                android.util.Log.w("LTD", "width = ${width.value}")
                offset.fling(velocity.x.value,
                    adjustTarget(velocity.x.value),
                    ExponentialDecay(3.0f),
                    targetAnimation = physics {
                    dampingRatio = 1.0f
                    stiffness = 200f
                })
            }}) {
                val children = @Composable {
                    drawItem(offset.value, index.value)
                }
                Layout(children = children, layoutBlock = { _, constraints ->
                    layout(constraints.maxWidth, IntPx(400)) {}
                    width.value = constraints.maxWidth.value.toFloat()
                })
            }
    }

    @Composable
    fun drawItem(offset: Float, index: Int) {
        android.util.Log.w("LTD", "drawing item at offset: $offset")
        var paint = +state { Paint() }
        Draw { canvas, parentSize ->
            if (offset > 0) {
                paint.value.color = colors[(index + 1) % colors.size]
                canvas.drawRect(
                    Rect(0f, 0f, parentSize.width.value, parentSize.height.value),
                    paint.value
                )
            }
                paint.value.color = colors[index % colors.size]
                canvas.drawRect(
                    Rect(offset, 0f, parentSize.width.value, parentSize.height.value),
                    paint.value
                )

        }
    }

    private val colors = listOf(
        Color(0xFFff5858.toInt()),
        Color(0xFFff9358.toInt()),
        Color(0xFFf5ff58.toInt()),
        Color(0xFF77ff58.toInt()),
        Color(0xFF58d9ff.toInt()),
        Color(0xFF2b55f7.toInt()),
        Color(0xFFb300b3.toInt()))
}
