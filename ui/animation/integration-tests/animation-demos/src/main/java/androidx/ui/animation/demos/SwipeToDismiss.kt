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
import android.util.Log
import androidx.animation.ExponentialDecay
import androidx.animation.FastOutLinearInEasing
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
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.painting.Paint
import androidx.ui.painting.TextStyle
import kotlin.math.sign

class SwipeToDismissDemo : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                Column() {
                    Padding(left = 100.dp, right = 100.dp) {
                        SwipeToDismiss()
                    }

                    Padding(40.dp) {
                        Text("Swipe up to dismiss", style = TextStyle(fontSize = 80f))
                    }
                }
            }
        }
    }

    val height = 1600f
    val offset = AnimatedFloat(0f)

    @Composable
    fun SwipeToDismiss() {
        offset.setBounds(-height, 0f)
        val index = +state { 0 }
        DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                offset.snapToValue(offset.target + dragDistance.y.value)
                android.util.Log.w("LTD", "drag delta: ${dragDistance.y.value}, offset: ${offset.value}")
                return dragDistance
            }

            fun adjustTarget(velocity: Float) : (Float) -> Float {
                return { target: Float ->
                    if (target <= -height) {
                        target
                    } else {
                        var projectedTarget = target + sign(velocity) * 0.2f * height
                        if (projectedTarget < -0.5 * height) {
                            -height
                        } else {
                            0f
                        }
                    }
                }
            }
            override fun onStop(velocity: PxPosition) {
                offset.fling(velocity.y.value,
                    adjustTarget(velocity.y.value),
                    ExponentialDecay(3.0f),
                    targetAnimation = physics {
                        dampingRatio = 1.0f
                        stiffness = 500f
                    }, onFinished = {
                        Log.w("LTD", "Finished. offset: ${offset.value}")
                        if (it || offset.value <= (-height)) {
                            offset.snapToValue(0f)
                            index.value++
                        }
                    })
            }
            } ) {
            val children = @Composable {
                val alpha = FastOutLinearInEasing.invoke(1f + offset.value / height).coerceIn(0f, 1f)
                drawItem(offset.value, index.value, alpha)
            }
            Layout(children = children, layoutBlock = { _, constraints ->
                layout(constraints.maxWidth, IntPx(height.toInt())) {}
            })
        }
    }

    @Composable
    fun drawItem(offset: Float, index: Int, alpha: Float) {
        android.util.Log.w("LTD", "drawing item at offset: $offset")
        var paint = +state { Paint() }
        Draw { canvas, parentSize ->
            paint.value.color = colors[(index + 1) % colors.size]
            paint.value.alpha = alpha
            canvas.drawRect(
                Rect(0f, 400 + offset, parentSize.width.value,
                    1600 + offset),
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
