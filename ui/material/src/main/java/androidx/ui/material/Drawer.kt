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

import androidx.animation.AnimatedFloat
import androidx.animation.ExponentialDecay
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.onCommit
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
import androidx.ui.core.WithConstraints
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.min
import androidx.ui.core.px
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
import kotlin.math.max

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
        WithConstraints { pxConstraints ->
            val constraints = +withDensity {
                DpConstraints(pxConstraints)
            }
            val info = +memo(pxConstraints.maxWidth) {
                ModalDragInfo(pxConstraints.maxWidth.value.toFloat())
            }
            val fling = +memo(pxConstraints.maxWidth, onStateChange) {
                ModalDrawerFlinger(pxConstraints.maxWidth.value.toFloat(), onStateChange)
            }

            val startValue = if (drawerState == DrawerState.Opened) info.maxBound else info.minBound
            AnimatedDraggable(info, startValue, fling) { animatedValue ->
                val fraction = calculateFraction(info.minBound, info.maxBound, animatedValue.value)
                val scrimAlpha = fraction * ScrimDefaultOpacity
                val dpOffset = +withDensity {
                    animatedValue.value.toDp()
                }

                Stack {
                    aligned(TopLeft) {
                        children()
                        // TODO: add click to request state change when b/134178145 will be fixed
                        Scrim(scrimAlpha)
                        DrawerContent(dpOffset, constraints, drawerContent)
                    }
                }
            }
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
        WithConstraints { pxConstraints ->
            val constraints = +withDensity {
                DpConstraints(pxConstraints)
            }
            val info = +memo(pxConstraints.maxHeight) {
                BottomDragInfo(pxConstraints.maxHeight.value.toFloat())
            }
            val fling = +memo(pxConstraints.maxHeight, onStateChange) {
                BottomDrawerFlinger(pxConstraints.maxHeight.value.toFloat(), onStateChange)
            }

            // TODO: add proper landscape support
            val isLandscape = constraints.maxWidth > constraints.maxHeight
            val openedValue = if (isLandscape) info.maxBound else lerp(
                info.minBound,
                info.maxBound,
                BottomDrawerOpenFraction
            )
            val startValue = if (drawerState == DrawerState.Opened) openedValue else info.maxBound

            AnimatedDraggable(info, startValue, fling) { animatedValue ->
                val fractionToOpened =
                    1 - max(0f, calculateFraction(openedValue, info.maxBound, animatedValue.value))
                val scrimAlpha = fractionToOpened * ScrimDefaultOpacity
                val dpOffset = +withDensity {
                    animatedValue.value.toDp()
                }
                Stack {
                    aligned(TopLeft) {
                        children()
                        // TODO: add click to request state change when b/134178145 will be fixed
                        Scrim(scrimAlpha)
                        BottomDrawerContent(dpOffset, constraints, drawerContent)
                    }
                }
            }
        }
    }
}

// start : Items to put inside drawer as content

@Composable
fun DrawerHeader(
    title: @Composable() () -> Unit,
    subtitle: @Composable() (() -> Unit) = {}
) {
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
            val titleStyle = +themeTextStyle { h6 }
            val subStyle = +themeTextStyle { subtitle2.copy(color = SubtitleTextColor) }
            CurrentTextStyleProvider(titleStyle) {
                title()
            }
            CurrentTextStyleProvider(subStyle) {
                subtitle()
            }
        }
    }
}

@Composable
fun DrawerVerticalSpace() {
    HeightSpacer(DrawerVerticalSpace)
}

@Composable
fun DrawerSectionLabel(text: @Composable() () -> Unit) {
    Container(
        height = DrawerSectionLabelHeight,
        alignment = Alignment.BottomLeft,
        expanded = true,
        padding = EdgeInsets(left = DrawerContentPadding, right = DrawerContentPadding)
    ) {
        CurrentTextStyleProvider(+themeTextStyle { subtitle2.copy(color = SubtitleTextColor) }) {
            text()
        }
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
        if (selected) primary.copy(alpha = SelectedItemDefaultOpacity) else Color.Transparent
    }
    val textStyle = +themeTextStyle {
        val style = body1
        if (selected) style.copy(color = +themeColor { primary })
        else body1
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

@Composable
private fun DrawerContent(
    xOffset: Dp,
    constraints: DpConstraints,
    child: @Composable() () -> Unit
) {
    WithOffset(xOffset = xOffset) {
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
    WithOffset(yOffset = yOffset) {
        Container(constraints = constraints) {
            // remove Container when we will support multiply children
            Surface { Container { child() } }
        }
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) = (pos - a) / (b - a)

private interface Flinger {
    fun fling(animation: AnimatedFloat, velocity: Float)
}

private data class DragInfo(
    val minBound: Float,
    val maxBound: Float,
    val positionToAxis: (PxPosition) -> Px,
    val axisToPosition: (Float) -> PxPosition,
    val isDraggableInDirection: (direction: Direction, currentValue: Float) -> Boolean
)

@Composable
private fun AnimatedDraggable(
    dragInfo: DragInfo,
    startValue: Float,
    fling: Flinger,
    @Children children: @Composable() (AnimatedFloat) -> Unit
) {
    val offset = (+animatedFloat(startValue)).apply {
        setBounds(dragInfo.minBound, dragInfo.maxBound)
    }
    +onCommit(startValue) {
        offset.animateTo(startValue, AnimationBuilder)
    }
    DragGestureDetector(
        canDrag = { direction ->
            dragInfo.isDraggableInDirection(direction, offset.value)
        },
        dragObserver = object : DragObserver {

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val draggedFraction = dragInfo.positionToAxis(dragDistance).value
                val newValue =
                    (offset.value + draggedFraction)
                        .coerceIn(dragInfo.minBound, dragInfo.maxBound)
                val consumed = newValue - offset.value
                offset.snapTo(newValue)
                return dragInfo.axisToPosition(consumed)
            }

            override fun onStop(velocity: PxPosition) {
                fling.fling(offset, dragInfo.positionToAxis(velocity).value)
            }
        }
    ) {
        children(offset)
    }
}

private fun ModalDragInfo(width: Float): DragInfo {
    val min = -width
    val max = 0f
    return DragInfo(
        minBound = min,
        maxBound = max,
        positionToAxis = { it.x },
        axisToPosition = { PxPosition(it.px, 0.px) },
        isDraggableInDirection = { direction, currentValue ->
            when (direction) {
                Direction.RIGHT -> currentValue <= max
                Direction.LEFT -> currentValue >= min
                else -> false
            }
        }
    )
}

private fun BottomDragInfo(height: Float): DragInfo {
    val min = 0f
    val max = height
    return DragInfo(
        minBound = min,
        maxBound = max,
        positionToAxis = { it.y },
        axisToPosition = { PxPosition(0.px, it.px) },
        isDraggableInDirection = { direction, currentValue ->
            when (direction) {
                Direction.UP -> currentValue <= max
                Direction.DOWN -> currentValue >= min
                else -> false
            }
        }

    )
}

private fun BottomDrawerFlinger(height: Float, onStateChange: (DrawerState) -> Unit) =
    object : Flinger {
        val openValue = lerp(0f, height, BottomDrawerOpenFraction)
        val expandedValue = 0f
        val closedValue = height

        override fun fling(animation: AnimatedFloat, velocity: Float) {
            animation.fling(
                startVelocity = velocity,
                decay = DefaultDecay,
                adjustTarget = adjustTarget(animation),
                onFinished = onFinished(animation)
            )
        }

        fun onFinished(animation: AnimatedFloat) = { _: Boolean ->
            if (animation.value >= closedValue) onStateChange(DrawerState.Closed)
            else if (animation.value <= openValue) onStateChange(DrawerState.Opened)
        }

        fun adjustTarget(animation: AnimatedFloat) = { targetToAdjust: Float ->
            val target =
                if (targetToAdjust > openValue / 2) closedValue
                else if (animation.value <= openValue && targetToAdjust < openValue) expandedValue
                else openValue
            TargetAnimation(target, AnimationBuilder)
        }
    }

private fun ModalDrawerFlinger(width: Float, onStateChange: (DrawerState) -> Unit) =
    object : Flinger {
        val openValue = 0f
        val closedValue = -width

        override fun fling(animation: AnimatedFloat, velocity: Float) {
            animation.fling(
                startVelocity = velocity,
                decay = DefaultDecay,
                adjustTarget = adjustTarget,
                onFinished = onFinished(animation)
            )
        }

        fun onFinished(animation: AnimatedFloat) = { _: Boolean ->
            if (animation.value <= closedValue) onStateChange(DrawerState.Closed)
            else if (animation.value >= openValue) onStateChange(DrawerState.Opened)
        }

        val adjustTarget = { targetToAdjust: Float ->
            val target = if (targetToAdjust < openValue / 2) closedValue else openValue
            TargetAnimation(target, AnimationBuilder)
        }
    }

@Composable
private fun Scrim(opacity: Float) {
    ColoredRect(+themeColor { onSurface.copy(alpha = opacity) })
}

// TODO: consider make pretty and move to public
@Composable
private fun WithOffset(
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

// TODO: figure out default decay
private val DefaultDecay = ExponentialDecay(0.7f)
private val AnimationBuilder =
    PhysicsBuilder<Float>().apply {
        stiffness = DrawerStiffness
    }

private val BottomDrawerOpenFraction = 0.5f