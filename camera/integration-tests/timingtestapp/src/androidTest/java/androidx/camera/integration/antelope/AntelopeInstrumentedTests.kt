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

import android.content.Intent
import android.hardware.camera2.CameraManager
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.IdlingRegistry
import org.junit.Rule
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Before
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.PerformException
import androidx.test.espresso.assertion.ViewAssertions.matches
import java.io.File
import androidx.test.espresso.matcher.RootMatchers.isTouchable
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.filters.MediumTest
import androidx.test.filters.LargeTest
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeUnit

/**
 * Suite of tests that cover the major use cases for Antelope.
 *
 * Assumes device/emulator has a front and a back camera.
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

    @Rule @JvmField
    var repeatRule: RepeatRule = RepeatRule()

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
     * Make sure all system dialogs are closed from any previous tests
     */
    @Before
    fun closeSystemDialogs() {
        val activity = activityRule.activity as MainActivity
        activity.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    /**
     * Basic context sanity test
     */
    @Test
    @MediumTest
    fun test01ContextSanity() {
        val context = activityRule.activity.applicationContext
        Assert.assertEquals("androidx.camera.integration.antelope", context.packageName)
    }

    /**
     * Test log file deletion
     */
    @Test
    @MediumTest
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

        // Close options menu
        pressBackUnconditionally()
    }

    /**
     * Performs a single capture with the camera device 0 using the Camera 2 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test03SingleCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Single Capture\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a multi capture with the camera device 1 using the Camera 2 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test04MultiCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)

        if (hasCamera("1"))
            prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "1")
        else
            prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")

        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a multi capture "chained" test with camera device 0 using the Camera 2 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test05MultiCaptureChainedTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key),
            "MULTI_PHOTO_CHAIN")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Min")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(
            withSubstring("Multiple Captures (Chained)\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Starts a multi-capture test with camera device 0 using Camera 2 and aborts it after 5s
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test06AbortTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "30")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "1500")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Set a reasonable timeout for aborting the test
        IdlingPolicies.setMasterPolicyTimeout(15, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(15, TimeUnit.SECONDS)

        // When the abort button is visible, abort the multi-capture test
        val abortEnabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_abort))
        IdlingRegistry.getInstance().register(abortEnabledIdlingResource)

        try {
            onView(withId(R.id.button_abort))
                .inRoot(isTouchable()).check(matches(isDisplayed())).perform(click())
        } catch (ex: PerformException) {
            // Having aborted the camera test, a PerformException is expected here
        }

        IdlingRegistry.getInstance().unregister(abortEnabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("ABORTED")))

        assert(isLogDirEmpty())
    }

    /**
     * Performs a single camera switch back->front->back
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test07SwitchCameraTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device doesn't have both a front and back camera, skip this test
        assumeTrue(hasFrontAndBackCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up switch test
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "SWITCH_CAMERA")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Switch Cameras\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a single capture and saves the image to disk. Tests:
     *  - image was saved to disk
     *  - deleting images from settings menu works
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test08ImageSaveAndDeleteTest() {
        val activity = activityRule.activity as MainActivity
        val context = activityRule.activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up single a capture and save the photo
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), false)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Single Capture\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        // Check photo is on disk
        assert(!isPhotoDirEmpty())

        // Delete all photos on the device
        openActionBarOverflowOrOptionsMenu(context) // Open options menu
        onView(withId(R.id.menu_delete_photos))
        assert(isPhotoDirEmpty())

        // Close options menu
        pressBackUnconditionally()
    }

    /**
     * Performs a multi capture with the camera device 0 using the Camera 1 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test09MultiCaptureCamera1Test() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera1")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        val camViewModel = ViewModelProviders.of(activity).get(CamViewModel::class.java)
        camViewModel.getShouldOutputLog().value = true

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a multi capture with the camera device 0 using the Camera X API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test10MultiCaptureCameraXTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "CameraX")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        val camViewModel = ViewModelProviders.of(activity).get(CamViewModel::class.java)
        camViewModel.getShouldOutputLog().value = true

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures\nCamera")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a full set of captures for all possible cameras/image sizes/tests for Camera2
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test11MultipleCaptureTestCamera2() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up maximum test coverage for Camera2
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            hashSetOf("Camera2"))
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        if (hasFrontAndBackCamera())
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        else
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), false)

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)

        prefEditor.putString(res.getString(R.string.settings_numtests_key), "1")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")

        prefEditor.commit()

        val camViewModel = ViewModelProviders.of(activity).get(CamViewModel::class.java)
        camViewModel.getShouldOutputLog().value = true

        activity.runOnUiThread {
            activity.startMultiTest()
        }

        // Full test suite may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a full set of captures for all possible cameras/image sizes/tests for CameraX
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test12MultipleCaptureTestCameraX() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up maximum test coverage for CameraX
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            hashSetOf("CameraX"))
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        if (hasFrontAndBackCamera())
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        else
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), false)

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)

        prefEditor.putString(res.getString(R.string.settings_numtests_key), "1")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")

        prefEditor.commit()

        val camViewModel = ViewModelProviders.of(activity).get(CamViewModel::class.java)
        camViewModel.getShouldOutputLog().value = true

        activity.runOnUiThread {
            activity.startMultiTest()
        }

        // Full test suite may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

        assert(!isLogDirEmpty())
    }

    /**
     * Performs a full set of captures for all possible cameras/image sizes/tests for Camera1
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test13MultipleCaptureTestCamera1() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Delete any logs on the device
        deleteCSVFiles(activity)
        assert(isLogDirEmpty())

        // Set up maximum test coverage for Camera1
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            hashSetOf("Camera1"))
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        if (hasFrontAndBackCamera())
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        else
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), false)

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)

        prefEditor.putString(res.getString(R.string.settings_numtests_key), "1")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), "250")

        prefEditor.commit()

        val camViewModel = ViewModelProviders.of(activity).get(CamViewModel::class.java)
        camViewModel.getShouldOutputLog().value = true

        activity.runOnUiThread {
            activity.startMultiTest()
        }

        // Full test suite may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        // Test is completed when single/multi test buttons are re-enabled
        val enabledIdlingResource = ViewEnabledIdlingResource(
            activityRule.activity.findViewById<View>(R.id.button_single))
        IdlingRegistry.getInstance().register(enabledIdlingResource)

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))

        IdlingRegistry.getInstance().unregister(enabledIdlingResource)

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
     * Checks whether the test device has the given camera
     */
    private fun hasCamera(cameraId: String): Boolean {
        val activity = activityRule.activity as MainActivity
        val manager = activity.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
        return manager.cameraIdList.contains(cameraId)
    }

    /**
     * Checks if this devices has either a camera with id 0 or id 1
     */
    private fun hasAnyCamera(): Boolean = hasCamera("0") || hasCamera("1")

    /**
     * Checks if this devices has both a camera with id 0 and id 1
     */
    private fun hasFrontAndBackCamera(): Boolean = hasCamera("0") && hasCamera("1")

    /**
     * Determine what the first camera in the system is.
     *
     * Return 0 if camera 0 exists, 1 if camera 1 exists, otherwise empty string
     */
    private fun getFirstCamera(): String {
        if (hasCamera("0"))
            return "0"
        if (hasCamera("1"))
            return "1"

        return ""
    }
}