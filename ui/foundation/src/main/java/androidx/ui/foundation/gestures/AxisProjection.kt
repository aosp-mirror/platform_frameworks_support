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

import androidx.ui.core.Direction
import androidx.ui.core.Px
import androidx.ui.core.PxPosition

/**
 * Axis projections is the projection from X and Y axis to value from [AnimatedDraggable].
 *
 * [xProjection] and [xProjection] defines what amount of corresponding axis to take, and
 * [project] function defines how to calculate result value from the [PxPosition]
 * which represents dragged distance.
 *
 * [isDraggableInDirection] is a function that calculated from the given min and max bounds and
 * current value whether or not drag is possible for certain [Direction].
 */
// TODO: add angle projection or something else which is more flexible than X and Y only
sealed class AxisProjection {

    internal abstract val xProjection: (Px) -> Float
    internal abstract val yProjection: (Px) -> Float
    internal abstract val isDraggableInDirection: (
        direction: Direction,
        minValue: Float,
        currentValue: Float,
        maxValue: Float
    ) -> Boolean

    internal fun project(pos: PxPosition) = xProjection(pos.x) + yProjection(pos.y)

    /**
     * X only axis projection, which takes 100% of X axis and 0% from Y.
     *
     * Use this for horizontal dragging in [AnimatedDraggable].
     */
    object X : AxisProjection() {
        override val xProjection: (Px) -> Float = { it.value }
        override val yProjection: (Px) -> Float = { 0f }
        override val isDraggableInDirection: (
            direction: Direction,
            minValue: Float,
            currentValue: Float,
            maxValue: Float
        ) -> Boolean =
            { direction, minValue, currentValue, maxValue ->
                when (direction) {
                    Direction.RIGHT -> currentValue <= maxValue
                    Direction.LEFT -> currentValue >= minValue
                    else -> false
                }
            }
    }

    /**
     * Y only axis projection, which takes 100% of Y axis and 0% from X.
     *
     * Use this for vertical dragging in [AnimatedDraggable].
     */
    object Y : AxisProjection() {
        override val xProjection: (Px) -> Float = { 0f }
        override val yProjection: (Px) -> Float = { it.value }
        override val isDraggableInDirection: (
            direction: Direction,
            minValue: Float,
            currentValue: Float,
            maxValue: Float
        ) -> Boolean =
            { direction, minValue, currentValue, maxValue ->
                when (direction) {
                    Direction.UP -> currentValue <= maxValue
                    Direction.DOWN -> currentValue >= minValue
                    else -> false
                }
            }
    }
}