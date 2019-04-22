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

package androidx.appcompat.testutils;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
<<<<<<< HEAD   (8c94d4 Merge "Fix spinner widget scroll" into androidx-g3-release)
=======
import androidx.appcompat.app.AppCompatDelegate.NightMode;
import androidx.lifecycle.Lifecycle;
>>>>>>> BRANCH (04abd8 Merge "Ignore tests on Q emulator while we stabilize them" i)
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

public class NightModeUtils {

<<<<<<< HEAD   (8c94d4 Merge "Fix spinner widget scroll" into androidx-g3-release)
=======
    public enum NightSetMode {
        /**
         * Set the night mode using {@link AppCompatDelegate#setDefaultNightMode(int)}
         */
        DEFAULT,

        /**
         * Set the night mode using {@link AppCompatDelegate#setLocalNightMode(int)}
         */
        LOCAL
    }

    public static void assertConfigurationNightModeEquals(int expectedNightMode,
            @NonNull Context context) {
        assertConfigurationNightModeEquals(expectedNightMode,
                context.getResources().getConfiguration());
    }

>>>>>>> BRANCH (04abd8 Merge "Ignore tests on Q emulator while we stabilize them" i)
    public static void assertConfigurationNightModeEquals(
            int expectedNightMode, Configuration configuration) {
        assertEquals(expectedNightMode, configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

<<<<<<< HEAD   (8c94d4 Merge "Fix spinner widget scroll" into androidx-g3-release)
    public static void setLocalNightModeAndWait(
            final ActivityTestRule<? extends AppCompatActivity> activityRule,
            @AppCompatDelegate.NightMode final int nightMode
=======
    public static <T extends AppCompatActivity> void setNightModeAndWait(
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
    ) throws Throwable {
        setNightModeAndWait(activityRule.getActivity(), activityRule, nightMode, setMode);
    }

    public static <T extends AppCompatActivity> void setNightModeAndWait(
            final AppCompatActivity activity,
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
>>>>>>> BRANCH (04abd8 Merge "Ignore tests on Q emulator while we stabilize them" i)
    ) throws Throwable {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
<<<<<<< HEAD   (8c94d4 Merge "Fix spinner widget scroll" into androidx-g3-release)
                activityRule.getActivity().getDelegate().setLocalNightMode(nightMode);
=======
                setNightMode(nightMode, activity, setMode);
>>>>>>> BRANCH (04abd8 Merge "Ignore tests on Q emulator while we stabilize them" i)
            }
        });
        instrumentation.waitForIdleSync();
    }
<<<<<<< HEAD   (8c94d4 Merge "Fix spinner widget scroll" into androidx-g3-release)
=======

    public static <T extends AppCompatActivity> void setNightModeAndWaitForDestroy(
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
    ) throws Throwable {
        final T activity = activityRule.getActivity();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setNightMode(nightMode, activity, setMode);
            }
        });
        LifecycleOwnerUtils.waitUntilState(activity, activityRule, Lifecycle.State.DESTROYED);
    }

    private static void setNightMode(
            @NightMode final int nightMode,
            final AppCompatActivity activity,
            final NightSetMode setMode) {
        if (setMode == NightSetMode.DEFAULT) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        } else {
            activity.getDelegate().setLocalNightMode(nightMode);
        }
    }
>>>>>>> BRANCH (04abd8 Merge "Ignore tests on Q emulator while we stabilize them" i)
}
