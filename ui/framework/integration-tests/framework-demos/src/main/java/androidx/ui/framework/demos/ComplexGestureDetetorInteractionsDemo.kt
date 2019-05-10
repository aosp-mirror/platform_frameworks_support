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

package androidx.ui.framework.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Direction
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle

/* Demo app created to study the interaction of animations, gestures and semantics. */
class ComplexGestureDetetorInteractionsDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                // Outer composable that scrolls
                DragGestureDetectorClient {
                    Items(3) {
                        Container(398.dp, 72.dp) {
                            // Inner composable that scrolls
                            DragGestureDetectorClient {
                                Items(5) {
                                    // Composable that indicates it is being pressed
                                    PressIndicatorGestureDetectorClient(
                                        72.dp,
                                        Color(0xFFFFFFFF.toInt()),
                                        Color(0x1f000000)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DragGestureDetectorClient(@Children children: @Composable() () -> Unit) {
    val offset = +state { 0.px }
    val maxOffset = +state { 0.px }

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: PxPosition): PxPosition {
            val resultingOffset = offset.value + dragDistance.y
            val dyToConsume =
                if (resultingOffset > 0.px) {
                    0.px - offset.value
                } else if (resultingOffset < maxOffset.value) {
                    maxOffset.value - offset.value
                } else {
                    dragDistance.y
                }
            offset.value = offset.value + dyToConsume
            return PxPosition(0.px, dyToConsume)
        }
    }

    val canDrag = { direction: Direction ->
        when (direction) {
            Direction.UP -> true
            Direction.DOWN -> true
            else -> false
        }
    }

    DragGestureDetector(canDrag, dragObserver) {
        Layout(children = {
            Draw { canvas, parentSize ->
                canvas.save()
                canvas.clipRect(parentSize.toRect())
            }
            children()
            Draw { canvas, _ ->
                canvas.restore()
            }
        }, layoutBlock = { measurables, constraints ->
            val placeable =
                measurables.first()
                    .measure(constraints.copy(minHeight = 0.ipx, maxHeight = IntPx.Infinity))

            maxOffset.value = constraints.maxHeight.value.px - placeable.height

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(0.ipx, offset.value.round())
            }
        })
    }
}

@Composable
fun PressIndicatorGestureDetectorClient(
    height: Dp,
    backgroundColor: Color,
    pressedBackgroundColor: Color
) {

    val pressed = +state { false }

    val onStart: (PxPosition) -> Unit = {
        pressed.value = true
    }

    val onStop = {
        pressed.value = false
    }

    val resolvedBackgroundColor =
        if (pressed.value) {
            pressedBackgroundColor
        } else {
            backgroundColor
        }

    val children = @Composable {
        Draw { canvas, parentSize ->
            val backgroundPaint = Paint().apply { this.color = resolvedBackgroundColor }
            canvas.drawRect(
                Rect(0f, 0f, parentSize.width.value, parentSize.height.value),
                backgroundPaint
            )
        }
    }

    PressIndicatorGestureDetector(onStart, onStop, onStop) {
        Layout(children) { _, constraints ->
            layout(
                constraints.maxWidth,
                height.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            ) {}
        }
    }
}

@Composable
fun Items(numItems: Int, row: @Composable() () -> Unit) {
    Column {
        for (i in 1..numItems) {
            row()
            if (i != numItems) {
                Divider(1.dp, Color.fromRGBO(0, 0, 0, .12f))
            }
        }
    }
}

@Composable
fun Container(maxHeight: Dp, padding: Dp, @Children children: @Composable() () -> Unit) {
    Layout({
        Padding(padding) {
            Border(color = Color.fromRGBO(0, 0, 0, .12f), width = 2.dp) {
                children()
            }
        }
    }, { measurables, constraints ->
        val newConstraints = constraints.copy(maxHeight = maxHeight.toIntPx())
        val placeable =
            measurables.first().measure(newConstraints)
        layout(newConstraints.maxWidth, newConstraints.maxHeight) {
            placeable.place(0.ipx, 0.ipx)
        }
    })
}

@Composable
fun Padding(padding: Dp, @Children children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val paddingPx = padding.toIntPx()
        val doublePadding = paddingPx * 2
        val maxWidth = constraints.maxWidth - doublePadding
        val maxHeight = constraints.maxHeight - doublePadding
        val minWidth =
            if (constraints.minWidth > maxWidth) {
                maxWidth
            } else {
                constraints.minWidth
            }
        val minHeight =
            if (constraints.minHeight > maxHeight) {
                maxHeight
            } else {
                constraints.minHeight
            }
        val placeable = measurables.first().measure(
            Constraints(minWidth, maxWidth, minHeight, maxHeight)
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(paddingPx, paddingPx)
        }
    }
}

@Composable
fun Border(color: Color, width: Dp, @Children children: @Composable() () -> Unit) {
    Layout(
        children = {
            children()
            Draw { canvas, parentSize ->

                val floatWidth = width.toPx().value

                val backgroundPaint = Paint().apply {
                    this.color = color
                    style = PaintingStyle.stroke
                    strokeWidth = floatWidth
                }
                canvas.drawRect(
                    Rect(
                        floatWidth / 2,
                        floatWidth / 2,
                        parentSize.width.value - floatWidth / 2 + 1,
                        parentSize.height.value - floatWidth / 2 + 1
                    ),
                    backgroundPaint
                )
            }
        },
        layoutBlock = { measurables, constraints ->
            val placeable = measurables.first().measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        })
}

@Composable
fun Column(@Children children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        var height = 0.ipx
        val placeables = measurables.map {
            val placeable = it.measure(
                constraints.copy(minHeight = 0.ipx, maxHeight = IntPx.Infinity)
            )
            height += placeable.height
            placeable
        }

        height = height.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constraints.maxWidth, height) {
            var currY = 0.ipx
            placeables.forEach {
                it.place(0.ipx, currY)
                currY += it.height
            }
        }
    }
}

@Composable
fun Divider(height: Dp, color: Color) {
    val children = @Composable {
        Draw { canvas, parentSize ->
            val backgroundPaint = Paint().apply { this.color = color }
            canvas.drawRect(
                Rect(0f, 0f, parentSize.width.value, parentSize.height.value),
                backgroundPaint
            )
        }
    }

    Layout(children) { _, constraints ->
        layout(
            constraints.maxWidth,
            height.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {}
    }
}