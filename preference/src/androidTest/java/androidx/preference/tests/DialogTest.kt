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
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.test.R
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.PollingCheck
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class ListPreferenceMultiplePointersTest {

    private lateinit var mInstrumentation: Instrumentation
    private lateinit var mActivity: ListPreferenceMultiplePointersActivity

    @get:Rule
    val activityRule = ActivityTestRule(ListPreferenceMultiplePointersActivity::class.java)

    @Before
    fun setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation()
        mActivity = activityRule.activity
    }

    private fun injectMultiTouchMotionEvent(
        action: Int,
        coords: Array<MotionEvent.PointerCoords>,
        pointerCount: Int,
        downTime: Long
    ) {

        val eventTime = SystemClock.uptimeMillis()
        val properties = Array(pointerCount, { i ->
            val p = MotionEvent.PointerProperties()
            p.id = i
            p.toolType = MotionEvent.TOOL_TYPE_FINGER
            p
        })

        val event = MotionEvent.obtain(downTime, eventTime, action, pointerCount,
            properties, coords, 0, 0, 0f, 0f,
            0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        mInstrumentation.uiAutomation.injectInputEvent(event, true)
        event.recycle()
        mInstrumentation.waitForIdleSync()
    }

    private fun waitForTapTimeout(view: View) {
        // Post to view's handler and wait until the message has been processed. This ensures that
        // anything that was posted prior to this event has been executed.
        val completed = BooleanArray(1)
        completed[0] = false
        val condition = PollingCheck.PollingCheckCondition { completed[0] }
        view.postDelayed({ completed[0] = true }, ViewConfiguration.getTapTimeout().toLong())
        PollingCheck.waitFor(condition)
    }

    @Test
    fun testMultiplePointers() {
        mInstrumentation.waitForIdleSync()
        mActivity.openDialog()
        mInstrumentation.waitForIdleSync()

        val buttonCancel = mActivity.getButton()
        val listView = mActivity.getListView()
        assertNotNull(buttonCancel)
        assertNotNull(listView)

        // Click the views
        val buttonCancelLocation = IntArray(2)
        buttonCancel.getLocationOnScreen(buttonCancelLocation)
        val listViewLocation = IntArray(2)
        listView.getLocationOnScreen(listViewLocation)
        val coords = Array<MotionEvent.PointerCoords>(2, { _ -> MotionEvent.PointerCoords() })
        // coords[0] = somewhere inside the button
        coords[0] = MotionEvent.PointerCoords()
        coords[0].x = (buttonCancelLocation[0] + 1).toFloat()
        coords[0].y = (buttonCancelLocation[1] + 1).toFloat()
        // coords[1] = somewhere inside the ListView
        coords[1] = MotionEvent.PointerCoords()
        coords[1].x = (listViewLocation[0] + 1).toFloat()
        coords[1].y = (listViewLocation[1] + 1).toFloat()
        val downTime = SystemClock.uptimeMillis()

        // Press and hold button Cancel
        injectMultiTouchMotionEvent(MotionEvent.ACTION_DOWN, coords, 1, downTime)

        // Click on ListView with the second pointer
        // Previously, releasing the second pointer would trigger the click handler, which would
        // execute "dialog.dismiss", and thus result in nested calls of dispatchTouchEvent
        // in the ListView (ViewGroup).
        // This test makes sure that this does not occur. To fix the bug, dialog.dismiss was posted
        // to get dialog instead of being run inline.
        val actionPointer1Down = MotionEvent.ACTION_POINTER_DOWN +
                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        injectMultiTouchMotionEvent(actionPointer1Down, coords, 2, downTime)

        // Currently, if the time difference between POINTER_DOWN and POINTER_UP is less than
        // ViewConfiguration.getTapTimeout(), then the click handler will not be invoked.
        waitForTapTimeout(buttonCancel)

        val actionPointer1Up = MotionEvent.ACTION_POINTER_UP +
                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        // The next line used to trigger null pointer exception in resetCancelNextUpFlag
        injectMultiTouchMotionEvent(actionPointer1Up, coords, 2, downTime)

        // Finish the gesture
        injectMultiTouchMotionEvent(MotionEvent.ACTION_UP, coords, 1, downTime)
    }
}

class ListPreferenceMultiplePointersActivity : AppCompatActivity() {
    private val fragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit()
    }

    /**
     * Find the resource id of a view that's in the app's view hierarchy, but is not explicitly
     * present in R. Therefore, there's no good way to import "R" to get this directly.
     */
    private fun getAndroidIdByName(name: String): Int {
        val id = resources.getIdentifier(name, "id", "android")
        assertNotEquals(0, id)
        return id
    }

    fun openDialog() {
        val pref: ListPreference = fragment.findPreference("choices")!!
        pref.performClick()
    }

    fun getButton(): Button {
        return (fragment.dialogFragment.dialog as AlertDialog).getButton(DialogInterface.BUTTON_NEGATIVE)
    }

    fun getListView(): View {
        return (fragment.dialogFragment.dialog as AlertDialog).listView
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = "SettingsFragment"

    lateinit var dialogFragment: ListPreferenceDialogFragmentCompat
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.test_dialog)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        // super.onDisplayPreferenceDialog(preference)
        // Do not call super here, and instead just duplicate some of the code.

        // This is needed because there is no other way to access ListPreferenceDialogFragment,
        // which we need in order to find the buttons of interest

        dialogFragment = ListPreferenceDialogFragmentCompat.newInstance(preference.key)
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(fragmentManager!!, TAG)
    }
}