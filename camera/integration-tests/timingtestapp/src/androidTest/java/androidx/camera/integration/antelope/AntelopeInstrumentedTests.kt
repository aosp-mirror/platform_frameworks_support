/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.junit.Rule
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Before
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.test.espresso.assertion.ViewAssertions.matches
import java.io.File
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import java.lang.Thread.sleep

/** Maximum time (ms) to wait for view to be enabled */
const val VIEW_TIMEOUT = 30000L
/** Polling interval (ms) when waiting for UI changes */
const val VIEW_CHECK_INTERVAL = 100L
/** Pause time (ms) between UI interactions to account for animations, etc. */
const val VIEW_INTERACTION_PAUSE = 1000L

/**
 * Suite of tests that cover the major use cases for Antelope.
 *
 * Assumes device/emulator has a front and a back camera and that the maximum latency
 * for a single capture is < VIEW_TIMEOUT.
 *
 * Currently tests are marked FlakyTest so as not to block all of androidx if there is an error.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AntelopeInstrumentedTests {
    @get: Rule
    var activityRule: ActivityTestRule<MainActivity> =
        ActivityTestRule(MainActivity::class.java)

    @get: Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get: Rule
    val writeStoragePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get: Rule
    val readStoragePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    /**
     * On some API levels, the above permissions rules do not always work but explicitly
     * using a shell command does.
     */
    @Before
    fun grantPermissions() {
        getInstrumentation().getUiAutomation().executeShellCommand(
            "pm grant " + activityRule.activity.applicationContext +
                " android.permission.CAMERA")
        getInstrumentation().getUiAutomation().executeShellCommand(
            "pm grant " + activityRule.activity.applicationContext +
                " android.permission.READ_EXTERNAL_STORAGE")
        getInstrumentation().getUiAutomation().executeShellCommand(
            "pm grant " + activityRule.activity.applicationContext +
                " android.permission.WRITE_EXTERNAL_STORAGE")
    }

    /**
     * Basic context sanity test
     */
    @Test
    fun test01ContextSanity() {
        val context = activityRule.activity.applicationContext
        Assert.assertEquals("androidx.camera.integration.antelope", context.packageName)
    }

    /**
     * Test log file deletion
     */
    @Test
    fun test02WriteandDeleteLogFiles() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext

        // Write a fake log file
        writeCSV(activity, "fakelogfile", "This is a fake log file")
        assert(!isLogDirEmpty())

        // Delete all logs from the device
        openActionBarOverflowOrOptionsMenu(context) // Open options menu
        onView(withId(R.id.menu_delete_logs))
        assert(isLogDirEmpty())
    }

    /**
     * Performs a single capture with the camera device 0 using the Camera 2 API
     */
    @Test
    fun test03SingleCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check dialog has closed and we have a log shown on screen and saved to disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("Single Capture")))
        assert(!isLogDirEmpty())
    }

    /**
     * Performs a multi capture with the camera device 1 using the Camera 2 API
     */
    @Test
    fun test04MultiCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "1")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "10")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check dialog has closed and we have a log shown on screen and saved to disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures")))
        assert(!isLogDirEmpty())
    }

    /**
     * Performs a multi capture "chained" test with camera device 0 using the Camera 2 API
     */
    @Test
    fun test05MultiCaptureChainedTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key),
            "MULTI_PHOTO_CHAIN")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Min")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "10")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check dialog has closed and we have a log shown on screen and saved to disk
        onView(withId(R.id.text_log)).check(matches(
            withSubstring("Multiple Captures (Chained)")))
        assert(!isLogDirEmpty())
    }

    /**
     * Starts a multi-capture test with camera device 0 using Camera 2 and aborts it after 5s
     */
    @Test
    fun test06AbortTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "30")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait 5 seconds and then abort
        sleep(5000)
        onView(withId(R.id.button_abort)).perform(click())

        // Check dialog has closed and we have a log on screen and on disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("ABORTED")))
        assert(isLogDirEmpty())
    }

    /**
     * Performs a single camera switch back->front->back
     */
    @Test
    fun test07SwitchCameraTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up switch test
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "SWITCH_CAMERA")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check dialog has closed and we have a log shown on screen and saved to disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("Switch Cameras")))
        assert(!isLogDirEmpty())
    }

    /**
     * Performs a single capture and saves the image to disk. Tests:
     *  - image was saved to disk
     *  - deleting images from settings menu works
     */
    @Test
    fun test08ImageSaveAndDeleteTest() {
        val context = activityRule.activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Set up single a capture and save the photo
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), false)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check photo is on disk
        assert(!isPhotoDirEmpty())

        // Delete all photos on the device
        openActionBarOverflowOrOptionsMenu(context) // Open options menu
        onView(withId(R.id.menu_delete_photos))
        assert(isPhotoDirEmpty())
    }

    /**
     * Performs a multi capture with the camera device 0 using the Camera 1 API
     */
    @Test
    fun test09MultiCaptureCamera1Test() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera1")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "10")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check dialog has closed and we have a log shown on screen and saved to disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures")))
        assert(!isLogDirEmpty())
    }

    /**
     * Performs a multi capture with the camera device 0 using the Camera X API
     */
    @Test
    fun test10MultiCaptureCameraXTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "CameraX")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "10")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        // Open single test dialog and perform test
        onView(withId(R.id.button_single)).check(matches(isDisplayed()))
        onView(withId(R.id.button_single)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilViewIsEnabled(R.id.button_single)

        // Check dialog has closed and we have a log shown on screen and saved to disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures")))
        assert(!isLogDirEmpty())
    }

    /**
     * Performs a full set of captures for all possible APIs/cameras/image sizes/tests
     */
    @Test
    fun test11MultipleCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up maximum test coverage
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            res.getStringArray(R.array.array_settings_api).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), false)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)

        prefEditor.putString(res.getString(R.string.settings_numtests_key), "10")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")

        prefEditor.commit()

        // Open multiple test dialog and perform test
        onView(withId(R.id.button_multi)).check(matches(isDisplayed()))
        onView(withId(R.id.button_multi)).perform(click())
        onView(withId(R.id.button_start)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.button_start)).inRoot(isDialog()).perform(click())

        // Wait until the test has completed
        waitUntilMultiTestComplete()

        // Check dialog has closed and we have a log on screen and on disk
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))
        assert(!isLogDirEmpty())
    }

    /**
     * Checks whether the default .csv log directory is empty
     */
    private fun isLogDirEmpty(): Boolean {
        val csvDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS), MainActivity.LOG_DIR)

        if (csvDir.exists()) {
            val children = csvDir.listFiles()
            return (children.isEmpty())
        } else {
            return true
        }
    }

    /**
     * Checks whether the default image directory is empty
     */
    private fun isPhotoDirEmpty(): Boolean {
        val photoDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM), MainActivity.PHOTOS_DIR)

        if (photoDir.exists()) {
            val children = photoDir.listFiles()
            return (children.isEmpty())
        } else {
            return true
        }
    }

    /**
     * We determine if multi-test is complete if the "multi test" button has been re-enabled.
     *
     * Multi-test runs can take 60 mins, to measure if the test is running and not locked up,
     * this function examines the progress bar. If it is changing, the test is still running. This
     * is checked every VIEW_CHECK_INTERVAL ms.
     *
     * There is a timeout of VIEW_TIMEOUT ms. If the progress bar has not advanced during that
     * timeout, this function returns.
     *
     * Note: this function could fail if every VIEW_CHECK_INTERVAL the test was on the same
     * iteration of a subsequent test. Unlikely for test repetitions > 2.
     */
    private fun waitUntilMultiTestComplete() {

        val view = activityRule.activity.findViewById<Button>(R.id.button_multi)
        val progressBar = activityRule.activity.findViewById<ProgressBar>(R.id.progress_test)
        var lastProgressBarValue = 0

        if (null != view) {
            var timeoutBegin = System.currentTimeMillis()
            while (!view.isEnabled) {
                sleep(VIEW_CHECK_INTERVAL)

                // If there has been change, restart the timeout
                if (progressBar.progress != lastProgressBarValue) {
                    lastProgressBarValue = progressBar.progress
                    timeoutBegin = System.currentTimeMillis()
                    continue
                }

                // No change in progress bar, check if this has timed out
                if (System.currentTimeMillis() - timeoutBegin >= VIEW_TIMEOUT) {
                    throw AssertionError("View: " + view.id + " not enabled after " +
                        (VIEW_TIMEOUT / 1000) + "s.")
                }
            }
        }
    }

    /**
     * Given a view id, wait, up until VIEW_TIMEOUT, for it to be enabled
     */
    private fun waitUntilViewIsEnabled(id: Int) {
        val view = activityRule.activity.findViewById<View>(id)

        if (null != view) {
            val startTime = System.currentTimeMillis()
            while (!view.isEnabled) {
                sleep(VIEW_CHECK_INTERVAL)
                if (System.currentTimeMillis() - startTime >= VIEW_TIMEOUT) {
                    throw AssertionError("View: " + view.id + " not enabled after " +
                        (VIEW_TIMEOUT / 1000) + "s.")
                }
            }
        }
    }
}