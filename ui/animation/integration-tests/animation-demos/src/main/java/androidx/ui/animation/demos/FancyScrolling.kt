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
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.painting.Paint
import androidx.ui.painting.TextStyle
import kotlin.math.roundToInt

class FancyScrolling : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                FlingExample()
            }
        }
    }

    @Composable
    fun FlingExample() {
        Column {
            Padding(40.dp) {
                Text("<== Scroll horizontally ==>", style = TextStyle(fontSize = 80f))
            }
            // TODO: This has to be decalred outside of drag gesture, kind of awkward,
            //  how can we do better here?
            val animScroll = AnimatedFloat(0f)
            val itemWidth = +state {0f}
                DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
                    override fun onDrag(dragDistance: PxPosition): PxPosition {
                        animScroll.snapToValue(animScroll.target + dragDistance.x.value)
                        return dragDistance
                    }
                    override fun onStop(velocity: PxPosition) {
                        Log.w("LTD", "start decaying + ${velocity.x.value}")
                        animScroll.decay(velocity.x.value) { finished ->
                            if (finished && itemWidth.value > 0) {
                                Log.w("LTD", "end animScroll.value for animScroll ${animScroll.value}, end velocity: }")
                                var rem = animScroll.value % itemWidth.value
                                if (rem < 0) {
                                    rem += itemWidth.value
                                }
                                val end = animScroll.value + itemWidth.value / 2 - rem
                                animScroll.toValue(end) {
                                    physics {
                                        stiffness = 1000f
                                        dampingRatio = 1.0f
                                    }
                                }
                            }
                        }
                    }
                }) {
                    val children = @Composable {
                        val scroll = animScroll.value
                        var paint = +state { Paint() }
                        Draw { canvas, parentSize ->
                            val width = parentSize.width.value / 2f
                            itemWidth.value = width
                            var startingPos = scroll % width
                            if (startingPos > 0) {
                                startingPos -= width
                            }
                            var startingColorIndex = ((scroll - startingPos) / width).roundToInt().rem(colors.size)
                            if (startingColorIndex < 0) {
                                startingColorIndex += colors.size
                            }
                            android.util.Log.w("LTD", "Scroll index = $startingColorIndex")
                            paint.value.color = colors[startingColorIndex]
                            canvas.drawRect(Rect(startingPos, 0f, startingPos + width,
                                parentSize.height.value), paint.value)
                            paint.value.color = colors[(startingColorIndex + colors.size - 1) % colors.size]
                            canvas.drawRect(Rect(startingPos + width, 0f, startingPos + width * 2,
                                parentSize.height.value), paint.value)
                            paint.value.color = colors[(startingColorIndex + colors.size - 2) % colors.size]
                            canvas.drawRect(Rect(startingPos + width * 2, 0f, startingPos + width * 3,
                                parentSize.height.value), paint.value)
                        }
                    }
                    Layout(children = children, layoutBlock = { _, constraints ->
                        layout(constraints.maxWidth, IntPx(800)) {}
                    })
                }
        }
    }

    private val colors = listOf(Color.Red, Color(0xFFFFA500.toInt()), Color.Yellow, Color.Green,
        Color.Cyan, Color.Blue, Color.Purple)

}
