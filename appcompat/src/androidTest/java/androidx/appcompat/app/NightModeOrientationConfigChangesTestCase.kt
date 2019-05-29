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

package androidx.appcompat.app

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightSetMode
import androidx.appcompat.testutils.TestUtilsActions.rotateScreenOrientation
import androidx.appcompat.testutils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.setNightModeAndWaitForRecreate
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModeOrientationConfigChangesTestCase(private val setMode: NightSetMode) {

    @Rule
    val rule = ActivityTestRule(
        NightModeOrientationConfigChangesActivity::class.java,
        false,
        false
    )

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Now launch the test activity
        rule.launchActivity(null)
    }

    @Test
    @Throws(Throwable::class)
    fun testRotateDoesNotRecreateActivity() {
        // Set local night mode to YES
        setNightModeAndWaitForRecreate(
            rule,
            nightMode = MODE_NIGHT_YES,
            setMode = setMode
        )

        val activity = rule.activity

        // Assert that the current Activity is 'dark'
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            activity.resources.configuration
        )

        // Now rotate the device
        onView(isRoot()).perform(rotateScreenOrientation(activity))

        // And assert that we have the same Activity, and thus was not recreated
        assertSame(activity, rule.activity)
    }

    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
    }
}
