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

package androidx.viewpager2.widget.swipe

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

class WaitForMotionEventInjectCapability : ViewAction {
    override fun getDescription(): String {
        return "wait until motion events are accepted"
    }

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isCompletelyDisplayed()
    }

    override fun perform(uiController: UiController?, view: View?) {
        if (uiController == null || view == null) {
            throw IllegalArgumentException("UiController and View must be non-null")
        }
        while (!tryTouch(uiController, view)) {
            uiController.loopMainThreadForAtLeast(10)
        }
    }

    private fun tryTouch(uiController: UiController, view: View): Boolean {
        val now = SystemClock.uptimeMillis()
        val coord = GeneralLocation.CENTER.calculateCoordinates(view)
        val events = mutableListOf<MotionEvent>()

        try {
            events.add(obtainDownEvent(now, coord))
            events.add(obtainCancelEvent(now))

            if (!uiController.injectMotionEventSequence(events)) {
                throw RuntimeException("Injection of motion events failed")
            }

            return true
        } catch (t: Throwable) {
            return false
        } finally {
            for (event in events) {
                event.recycle()
            }
        }
    }

    private fun obtainDownEvent(time: Long, coord: FloatArray): MotionEvent {
        return MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, coord[0], coord[1], 0)
    }

    private fun obtainCancelEvent(time: Long): MotionEvent {
        return MotionEvent.obtain(time, time, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
    }
}