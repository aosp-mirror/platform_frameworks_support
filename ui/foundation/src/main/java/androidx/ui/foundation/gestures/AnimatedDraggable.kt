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

package androidx.ui.foundation.gestures

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationBuilder
import androidx.animation.DecayAnimation
import androidx.animation.ExponentialDecay
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.animation.animatedFloat
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.px
import kotlin.math.abs
import kotlin.math.min

/**
 * Component that provides drag, fling and animation logic for one [Float] value.
 *
 * The common usecase for this component is when you need to be able to drag/scroll something
 * on the screen and also one or more of the following:
 * 1. Fling support when something is scrolled/dragged with velocity
 * 2. Stable anchors points support for dragging value,
 * e.g. be able to drag between only predefined set of values
 * 3. Automatic animation of draggable value, e.g emulate drag by click
 *
 * @see [AnimationAdjustment] to control anchors or fling intensity.
 *
 * This component provides high-level API and ownership for [AnimatedFloat]
 * and returns it as a parameter for its children.
 *
 * If you need only drag support without animations, consider using [DragGestureDetector] instead.
 *
 * If you need only animations without gesture support, consider using [animatedFloat] instead.
 *
 * @param startValue value to set as initial for draggable/animating value in this component
 * @param minValue lower bound for draggable/animating value in this component.
 * Use [Float.MIN_VALUE] if there's no lower bound
 * @param maxValue upped bound for draggable/animating value in this component
 * Use [Float.MAX_VALUE] if there's no upper bound
 * @param axisProjection projection from X and Y axis to draggable value.
 * Usually it's [AxisProjection.X] for horizontal drag and [AxisProjection.Y] for vertical drag.
 * @param onAnimationFinished callback to be invoked when animation finishes by fling
 * or being interrupted by gesture input.
 * Consider second boolean param "cancelled" to know what happened.
 * @param animationAdjustment adjustment strategy for draggable/animating value
 * when drag has ended and fling animation occurs.
 *
 */
@Composable
fun AnimatedDraggable(
    startValue: Float,
    minValue: Float,
    maxValue: Float,
    axisProjection: AxisProjection,
    onAnimationFinished: (finishValue: Float, cancelled: Boolean) -> Unit,
    animationAdjustment: AnimationAdjustment = DefaultAnimationAdjustment,
    children: @Composable() (AnimatedFloat) -> Unit
) {
    val animFloat = (+animatedFloat(startValue)).apply {
        setBounds(minValue, maxValue)
    }
    DragGestureDetector(
        canDrag = { direction ->
            axisProjection.isDraggableInDirection(direction, minValue, animFloat.value, maxValue)
        },
        dragObserver = object : DragObserver {

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val projected = axisProjection.project(dragDistance)
                val newValue = (animFloat.value + projected).coerceIn(minValue, maxValue)
                val consumed = newValue - animFloat.value
                animFloat.snapTo(newValue)
                val fractionConsumed = if (projected == 0f) 0f else consumed / projected
                return PxPosition(
                    axisProjection.xProjection(dragDistance.x).px * fractionConsumed,
                    axisProjection.yProjection(dragDistance.y).px * fractionConsumed
                )
            }

            override fun onStop(velocity: PxPosition) {
                val projected = axisProjection.project(velocity)
                animFloat.fling(
                    projected,
                    decay = animationAdjustment.decayAnimation,
                    onFinished = { onAnimationFinished(animFloat.value, it) },
                    adjustTarget = { target -> animationAdjustment.adjust(target) }
                )
            }
        }
    ) {
        children(animFloat)
    }
}

