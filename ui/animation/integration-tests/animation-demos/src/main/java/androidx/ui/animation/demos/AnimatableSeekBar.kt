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
import androidx.animation.AnimatedFloat
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.painting.BlendMode
import androidx.ui.painting.Paint

class AnimatableSeekBar : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                Padding(left = 10.dp, right = 10.dp) {
                    MovingTargetExample()
                }
            }
        }
    }

    @Composable
    fun MovingTargetExample() {
            val animValue = +state { AnimatedFloat(0f) }
            DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
                // TODO: feature request, on drag needs drag position as well
                // TODO: Drag is consistently off by a few pixels, needs to figure out why
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    android.util.Log.w("LTD", "drag delta: ${dragDistance.x.value}")
                    animValue.value.snapToValue(animValue.value.target + dragDistance.x.value)
                    return dragDistance
                }

            }) {
                PressGestureDetector(
                    onPress = { position ->
                        animValue.value.apply {
                            toValue(position.x.value) {
                                physics {
                                    // Expose the defaults in the companion class
                                    dampingRatio = 1.0f
                                    stiffness = 500f
                                }
                            }
                        }
                    },
//            onRelease = { animValue.value.toValue(0f) },
                    onCancel = { /* animValue.value.toValue(0f) */ }) {
                    val children = @Composable {
                        Recompose { recompose ->
                            DrawSeekBar(animValue.value.value)
                            // TODO: Trigger recomposition properly
                            animValue.value.onUpdate = recompose
                        }
                    }
                    Layout(children = children, layoutBlock = { _, constraints ->
                        layout(constraints.maxWidth, IntPx(200)) {}
                    })
                }
            }
        }

    @Composable
    fun DrawColors(scroll : Float) {
        var paint = +state { Paint() }
        android.util.Log.w("LTD", "Scroll = $scroll")
        Draw { canvas, parentSize ->
            paint.value.color = Color.Blue
            canvas.drawRect(Rect(0f, 0f, parentSize.width.value / 2f,
                parentSize.height.value), paint.value)
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
}
