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

package com.example.android.support.appcompat.tests;

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

import com.example.android.support.appcompat.ControlsActivity;
import com.example.android.support.appcompat.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ControlsActivityTest {

    private static final String ENABLED_BUTTON_CLICKED_NOTIFICATION =
            "The enabled button was clicked!";
    private static final String DISABLED_BUTTON_CLICKED_NOTIFICATION =
            "The disabled button was clicked!";
    private static final String ENABLED_SWITCH_OFF_NOTIFICATION =
            "The enabled switch is off";
    private static final String ENABLED_SWITCH_ON_NOTIFICATION =
            "The enabled switch is on";
    private static final String DISABLED_SWITCH_ON_NOTIFICATION =
            "The disabled switch is on";
    private static final String CHECKBOX_CHECKED_NOTIFICATION =
            "Checkbox is checked";
    private static final String CHECKBOX_UNCHECKED_NOTIFICATION =
            "Checkbox is unchecked";
    private static final String EDITTEXT_IS_EMPTY =
            "The EditText box is empty";
    private static final String EDITBOX_CONTAINS =
            "The EditText says: \"";
    private static final String RADIOBUTTON_1_CHECKED_NOTIFICATION =
            "RadioButton1 is checked";
    private static final String RADIOBUTTON_2_CHECKED_NOTIFICATION =
            "RadioButton2 is checked";
    private static final String RADIOBUTTON_1_UNCHECKED_NOTIFICATION =
            "RadioButton1 is unchecked";
    private static final String RADIOBUTTON_2_UNCHECKED_NOTIFICATION =
            "RadioButton2 is unchecked";
    private static final String STAR_CHECKED_NOTIFICATION =
            "Star is checked";
    private static final String STAR_UNCHECKED_NOTIFICATION =
            "Star is unchecked";
    private static final String TOGGLEBUTTON_ON_NOTIFICATION =
            "The ToggleButton is on";
    private static final String TOGGLEBUTTON_OFF_NOTIFICATION =
            "The ToggleButton is off";
    private static final String IS_SELECTED =
            " is selected on the spinner";
    private static final String END_QUOTES = "\"";

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
                .check(matches(withText(ENABLED_BUTTON_CLICKED_NOTIFICATION)));
    }

    @Test
    @MediumTest
    public void testDisabledButtonClicked() {
        onView(withId(R.id.disabled_button)).perform(click());
        onView(withId(R.id.disabled_button_notification))
                .check(matches(not(withText(DISABLED_BUTTON_CLICKED_NOTIFICATION))));
    }

    @Test
    @MediumTest
    public void testEnabledSwitchClickedTwice() {
        onView(withId(R.id.enabled_switch)).perform(click());
        onView(withId(R.id.enabled_switch_notification))
                .check(matches(withText(ENABLED_SWITCH_ON_NOTIFICATION)));
        onView(withId(R.id.enabled_switch)).perform(click());
        onView(withId(R.id.enabled_switch_notification))
                .check(matches(withText(ENABLED_SWITCH_OFF_NOTIFICATION)));
    }

    @Test
    @MediumTest
    public void testDisabledSwitchClicked() {
        onView(withId(R.id.disabled_switch)).perform(click());
        onView(withId(R.id.disabled_switch_notification))
                .check(matches(not(withText(DISABLED_SWITCH_ON_NOTIFICATION))));
    }

    @Test
    @MediumTest
    public void testEditText() {
        onView(withId(R.id.edittext))
                .perform(typeText("Testing"), closeSoftKeyboard());
        onView(withId(R.id.edittext_notification))
                .check(matches(withText(EDITBOX_CONTAINS + "Testing" + END_QUOTES)));
        onView(withId(R.id.edittext))
                .perform(replaceText(""), closeSoftKeyboard());
        onView(withId(R.id.edittext_notification))
                .check(matches(withText(EDITTEXT_IS_EMPTY)));
    }

    @Test
    @MediumTest
    public void testCheckBoxClickedTwice() {
        onView(withId(R.id.checkbox)).perform(click());
        onView(withId(R.id.checkbox_notification))
                .check(matches(withText(CHECKBOX_CHECKED_NOTIFICATION)));
        onView(withId(R.id.checkbox)).perform(click());
        onView(withId(R.id.checkbox_notification))
                .check(matches(withText(CHECKBOX_UNCHECKED_NOTIFICATION)));
    }

    @Test
    @MediumTest
    public void testRadioButtonsClickedAndMutuallyExclusive() {
        onView(withId(R.id.rb1)).perform(click());
        onView(withId(R.id.rb1_notification))
                .check(matches(withText(RADIOBUTTON_1_CHECKED_NOTIFICATION)));
        onView(withId(R.id.rb2_notification))
                .check(matches(withText(RADIOBUTTON_2_UNCHECKED_NOTIFICATION)));
        onView(withId(R.id.rb2)).perform(click());
        onView(withId(R.id.rb1_notification))
                .check(matches(withText(RADIOBUTTON_1_UNCHECKED_NOTIFICATION)));
        onView(withId(R.id.rb2_notification))
                .check(matches(withText(RADIOBUTTON_2_CHECKED_NOTIFICATION)));
        onView(withId(R.id.rb1)).perform(click());
        onView(withId(R.id.rb1_notification))
                .check(matches(withText(RADIOBUTTON_1_CHECKED_NOTIFICATION)));
        onView(withId(R.id.rb2_notification))
                .check(matches(withText(RADIOBUTTON_2_UNCHECKED_NOTIFICATION)));
    }

    @Test
    @MediumTest
    public void testStarClickedTwice() {
        onView(withId(R.id.star_checkbox)).perform(click());
        onView(withId(R.id.star_notification))
                .check(matches(withText(STAR_CHECKED_NOTIFICATION)));
        onView(withId(R.id.star_checkbox)).perform(click());
        onView(withId(R.id.star_notification))
                .check(matches(withText(STAR_UNCHECKED_NOTIFICATION)));
    }

    @Test
    @MediumTest
    public void testToggleButtonClickedTwice() {
        onView(withId(R.id.toggle_button)).perform(ViewActions.scrollTo()).perform(click());
        onView(withId(R.id.toggle_button_notification))
                .check(matches(withText(TOGGLEBUTTON_ON_NOTIFICATION)));
        onView(withId(R.id.toggle_button)).perform(click());
        onView(withId(R.id.toggle_button_notification))
                .check(matches(withText(TOGGLEBUTTON_OFF_NOTIFICATION)));
    }

    @Test
    @MediumTest
    public void testDifferentSpinnerOptions() {
        onView(withId(R.id.spinner)).perform(ViewActions.scrollTo()).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("Option2"))).perform(click());
        onView(withId(R.id.spinner_notification)).check(matches(withText("Option2" + IS_SELECTED)));

    }
}
