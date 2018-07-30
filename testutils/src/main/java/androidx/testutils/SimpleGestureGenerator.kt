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
import java.util.ArrayList

/** One [MotionEvent] every approximately every 10 milliseconds */
const val MOTION_EVENT_INTERVAL_MILLIS: Int = 10

/**
 * Distance and time span necessary to produce a fling.
 *
 * Distance and time necessary to produce a fling for [MotionEvent]
 *
 * @property distance Distance between [MotionEvent]s in pixels for a fling.
 * @property time Time between [MotionEvent]s in milliseconds for a fling.
 */
data class FlingData(val distance: Int, val time: Int) {

    /**
     * @property velocity Velocity of fling in pixels per millisecond.
     */
    val velocity: Int = Math.round(distance.toFloat() / time)
}

data class MotionEventData(
    val eventTimeDelta: Int,
    val action: Int,
    val x: Float,
    val y: Float,
    val metaState: Int
)

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

fun MotionEventData.toMotionEvent(downTime: Long): MotionEvent = MotionEvent.obtain(
        downTime,
        this.eventTimeDelta + downTime,
        this.action,
        this.x,
        this.y,
        this.metaState)

/**
 * Produces fling data from a [Context].
 */
fun Context.getFlingData(): FlingData {
    val configuration = ViewConfiguration.get(this)
    val touchSlop = configuration.scaledTouchSlop
    val mMinimumVelocity = configuration.scaledMinimumFlingVelocity
    val mMaximumVelocity = configuration.scaledMaximumFlingVelocity

    val targetVelocitySeconds = (mMaximumVelocity + mMinimumVelocity) / 2
    val targetDistanceTraveled = touchSlop * 2
    val targetTimePassed = targetDistanceTraveled * 1000 / targetVelocitySeconds

    if (targetTimePassed < 1) {
        throw IllegalArgumentException("Flings must require some time")
    }

    return FlingData(targetDistanceTraveled, targetTimePassed)
}

fun FlingData.generateFlingMotionEvents(originX: Int, originY: Int, fingerDirection: Direction):
    List<MotionEventData> {

    val fromX = originX.toFloat()
    val fromY = originY.toFloat()
    val time = this.time
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

    val remainder = time % MOTION_EVENT_INTERVAL_MILLIS
    val numberOfInnerEvents = time / MOTION_EVENT_INTERVAL_MILLIS - if (remainder == 0) 1 else 0

    val motionEventData = ArrayList<MotionEventData>()
    motionEventData.add(MotionEventData(0, MotionEvent.ACTION_DOWN, fromX, fromY, 0))
    for (i in 0..(numberOfInnerEvents - 1)) {
        val timeDelta = (i + 1) * MOTION_EVENT_INTERVAL_MILLIS
        motionEventData.add(MotionEventData(timeDelta, MotionEvent.ACTION_MOVE, toX, toY, 0))
    }
    motionEventData.add(MotionEventData(time, MotionEvent.ACTION_MOVE, toX, toY, 0))
    motionEventData.add(MotionEventData(time, MotionEvent.ACTION_UP, toX, toY, 0))

    return motionEventData
}

/**
 * Dispatches an array of [MotionEvent] to a [View].
 */
fun View.dispatchMotionEventsToView(downTime: Long, motionEventData: List<MotionEventData>) {
    for (motionEventDataItem in motionEventData) {
        this.dispatchTouchEvent(motionEventDataItem.toMotionEvent(downTime))
    }
}

/**
 * Simulates a fling on a [View].
 */
fun View.simulateFling(
    context: Context,
    downTime: Long,
    originX: Int,
    originY: Int,
    direction: Direction
) {
    this.dispatchMotionEventsToView(downTime,
            context.getFlingData().generateFlingMotionEvents(originX, originY, direction))
}
