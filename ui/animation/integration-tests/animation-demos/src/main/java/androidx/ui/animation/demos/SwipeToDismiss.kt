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
import androidx.animation.FastOutSlowInEasing
import androidx.animation.PhysicsBuilder
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
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
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.painting.Paint
import androidx.ui.painting.TextStyle
import kotlin.math.sign

class SwipeToDismiss : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                Column {
                        SwipeToDismiss()

                    Padding(40.dp) {
                        Text("Swipe up to dismiss", style = TextStyle(fontSize = 80f))
                    }
                }
            }
        }
    }

    val height = 1600f
    val itemHeight = 1600f * 2 / 3f
    val padding = 10f

    @Composable
    fun SwipeToDismiss() {
        val itemBottom = +memo { AnimatedFloat(height) }
        val index = +state { 0 }
        val itemWidth = +state { 0f }
        DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
            override fun onStart() {
                itemBottom.setBounds(0f, height)
                if (itemBottom.isFlinging && itemBottom.target < 100f) {
                    reset()
                }
            }

            private fun reset() {
                itemBottom.snapToValue(height)
                index.value--
                if (index.value < 0) {
                    index.value += colors.size
                }
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                itemBottom.snapToValue(itemBottom.target + dragDistance.y.value)
                return dragDistance
            }

            fun adjustTarget(velocity: Float): (Float) -> Float {
                return { target: Float ->
                    // The velocity is fast enough to fly off screen
                    if (target <= 0) {
                        target
                    } else {
                        var projectedTarget = target + sign(velocity) * 0.2f * height
                        if (projectedTarget < 0.6 * height) {
                            0f
                        } else {
                            height
                        }
                    }
                }
            }
            override fun onStop(velocity: PxPosition) {
                itemBottom.fling(velocity.y.value,
                    adjustTarget(velocity.y.value),
                    ExponentialDecay(3.0f),
                    targetAnimation = PhysicsBuilder(dampingRatio = 0.8f, stiffness = 300f),
                    onFinished = {
                        if (it && itemBottom.value == 0f) {
                            reset()
                        }
                    })
            }
        }) {
            val children = @Composable {
                val progress = 1 - itemBottom.value / height
                // TODO: this progress can be used to drive state transitions
                val alpha = 1f - FastOutSlowInEasing(progress)
                val horizontalOffset = progress * itemWidth.value
                drawLeftItems(horizontalOffset, itemWidth.value, itemHeight, index.value)
                drawDismissingItem(itemBottom.value, itemWidth.value, itemHeight, index.value + 1,
                    alpha)
            }
            Layout(children = children, layoutBlock = { _, constraints ->
                layout(constraints.maxWidth, IntPx(height.toInt())) {}
                itemWidth.value = constraints.maxWidth.value * 2 / 3f
            })
        }
    }

    @Composable
    fun drawLeftItems(horizontalOffset: Float, width: Float, height: Float, index: Int) {
        var paint = +state { Paint() }
        Draw { canvas, parentSize ->
            paint.value.color = colors[index % colors.size]
            val centerX = parentSize.width.value / 2
            val itemRect =
                Rect(centerX - width * 1.5f + horizontalOffset + padding,
                    parentSize.height.value - height,
                    centerX - width * 0.5f + horizontalOffset - padding,
                    parentSize.height.value)
            canvas.drawRect(itemRect, paint.value)

            if (itemRect.left >= 0) {
                // draw another item
                paint.value.color = colors[(index - 1 + colors.size) % colors.size]
                canvas.drawRect(itemRect.translate(-width, 0f), paint.value)
            }
        }
    }

    @Composable
    fun drawDismissingItem(bottom: Float, width: Float, height: Float, index: Int, alpha: Float) {
        var paint = +state { Paint() }
        Draw { canvas, parentSize ->
            paint.value.color = colors[index % colors.size]
            paint.value.alpha = alpha
            val centerX = parentSize.width.value / 2
            canvas.drawRect(
                Rect(centerX - width / 2 + padding,
                    bottom - height,
                    centerX + width / 2 - padding,
                    bottom),
                paint.value)
        }
    }

    private val colors = listOf(
        Color(0xFFffd7d7.toInt()),
        Color(0xFFffe9d6.toInt()),
        Color(0xFFfffbd0.toInt()),
        Color(0xFFe3ffd9.toInt()),
        Color(0xFFd0fff8.toInt()))
}
