/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.testutils

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

data class FlingData(val velocityMillis: Int, val distance: Int, val timeMillis: Int)

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

fun Context.getFlingData(): FlingData {
    val configuration = ViewConfiguration.get(this)
    val touchSlop = configuration.scaledTouchSlop
    val mMinimumVelocity = configuration.scaledMinimumFlingVelocity
    val mMaximumVelocity = configuration.scaledMaximumFlingVelocity

    val targetVelocitySeconds = (mMaximumVelocity - mMinimumVelocity) / 2 + mMinimumVelocity
    val targetDistanceTraveled = touchSlop * 2
    val targetTimePassed = targetDistanceTraveled * 1000 / targetVelocitySeconds

    return FlingData(targetVelocitySeconds, targetDistanceTraveled, targetTimePassed)
}

fun FlingData.generateFlingMotionEvents(originX: Int, originY: Int, fingerDirection: Direction):
        Array<MotionEvent> {

    val fromX = originX.toFloat()
    val fromY = originY.toFloat()
    val time = this.timeMillis.toLong()
    val toX = fromX + when (fingerDirection) {
        Direction.LEFT -> -this.distance
        Direction.RIGHT -> this.distance
        else -> 0
    }
    val toY = fromY + when (fingerDirection) {
        Direction.UP -> -this.distance
        Direction.DOWN -> this.distance
        else -> 0
    }

    val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, fromX, fromY, 0)
    val move = MotionEvent.obtain(0, time, MotionEvent.ACTION_MOVE, toX, toY, 0)
    val up = MotionEvent.obtain(0, time, MotionEvent.ACTION_UP, toX, toY, 0)

    return arrayOf(down, move, up)
}

fun View.dispatchMotionEventsToView(motionEvents: Array<MotionEvent>) {
    for (motionEvent in motionEvents) {
        this.dispatchTouchEvent(motionEvent)
    }
}

fun View.simulateFling(context: Context, originX: Int, originY: Int, direction: Direction) {
    this.dispatchMotionEventsToView(
            context.getFlingData().generateFlingMotionEvents(originX, originY, direction))
}
