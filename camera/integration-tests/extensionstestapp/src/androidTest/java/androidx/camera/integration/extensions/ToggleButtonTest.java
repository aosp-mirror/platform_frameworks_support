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

package androidx.camera.integration.extensions;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static junit.framework.TestCase.assertNotNull;

import androidx.camera.integration.extensions.idlingresource.ElapsedTimeIdlingResource;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test toggle buttons in extension test app. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class ToggleButtonTest {

    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int IDLE_TIMEOUT_MS = 1000;

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();

    @Rule
    public ActivityTestRule<CameraExtensionsActivity> mActivityRule =
            new ActivityTestRule<>(CameraExtensionsActivity.class);

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule mAudioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    public static void waitFor(IdlingResource idlingResource) {
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Test
    public void testSwitchModeButton() {
        boolean isPreviewExist = mActivityRule.getActivity().getPreview() != null;
        boolean isImageCaptureExist = mActivityRule.getActivity().getImageCapture() != null;

        // To switch all extensions.
        for (int i = 0; i < CameraExtensionsActivity.ImageCaptureType.values().length; i++) {
            waitFor(new ElapsedTimeIdlingResource(2000));
            onView(withId(R.id.PhotoToggle)).perform(click());
            if (isImageCaptureExist && mActivityRule.getActivity().getCurrentImageCaptureType()
                    != CameraExtensionsActivity.ImageCaptureType.IMAGE_CAPTURE_TYPE_NONE) {
                assertNotNull(mActivityRule.getActivity().getImageCapture());
            }
            if (isPreviewExist) {
                assertNotNull(mActivityRule.getActivity().getPreview());
            }
        }

        waitForIdlingRegistryAndPressBackAndHomeButton();
    }

    private void waitForIdlingRegistryAndPressBackAndHomeButton() {
        // Idles Espresso thread and make activity complete each action.
        waitFor(new ElapsedTimeIdlingResource(IDLE_TIMEOUT_MS));

        mDevice.pressBack();

        // Returns to Home to restart next test.
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }

}

