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

package com.example.androidx.appcompat.tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import com.example.androidx.appcompat.ControlsActivity;
import com.example.androidx.appcompat.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ControlsActivityTest {

    @Rule
    public final ActivityTestRule<ControlsActivity> mActivityTestRule;


    public ControlsActivityTest() {
        mActivityTestRule = new ActivityTestRule<>(ControlsActivity.class);
    }

    @Before
    public void setUp() {
        final ControlsActivity activity = mActivityTestRule.getActivity();
    }


    @Test
    @MediumTest
    public void testEnabledButtonClicked() {
        onView(withId(R.id.enabled_button)).perform(click());
        onView(withId(R.id.enabled_button_notification))
                .check(matches(withText(R.string.enabled_button_clicked_notification)));
    }

    @Test
    @MediumTest
    public void testDisabledButtonClicked() {
        onView(withId(R.id.disabled_button)).perform(click());
        onView(withId(R.id.disabled_button_notification))
                .check(matches(not(withText(R.string.disabled_button_clicked_notification))));
    }

    @Test
    @MediumTest
    public void testEnabledSwitchClickedTwice() {
        onView(withId(R.id.enabled_switch)).perform(click());
        onView(withId(R.id.enabled_switch_notification))
                .check(matches(withText(R.string.enabled_switch_on_notification)));
        onView(withId(R.id.enabled_switch)).perform(click());
        onView(withId(R.id.enabled_switch_notification))
                .check(matches(withText(R.string.enabled_switch_off_notification)));
    }

    @Test
    @MediumTest
    public void testDisabledSwitchClicked() {
        onView(withId(R.id.disabled_switch)).perform(click());
        onView(withId(R.id.disabled_switch_notification))
                .check(matches(not(withText(R.string.disabled_switch_on_notification))));
    }

    @Test
    @MediumTest
    public void testEditText() {
        onView(withId(R.id.edittext))
                .perform(typeText("Testing"), closeSoftKeyboard());
        onView(withId(R.id.edittext_notification))
                .check(matches(withText(R.string.editbox_contains + "Testing" + "\"")));
        onView(withId(R.id.edittext))
                .perform(replaceText(""), closeSoftKeyboard());
        onView(withId(R.id.edittext_notification))
                .check(matches(withText(R.string.edittext_empty)));
    }

    @Test
    @MediumTest
    public void testCheckBoxClickedTwice() {
        onView(withId(R.id.checkbox)).perform(click());
        onView(withId(R.id.checkbox_notification))
                .check(matches(withText(R.string.checkbox_checked_notification)));
        onView(withId(R.id.checkbox)).perform(click());
        onView(withId(R.id.checkbox_notification))
                .check(matches(withText(R.string.checkbox_unchecked_notification)));
    }

    @Test
    @MediumTest
    public void testRadioButtonsClickedAndMutuallyExclusive() {
        onView(withId(R.id.rb1)).perform(click());
        onView(withId(R.id.rb1_notification))
                .check(matches(withText(R.string.rb1_checked_notification)));
        onView(withId(R.id.rb2_notification))
                .check(matches(withText(R.string.rb2_unchecked_notification)));
        onView(withId(R.id.rb2)).perform(click());
        onView(withId(R.id.rb1_notification))
                .check(matches(withText(R.string.rb1_unchecked_notification)));
        onView(withId(R.id.rb2_notification))
                .check(matches(withText(R.string.rb2_checked_notification)));
        onView(withId(R.id.rb1)).perform(click());
        onView(withId(R.id.rb1_notification))
                .check(matches(withText(R.string.rb1_checked_notification)));
        onView(withId(R.id.rb2_notification))
                .check(matches(withText(R.string.rb2_unchecked_notification)));
    }

    @Test
    @MediumTest
    public void testStarClickedTwice() {
        onView(withId(R.id.star_checkbox)).perform(click());
        onView(withId(R.id.star_notification))
                .check(matches(withText(R.string.star_checked_notification)));
        onView(withId(R.id.star_checkbox)).perform(click());
        onView(withId(R.id.star_notification))
                .check(matches(withText(R.string.star_unchecked_notification)));
    }

    @Test
    @MediumTest
    public void testToggleButtonClickedTwice() {
        onView(withId(R.id.toggle_button)).perform(ViewActions.scrollTo()).perform(click());
        onView(withId(R.id.toggle_button_notification))
                .check(matches(withText(R.string.toggle_button_on_notification)));
        onView(withId(R.id.toggle_button)).perform(click());
        onView(withId(R.id.toggle_button_notification))
                .check(matches(withText(R.string.toggle_button_off_notification)));
    }

    @Test
    @MediumTest
    public void testDifferentSpinnerOptions() {
        onView(withId(R.id.spinner)).perform(ViewActions.scrollTo()).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("Option2"))).perform(click());
        onView(withId(R.id.spinner_notification)).check(matches(withText("Option2"
                + R.string.is_selected)));

    }
}
