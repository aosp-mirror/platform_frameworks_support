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

package androidx.preference.tests

import android.app.Instrumentation
import android.content.DialogInterface
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.test.R
import androidx.preference.tests.helpers.PreferenceTestHelperActivity
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.PollingCheck
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [androidx.preference.ListPreference] dialog dismiss logic.
 * Previously dismissing the dialog while touch events were being dispatched could cause a crash,
 * this test ensures that this doesn't regress.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ListPreferenceDialogDismissTest {

    @get:Rule
    val activityRule = ActivityTestRule(PreferenceTestHelperActivity::class.java)

    private val tag = "androidx.preference.PreferenceFragment.DIALOG"

    private lateinit var fragment: PreferenceFragmentCompat
    private lateinit var instrumentation: Instrumentation
    private val dialog by lazy {
        (fragment.fragmentManager!!.findFragmentByTag(tag) as DialogFragment).dialog as AlertDialog
    }

    @Before
    @UiThreadTest
    fun setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        fragment = activityRule.activity.setupPreferenceHierarchy(
            R.xml.test_dialog
        )
    }

    @Test
    fun testDismissWithTwoPointers() {
        // Open ListPreference dialog
        instrumentation.waitForIdleSync()
        val preference: Preference = fragment.findPreference("dialog")!!
        preference.performClick()
        instrumentation.waitForIdleSync()

        // Cancel button in dialog
        val cancelButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val cancelButtonLocation = IntArray(2)
        cancelButton.getLocationOnScreen(cancelButtonLocation)

        // ListView containing the entries from the ListPreference
        val listViewLocation = IntArray(2)
        dialog.listView.getLocationOnScreen(listViewLocation)

        val coordinates = arrayOf(
            // Inside the cancel button
            MotionEvent.PointerCoords().apply {
                x = (cancelButtonLocation[0] + 1).toFloat()
                y = (cancelButtonLocation[1] + 1).toFloat()
            },
            // Inside the list of entries
            MotionEvent.PointerCoords().apply {
                x = (listViewLocation[0] + 1).toFloat()
                y = (listViewLocation[1] + 1).toFloat()
            })

        val downTime = SystemClock.uptimeMillis()

        // Press and hold cancel
        injectMotionEvent(MotionEvent.ACTION_DOWN, coordinates, 1, downTime)

        // Press and hold the entry inside the ListView
        val secondPointerDown = MotionEvent.ACTION_POINTER_DOWN +
                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        injectMotionEvent(secondPointerDown, coordinates, 2, downTime)

        // Currently, if the time difference between POINTER_DOWN and POINTER_UP is less than
        // ViewConfiguration.getTapTimeout(), then the click handler will not be invoked.
        var completed = false
        val condition = PollingCheck.PollingCheckCondition { completed }

        // Post to view's handler and wait until the message has been processed. This ensures that
        // anything that was posted prior to this event has been executed.
        cancelButton.postDelayed({ completed = true }, ViewConfiguration.getTapTimeout().toLong())
        PollingCheck.waitFor(condition)

        // Release the pointer holding the entry inside the ListView
        // Previously, releasing this pointer would trigger the click handler, which would
        // call "dialog.dismiss()", and thus result in nested calls of dispatchTouchEvent
        // inside the ListView (since it is a ViewGroup).
        // If these calls haven't completed by the time the dialog has been dismissed, this will
        // cause a NullPointerException.
        // This test makes sure that this does not occur.
        // To fix this, dialog.dismiss() is now posted in a runnable instead of being run inline,
        // so dispatchTouchEvent can complete safely.
        val secondPointerUp = MotionEvent.ACTION_POINTER_UP +
                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        injectMotionEvent(secondPointerUp, coordinates, 2, downTime)

        // Release the first pointer
        injectMotionEvent(MotionEvent.ACTION_UP, coordinates, 1, downTime)
    }

    private fun injectMotionEvent(
        action: Int,
        coords: Array<MotionEvent.PointerCoords>,
        pointerCount: Int,
        downTime: Long
    ) {

        val eventTime = SystemClock.uptimeMillis()
        val properties = Array(pointerCount) { i ->
            MotionEvent.PointerProperties().apply {
                id = i
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }

        val event = MotionEvent.obtain(
            downTime, eventTime, action, pointerCount,
            properties, coords, 0, 0, 0f, 0f,
            0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        instrumentation.uiAutomation.injectInputEvent(event, true)
        event.recycle()
        instrumentation.waitForIdleSync()
    }
}
