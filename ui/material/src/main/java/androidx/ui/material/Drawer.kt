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

import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.animatedFloat
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Direction
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.RepaintBoundary
import androidx.ui.core.Text
import androidx.ui.core.WithConstraints
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.min
import androidx.ui.core.withDensity
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Alignment.Companion.TopLeft
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.HeightSpacer
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.MainAxisSize
import androidx.ui.layout.Stack
import androidx.ui.lerp
import androidx.ui.material.surface.Surface
import kotlin.math.sign

enum class DrawerState {
    Closed,
    Opened,
}

@Composable
fun ModalDrawer(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    drawerContent: @Composable() () -> Unit,
    @Children children: @Composable() () -> Unit
) {
    Container(expanded = true) {
        WithConstraints { constraints ->
            val dpConstraints = +withDensity {
                DpConstraints(constraints)
            }
            val dragSpec = +memo(constraints.maxWidth) {
                ModalDragInfo(constraints.maxWidth.value.toFloat())
            }
            DrawerImpl(
                drawerState, onStateChange, dragSpec,
                drawerContent = { dpOffset ->
                    DrawerContent(dpOffset, dpConstraints, drawerContent)
                },
                children = children
            )
        }
    }
}

@Composable
fun BottomDrawer(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    drawerContent: @Composable() () -> Unit,
    @Children children: @Composable() () -> Unit
) {
    Container(expanded = true) {
        WithConstraints { constraints ->
            val dpConstraints = +withDensity {
                DpConstraints(constraints)
            }
            val dragSpec = +memo(constraints.maxHeight) {
                BottomDragInfo(constraints.maxHeight.value.toFloat())
            }
            DrawerImpl(
                drawerState, onStateChange, dragSpec,
                drawerContent = { dpOffset ->
                    BottomDrawerContent(dpOffset, dpConstraints, drawerContent)
                },
                children = children
            )
        }
    }
}

// start : Items to put inside drawer as content

@Composable
fun DrawerHeader(title: String, subtitle: String?) {
    // TODO (no baseline alignment, so rewrite it to material specs later)
    Container(
        height = DrawerHeaderHeight + DrawerVerticalSpace * 2,
        expanded = true,
        alignment = Alignment.CenterLeft,
        padding = EdgeInsets(
            left = DrawerContentPadding,
            right = DrawerContentPadding,
            top = DrawerVerticalSpace,
            bottom = DrawerVerticalSpace
        )
    ) {
        Column(
            mainAxisSize = MainAxisSize.Min,
            mainAxisAlignment = MainAxisAlignment.Center,
            crossAxisAlignment = CrossAxisAlignment.Start
        ) {
            Text(title, style = +themeTextStyle { h6 })
            if (subtitle != null) {
                val subStyle = +themeTextStyle { subtitle2.copy(color = SubtitleTextColor) }
                Text(subtitle, subStyle)
            }
        }
    }
}

@Composable
fun DrawerVerticalSpace() {
    HeightSpacer(DrawerVerticalSpace)
}

@Composable
fun DrawerSectionLabel(label: String) {
    Container(
        height = DrawerSectionLabelHeight,
        alignment = Alignment.BottomLeft,
        expanded = true,
        padding = EdgeInsets(left = DrawerContentPadding, right = DrawerContentPadding)
    ) {
        Text(label, +themeTextStyle { subtitle2.copy(color = SubtitleTextColor) })
    }
}

@Composable
fun NavigationDrawerItem(
    selected: Boolean,
    onSelect: () -> Unit,
    text: @Composable() () -> Unit
) {
    // TODO: Matvei made this up, change to proper specs later
    val surfaceColor = +themeColor {
        if (selected) primary.copy(alpha = SelectedItemDefaultOpacity) else surface
    }
    val textStyle = +themeTextStyle {
        body1.copy(
            color = if (selected) +themeColor { primary } else body1.color
        )
    }
    Container(
        height = DrawerItemHeight,
        padding = EdgeInsets(
            left = ItemBackgroundHorizontalPadding,
            right = ItemBackgroundHorizontalPadding,
            top = ItemBackgroundVerticalPadding,
            bottom = ItemBackgroundVerticalPadding
        )
    ) {
        Button(color = surfaceColor, onClick = onSelect) {
            Container(
                alignment = Alignment.CenterLeft,
                expanded = true,
                padding = EdgeInsets(left = ItemContentPadding, right = ItemContentPadding)
            ) {
                CurrentTextStyleProvider(textStyle) {
                    text()
                }
            }
        }
    }
}

// end : Items to put inside drawer as content

private data class DragInfo(
    val minValue: Float,
    val maxValue: Float,
    val openedValue: Float,
    val axisChoice: (PxPosition) -> Px,
    val draggableDirections: List<Direction>
)

private fun ModalDragInfo(width: Float) = DragInfo(
    minValue = -width,
    maxValue = 0f,
    openedValue = 0f,
    axisChoice = { it.x },
    draggableDirections = listOf(Direction.RIGHT, Direction.LEFT)
)

private fun BottomDragInfo(height: Float) = DragInfo(
    minValue = height,
    maxValue = 0f,
    openedValue = height / 2,
    axisChoice = { it.y },
    draggableDirections = listOf(Direction.UP, Direction.DOWN)
)

@Composable
private fun DrawerImpl(
    state: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    dragInfo: DragInfo,
    drawerContent: @Composable() (Dp) -> Unit,
    @Children children: @Composable() () -> Unit
) {
    DrawerDraggable(state, onStateChange, dragInfo) { openFraction ->
        val scrimAlpha = openFraction * ScrimDefaultOpacity
        val dpOffset = +withDensity {
            lerp(dragInfo.minValue, dragInfo.maxValue, openFraction).toDp()
        }

        Stack {
            aligned(TopLeft) {
                children()
            }
            aligned(TopLeft) {
                // TODO: add clickable here to close when b/134178145 will be fixed
                Scrim(scrimAlpha)
            }
            aligned(TopLeft) {
                drawerContent(dpOffset)
            }
        }
    }
}

@Composable
private fun DrawerContent(
    xOffset: Dp,
    constraints: DpConstraints,
    child: @Composable() () -> Unit
) {
    WithXOffset(xOffset = xOffset) {
        Container(
            constraints = constraints,
            padding = EdgeInsets(right = VerticalDrawerPadding)
        ) {
            // remove Container when we will support multiply children
            Surface { Container { child() } }
        }
    }
}

@Composable
private fun BottomDrawerContent(
    yOffset: Dp,
    constraints: DpConstraints,
    child: @Composable() () -> Unit
) {
    WithXOffset(yOffset = yOffset) {
        Container(constraints = constraints) {
            // remove Container when we will support multiply children
            Surface { Container { child() } }
        }
    }
}

private fun reverseLerp(a: Float, b: Float, pos: Float) = (pos - a) / (b - a)

@Composable
private fun DrawerDraggable(
    state: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    dragInfo: DragInfo,
    @Children children: @Composable() (Float) -> Unit
) {
    val minFraction = 0f
    val maxFraction = 1f
    val openFraction = reverseLerp(dragInfo.minValue, dragInfo.maxValue, dragInfo.openedValue)

    val animationBuilder = +memo {
        PhysicsBuilder<Float>().apply {
            stiffness = DrawerStiffness
        }
    }
    val offsetByState = if (state == DrawerState.Opened) openFraction else minFraction
    val offset = (+animatedFloat(offsetByState)).apply {
        setBounds(minFraction, maxFraction)
    }
    +memo(offsetByState) {
        offset.animateTo(offsetByState, animationBuilder)
    }
    val onFinished = { _: Boolean ->
        if (offset.value <= minFraction) onStateChange(DrawerState.Closed)
        else if (offset.value >= openFraction) onStateChange(DrawerState.Opened)
    }

    fun adjustTarget(velocity: Float) = { targetToAdjust: Float ->
        // TODO : figure out right formula here
        val projected = targetToAdjust + sign(velocity) * 0.2f
        val target =
            if (projected < openFraction / 2) minFraction
            else if (offset.value > openFraction && targetToAdjust > openFraction) maxFraction
            else openFraction
        TargetAnimation(target, animationBuilder)
    }

    DragGestureDetector(
        canDrag = { direction -> dragInfo.draggableDirections.contains(direction) },
        dragObserver = object : DragObserver {

            fun scaleToFraction(absolute: Float) =
                absolute / (dragInfo.maxValue - dragInfo.minValue)

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val draggedFraction = scaleToFraction(dragInfo.axisChoice(dragDistance).value)
                offset.snapTo(offset.value + draggedFraction)
                return dragDistance
            }

            override fun onStop(velocity: PxPosition) {
                val scaledVelocity = scaleToFraction(dragInfo.axisChoice(velocity).value)
                offset.fling(
                    startVelocity = scaledVelocity,
                    adjustTarget = adjustTarget(scaledVelocity),
                    onFinished = onFinished
                )
            }
        }
    ) {
        children(offset.value)
    }
}

@Composable
private fun Scrim(opacity: Float) {
    ColoredRect(+themeColor { onSurface.copy(alpha = opacity) })
}

// TODO: consider make pretty and move to public
@Composable
private fun WithXOffset(
    xOffset: Dp = 0.dp,
    yOffset: Dp = 0.dp,
    @Children child: @Composable() () -> Unit
) {
    Layout(children = {
        RepaintBoundary {
            child()
        }
    }, layoutBlock = { measurables, constraints ->
        if (measurables.size > 1) {
            throw IllegalStateException("Only one child is allowed")
        }
        val childMeasurable = measurables.firstOrNull()
        val placeable = childMeasurable?.measure(constraints)
        val width: IntPx
        val height: IntPx
        if (placeable == null) {
            width = constraints.minWidth
            height = constraints.minHeight
        } else {
            width = min(placeable.width, constraints.maxWidth)
            height = min(placeable.height, constraints.maxHeight)
        }
        layout(width, height) {
            placeable?.place(xOffset.toIntPx(), yOffset.toIntPx())
        }
    })
}

private val ScrimDefaultOpacity = 0.32f
private val VerticalDrawerPadding = 56.dp

// drawer children specs
private val DrawerVerticalSpace = 8.dp
private val DrawerContentPadding = 16.dp
private val DrawerSectionLabelHeight = 28.dp
private val DrawerHeaderHeight = 64.dp
private val DrawerItemHeight = 48.dp
private val DrawerStiffness = 1000f
// not in specs, clarify
private val ItemBackgroundVerticalPadding = 4.dp
private val ItemBackgroundHorizontalPadding = 8.dp
private val ItemContentPadding = DrawerContentPadding - ItemBackgroundHorizontalPadding
private val SelectedItemDefaultOpacity = 0.2f
private val SubtitleTextColor = Color(0x8a000000.toInt())
