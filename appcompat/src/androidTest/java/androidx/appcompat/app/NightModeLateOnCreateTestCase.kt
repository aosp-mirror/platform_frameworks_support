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
import androidx.appcompat.testutils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.setNightModeAndWaitForRecreate
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.testutils.currentActivity
import androidx.testutils.waitUntilState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModeLateOnCreateTestCase(private val setMode: NightSetMode) {
    private lateinit var scenario: ActivityScenario<NightModeLateOnCreateActivity>

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        scenario = ActivityScenario.launch(NightModeLateOnCreateActivity::class.java)
    }

    @Test
    fun testActivityRecreateLoop() {
        // Activity should be able to reach fully resumed state in default NIGHT_NO.
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO, scenario.currentActivity)

        // Simulate the user setting night mode, which should force an activity recreate().
        setNightModeAndWaitForRecreate(scenario.currentActivity, MODE_NIGHT_YES, setMode)

        // Activity should be able to reach fully resumed state again.
        waitUntilState(scenario.currentActivity, Lifecycle.State.RESUMED)

        // The request night mode value should have been set during attachBaseContext().
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            scenario.currentActivity
        )
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
    }
}
