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

package androidx.ui.demos

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.test.assertIsVisible
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DemosAppTest {

    @get:Rule
    val activityTestRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    /**
     * Just a simple smoke test to make sure our demo app runs. Feel free to modify as needed.
     */
    @Test
    fun simpleSmokeTest() {

        onView(withText("Material")).perform(click())

        onView(withText("Buttons")).perform(click())

        findByText("long text", ignoreCase = true).assertIsVisible()

        pressBack()

        onView(withText("Text")).perform(click())

        findByText("Hello").assertIsVisible()
    }
}