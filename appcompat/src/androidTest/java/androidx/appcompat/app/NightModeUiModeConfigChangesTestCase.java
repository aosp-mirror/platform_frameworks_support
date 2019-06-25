<<<<<<< HEAD   (be0ce7 Merge "Merge empty history for sparse-5662278-L1600000033295)
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

import static androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals;
import static androidx.appcompat.testutils.NightModeUtils.setLocalNightModeAndWait;

import static org.junit.Assert.assertEquals;

import android.content.res.Configuration;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NightModeUiModeConfigChangesTestCase {
    @Rule
    public final ActivityTestRule<NightModeUiModeConfigChangesActivity> mActivityTestRule;

    public NightModeUiModeConfigChangesTestCase() {
        mActivityTestRule = new ActivityTestRule<>(NightModeUiModeConfigChangesActivity.class);
    }

    @Before
    public void setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Test
    public void testResourcesNotUpdated() throws Throwable {
        final int defaultNightMode = mActivityTestRule.getActivity()
                .getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        // Set local night mode to YES
        setLocalNightModeAndWait(mActivityTestRule, AppCompatDelegate.MODE_NIGHT_YES);

        // Assert that the Activity did not get updated
        assertConfigurationNightModeEquals(defaultNightMode,
                mActivityTestRule.getActivity().getResources().getConfiguration());

        // Set local night mode back to NO
        setLocalNightModeAndWait(mActivityTestRule, AppCompatDelegate.MODE_NIGHT_NO);

        // Assert that the Activity did not get updated
        assertConfigurationNightModeEquals(defaultNightMode,
                mActivityTestRule.getActivity().getResources().getConfiguration());
    }

    @Test
    public void testOnNightModeChangedCalled() throws Throwable {
        // Set local night mode to YES
        setLocalNightModeAndWait(mActivityTestRule, AppCompatDelegate.MODE_NIGHT_YES);
        // Assert that the Activity received a new value
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES,
                mActivityTestRule.getActivity().getLastNightModeAndReset());

        // Set local night mode to NO
        setLocalNightModeAndWait(mActivityTestRule, AppCompatDelegate.MODE_NIGHT_NO);
        // Assert that the Activity received a new value
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO,
                mActivityTestRule.getActivity().getLastNightModeAndReset());
    }
}
=======
>>>>>>> BRANCH (e55c95 Merge "Merge cherrypicks of [990151, 990154] into sparse-568)
