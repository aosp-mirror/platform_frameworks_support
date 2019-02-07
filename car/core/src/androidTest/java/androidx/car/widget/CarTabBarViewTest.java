/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.car.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withAlpha;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withTagKey;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.util.TypedValue;

import androidx.car.test.R;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link CarTabBarViewTest}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class CarTabBarViewTest {
    @Rule
    public ActivityTestRule<CarTabBarViewTestActivity> mActivityRule =
            new ActivityTestRule<>(CarTabBarViewTestActivity.class);

    private Context mContext;
    private CarTabBarView mTabBarView;

    /** The alpha for a tab that is considered selected. */
    private float mSelectedAlpha;

    /** The alpha for a tab that is not currently selected. */
    private float mInactiveAlpha;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        Activity activity = mActivityRule.getActivity();
        mTabBarView = activity.findViewById(R.id.car_tab_bar);
        mContext = activity;

        Resources res = mContext.getResources();
        TypedValue outValue = new TypedValue();
        res.getValue(R.dimen.selected_tab_alpha, outValue, true);
        mSelectedAlpha = outValue.getFloat();

        res.getValue(R.dimen.inactive_tab_alpha, outValue, true);
        mInactiveAlpha = outValue.getFloat();
    }

    @Test
    public void testMinimumHeight_fixedHeight() throws Throwable {
        // Set all widgets to null - tab bar should still be minimum-height tall.
        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(null));

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        int expected = mContext.getResources().getDimensionPixelSize(R.dimen.car_app_bar_height);
        assertThat(mTabBarView.getHeight(), is(equalTo(expected)));
    }

    @Test
    public void testNullTabs_noTabsAdded() throws Throwable {
        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(null));
        onView(withId(R.id.car_tab)).check(doesNotExist());
    }

    @Test
    public void testSingleTab_addedAsChild() throws Throwable {
        CarTabItem tab = new CarTabItem.Builder()
                .setIcon(Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon))
                .setText("text")
                .build();

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(Arrays.asList(tab)));

        onView(withId(R.id.car_tab)).check(matches(isDisplayed()));
    }

    @Test
    public void testSingleTab_hasTextContent() throws Throwable {
        String tabText = "tabText";

        CarTabItem tab = new CarTabItem.Builder()
                .setText(tabText)
                .build();

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(Arrays.asList(tab)));

        onView(allOf(withId(R.id.car_tab_text), withParent(withId(R.id.car_tab))))
                .check(matches(withText(tabText)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSingleTab_emptyText() throws Throwable {
        CarTabItem tab = new CarTabItem.Builder().build();

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(Arrays.asList(tab)));

        onView(allOf(withId(R.id.car_tab_text), withParent(withId(R.id.car_tab))))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testSingleTab_hasIcon() throws Throwable {
        CarTabItem tab = new CarTabItem.Builder()
                .setIcon(Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon))
                .build();

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(Arrays.asList(tab)));

        onView(allOf(withId(R.id.car_tab_icon), withParent(withId(R.id.car_tab))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSingleTab_emptyIcon() throws Throwable {
        CarTabItem tab = new CarTabItem.Builder().build();

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(Arrays.asList(tab)));

        onView(allOf(withId(R.id.car_tab_icon), withParent(withId(R.id.car_tab))))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testMultipleTabs_addsTabsAsChildren() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 5;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(tabs));

        for (int i = 0; i < numOfTabs; i++) {
            assertTabAtPositionIsDisplayed(/* position= */ i);
        }
    }

    @Test
    public void testTabSelection_firstTabHighlightedByDefault() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(tabs));

        assertTabIsDisplayedWithAlpha(/* position= */ 0, mSelectedAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 1, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 2, mInactiveAlpha);
    }

    @Test
    public void testTabSelection_highlightsSelectedTabAfterClick() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        mActivityRule.runOnUiThread(() -> mTabBarView.setTabs(tabs));

        // Click the tab at position 1.
        onView(allOf(withId(R.id.car_tab), withTagKey(R.id.car_tab_position, is(1))))
                .perform(click());

        // Verify position 1 is selected.
        assertTabIsDisplayedWithAlpha(/* position= */ 0, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 1, mSelectedAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 2, mInactiveAlpha);
    }

    @Test
    public void testTabSelection_manuallySetSelectedTab() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        // Manually set the tab at position 1 to be selected.
        mActivityRule.runOnUiThread(() -> {
            mTabBarView.setSelectedTab(1);
            mTabBarView.setTabs(tabs);
        });

        // Verify position 1 is selected.
        assertTabIsDisplayedWithAlpha(/* position= */ 0, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 1, mSelectedAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 2, mInactiveAlpha);
    }

    @Test
    public void testTabSelection_manuallySetSelectedTab_outOfBounds() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        // Manually set the selected tab to be a position greater than the number of tabs.
        mActivityRule.runOnUiThread(() -> {
            mTabBarView.setSelectedTab(numOfTabs);
            mTabBarView.setTabs(tabs);
        });

        // No tab should be selected.
        assertTabIsDisplayedWithAlpha(/* position= */ 0, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 1, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 2, mInactiveAlpha);
    }

    @Test
    public void testTabSelection_manuallySetSelectedTabAndClickPerformed() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        // Manually set the selected tab to be position 1.
        mActivityRule.runOnUiThread(() -> {
            mTabBarView.setSelectedTab(1);
            mTabBarView.setTabs(tabs);
        });

        // However, perform a click on position 2.
        onView(allOf(withId(R.id.car_tab), withTagKey(R.id.car_tab_position, is(2))))
                .perform(click());

        // Verify position 2 selected.
        assertTabIsDisplayedWithAlpha(/* position= */ 0, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 1, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 2, mSelectedAlpha);
    }

    @Test
    public void testTabSelection_clearTabsRestsBackToZero() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        // Manually set the selected tab to "1" but then clear the tabs, which should reset this.
        mActivityRule.runOnUiThread(() -> {
            mTabBarView.setSelectedTab(1);
            mTabBarView.setTabs(tabs);

            // Clear the tabs.
            mTabBarView.setTabs(null);

            // Reset the tabs.
            mTabBarView.setTabs(tabs);
        });

        // Resetting the tabs should set the selected position back to 0.
        assertTabIsDisplayedWithAlpha(/* position= */ 0, mSelectedAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 1, mInactiveAlpha);
        assertTabIsDisplayedWithAlpha(/* position= */ 2, mInactiveAlpha);
    }

    @Test
    public void testTabSelection_triggersTabListener() throws Throwable {
        List<CarTabItem> tabs = new ArrayList<>();

        int numOfTabs = 3;
        for (int i = 0; i < numOfTabs; i++) {
            Icon icon = Icon.createWithResource(mContext, android.R.drawable.sym_def_app_icon);
            tabs.add(new CarTabItem.Builder()
                    .setIcon(icon)
                    .setText("text")
                    .build());
        }

        // 3 tabs, so create this boolean array with 3 entries.
        boolean[] clicked = new boolean[] {false, false, false};

        mActivityRule.runOnUiThread(() -> {
            mTabBarView.setTabs(tabs);
            mTabBarView.setOnTabSelectListener(position -> clicked[position] = true);
        });

        // Perform a click on position 1.
        onView(allOf(withId(R.id.car_tab), withTagKey(R.id.car_tab_position, is(1))))
                .perform(click());

        // Should only have triggered the listener with position 1.
        assertFalse(clicked[0]);
        assertTrue(clicked[1]);
        assertFalse(clicked[2]);
    }

    /** Asserts the tab at the given position is currently being displayed. */
    private void assertTabAtPositionIsDisplayed(int position) {
        // All tabs have the id "car_tab" and should have a tag with the key "car_tab_position" and
        // a value corresponding to its position.
        ViewInteraction tabView = onView(allOf(
                withId(R.id.car_tab),
                withTagKey(R.id.car_tab_position, is(position))));

        tabView.check(matches(isDisplayed()));
    }

    /** Asserts that the tab at the given position has the given alpha. */
    private void assertTabIsDisplayedWithAlpha(int position, float alpha) {
        ViewInteraction tabView = onView(allOf(
                withId(R.id.car_tab),
                withTagKey(R.id.car_tab_position, is(position)),
                withAlpha(alpha)));

        tabView.check(matches(isDisplayed()));
    }

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }
}
