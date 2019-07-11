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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.waitUntilState
import androidx.testutils.withActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(AndroidJUnit4::class)
class NightModePreventOverrideConfigTestCase(private val setMode: NightSetMode) {
    private lateinit var scenario: ActivityScenario<NightModePreventOverrideConfigActivity>

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the tests below
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Launch the test activity
        scenario = ActivityScenario.launch(NightModePreventOverrideConfigActivity::class.java)
    }

    @Test
    fun testActivityRecreate() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onActivity {
            // Assert that we're in the correct configuration
            assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO, it)
        }

        // Simulate the user setting night mode, which should force an activity recreate().
        val recreatedActivity = scenario.withActivity {
            setNightModeAndWaitForRecreate(this, MODE_NIGHT_YES, setMode)
        }

        // Activity should be able to reach fully resumed state again.
        waitUntilState(recreatedActivity, Lifecycle.State.RESUMED)
        // The requested night mode value should have been set by
        // updateResourcesConfigurationForNightMode().
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, recreatedActivity)
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
