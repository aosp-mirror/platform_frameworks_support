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

package androidx.appcompat.app;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals;
import static androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForDestroy;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.appcompat.testutils.NightModeUtils.NightSetMode;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NightModeLateSuperOnCreateTestCase {
    @Rule
    public final ActivityTestRule<NightModePreventOverrideActivity> mActivityTestRule;

    public NightModeLateSuperOnCreateTestCase() {
        mActivityTestRule = new ActivityTestRule<>(NightModePreventOverrideActivity.class);
    }

    @Before
    public void setup() throws Throwable {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    @Test
    public void testActivityRecreate() throws Throwable {
        // Set local night mode to YES
        setNightModeAndWaitForDestroy(mActivityTestRule, MODE_NIGHT_YES, NightSetMode.LOCAL);

        final Activity activity = mActivityTestRule.getActivity();

        // Setting night mode is guaranteed to fail since the activity won't allow
        // configuration overrides.
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO,
                activity.getResources().getConfiguration());
    }
}
