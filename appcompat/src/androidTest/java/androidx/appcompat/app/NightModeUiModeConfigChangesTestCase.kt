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
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightSetMode
import androidx.appcompat.testutils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.setNightModeAndWait
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModeUiModeConfigChangesTestCase(private val setMode: NightSetMode) {

    @get:Rule
    val rule = ActivityTestRule(
        NightModeUiModeConfigChangesActivity::class.java,
        false,
        false
    )

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the tests below
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        // Now launch the test activity
        rule.launchActivity(null)
    }

    @Test
    @Throws(Throwable::class)
    fun testResourcesNotUpdated() {
        val defaultNightMode = rule.activity
            .resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set local night mode to YES
        setNightModeAndWait(
            rule,
            nightMode = MODE_NIGHT_YES,
            setMode = setMode
        )

        // Assert that the Activity did not get updated
        assertConfigurationNightModeEquals(
            defaultNightMode,
            rule.activity.resources.configuration
        )

        // Set local night mode back to NO
        setNightModeAndWait<NightModeUiModeConfigChangesActivity>(
            rule,
            nightMode = MODE_NIGHT_NO,
            setMode = setMode
        )

        // Assert that the Activity did not get updated
        assertConfigurationNightModeEquals(
            defaultNightMode,
            rule.activity.resources.configuration
        )
    }

    @Test
    @Throws(Throwable::class)
    fun testOnNightModeChangedCalled() {
        // Set local night mode to YES
        setNightModeAndWait<NightModeUiModeConfigChangesActivity>(
            rule,
            nightMode = MODE_NIGHT_YES,
            setMode = setMode
        )
        // Assert that the Activity received a new value
        assertEquals(
            MODE_NIGHT_YES,
            rule.activity.lastNightModeAndReset
        )

        // Set local night mode to NO
        setNightModeAndWait<NightModeUiModeConfigChangesActivity>(
            rule,
            nightMode = MODE_NIGHT_NO,
            setMode = setMode
        )
        // Assert that the Activity received a new value
        assertEquals(
            MODE_NIGHT_NO,
            rule.activity.lastNightModeAndReset
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
    }
}
