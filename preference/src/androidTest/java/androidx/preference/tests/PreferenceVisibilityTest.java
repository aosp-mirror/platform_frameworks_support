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

package androidx.preference.tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.test.R;
import androidx.preference.tests.helpers.PreferenceTestHelperActivity;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link androidx.preference.Preference} visibility logic.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreferenceVisibilityTest {

    @Rule
    public ActivityTestRule<PreferenceTestHelperActivity> mActivityRule =
            new ActivityTestRule<>(PreferenceTestHelperActivity.class);

    private static final String CATEGORY = "Category";
    private static final String DEFAULT = "Default";
    private static final String VISIBLE = "Visible";
    private static final String INVISIBLE = "Invisible";

    private PreferenceFragmentCompat mFragment;

    @Before
    @UiThreadTest
    public void setUp() {
        mFragment = mActivityRule.getActivity().setupPreferenceHierarchy(
                R.xml.test_visibility);
    }

    @Test
    public void preferencesInflatedFromXml_visibilitySetCorrectly() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Parent category without visibility set should be visible and shown
                assertTrue(mFragment.findPreference("category").isVisible());
                assertTrue(mFragment.findPreference("category").isShown());
                // Preference without visibility set should be visible and shown
                assertTrue(mFragment.findPreference("default").isVisible());
                assertTrue(mFragment.findPreference("default").isShown());
                // Preference with visibility set to true should be visible and shown
                assertTrue(mFragment.findPreference("visible").isVisible());
                assertTrue(mFragment.findPreference("visible").isShown());
                // Preference with visibility set to false should not be visible or shown
                assertFalse(mFragment.findPreference("invisible").isVisible());
                assertFalse(mFragment.findPreference("invisible").isShown());
            }
        });

        // Parent category without visibility set should be displayed
        onView(withText(CATEGORY)).check(matches(isDisplayed()));
        // Preference without visibility set should be displayed
        onView(withText(DEFAULT)).check(matches(isDisplayed()));
        // Preference with visibility set to true should be displayed
        onView(withText(VISIBLE)).check(matches(isDisplayed()));
        // Preference with visibility set to false should not be displayed
        onView(withText(INVISIBLE)).check(doesNotExist());

        // The category and its two visible children should be added to the RecyclerView
        assertEquals(3, mFragment.getListView().getChildCount());
    }

    @Test
    public void hidingPreference_visibilitySetCorrectly() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide a preference
                mFragment.findPreference("default").setVisible(false);

                // Preference should not be visible or shown
                assertFalse(mFragment.findPreference("default").isVisible());
                assertFalse(mFragment.findPreference("default").isShown());
            }
        });

        // Wait for hierarchy rebuild (happens async)
        SystemClock.sleep(500);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Preference should no longer be shown
        onView(withText(DEFAULT)).check(doesNotExist());

        // This shouldn't affect other preferences
        onView(withText(CATEGORY)).check(matches(isDisplayed()));
        onView(withText(VISIBLE)).check(matches(isDisplayed()));
        onView(withText(INVISIBLE)).check(doesNotExist());

        // The category and its only visible child should be added to the RecyclerView
        assertEquals(2, mFragment.getListView().getChildCount());
    }

    @Test
    public void hidingParentGroup_childrenNeverVisible() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide the parent category
                mFragment.findPreference("category").setVisible(false);

                // Category should not be visible or shown
                assertFalse(mFragment.findPreference("category").isVisible());
                assertFalse(mFragment.findPreference("category").isShown());

                // Preference visibility should be unchanged
                assertTrue(mFragment.findPreference("default").isVisible());
                assertTrue(mFragment.findPreference("visible").isVisible());
                assertFalse(mFragment.findPreference("invisible").isVisible());

                // Preferences should not be shown since their parent is not visible
                assertFalse(mFragment.findPreference("default").isShown());
                assertFalse(mFragment.findPreference("visible").isShown());
                assertFalse(mFragment.findPreference("invisible").isShown());
            }
        });

        // Wait for hierarchy rebuild (happens async)
        SystemClock.sleep(500);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Nothing should be displayed
        onView(withText(CATEGORY)).check(doesNotExist());
        onView(withText(DEFAULT)).check(doesNotExist());
        onView(withText(VISIBLE)).check(doesNotExist());
        onView(withText(INVISIBLE)).check(doesNotExist());

        // Nothing should be added to the RecyclerView
        assertEquals(0, mFragment.getListView().getChildCount());

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Attempt to show a previously hidden preference
                mFragment.findPreference("invisible").setVisible(true);
            }
        });

        // Wait for hierarchy rebuild (happens async)
        SystemClock.sleep(500);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // No preferences should be visible
        onView(withText(CATEGORY)).check(doesNotExist());
        onView(withText(DEFAULT)).check(doesNotExist());
        onView(withText(VISIBLE)).check(doesNotExist());
        onView(withText(INVISIBLE)).check(doesNotExist());

        // Nothing should be added to the RecyclerView
        assertEquals(0, mFragment.getListView().getChildCount());
    }

    @Test
    @UiThreadTest
    public void preferenceNotAttachedToHierarchy_visibleButNotShown() {
        // Create a new preference not attached to the root preference screen
        Preference preference = new Preference(mFragment.getContext());

        // Preference is visible, but since it is not attached to the hierarchy, it is not shown
        assertTrue(preference.isVisible());
        assertFalse(preference.isShown());
    }
}
