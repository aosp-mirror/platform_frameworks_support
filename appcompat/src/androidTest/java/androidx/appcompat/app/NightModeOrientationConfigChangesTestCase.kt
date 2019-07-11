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
import androidx.appcompat.testutils.TestUtilsActions.rotateScreenOrientation
import androidx.appcompat.testutils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.setNightModeAndWaitForRecreate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Arrays

@LargeTest
@RunWith(Parameterized::class)
class NightModeOrientationConfigChangesTestCase(private val setMode: NightSetMode) {
    private lateinit var scenario: ActivityScenario<NightModeOrientationConfigChangesActivity>

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        // Now launch the test activity
        scenario = ActivityScenario.launch(NightModeOrientationConfigChangesActivity::class.java)
    }

    @Test
    fun testRotateDoesNotRecreateActivity() {
        // Set local night mode to YES
        scenario.onActivity {
            setNightModeAndWaitForRecreate(it, MODE_NIGHT_YES, setMode)
        }

        // Assert that the current Activity is 'dark'
        scenario.onActivity {
            assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, it)
        }

        var activity: NightModeActivity? = null

        // Now rotate the device
        scenario.onActivity {
            activity = it
            onView(isRoot()).perform(rotateScreenOrientation(it))
        }

        // And assert that we have the same Activity, and thus was not recreated
        scenario.onActivity {
            assertSame(activity, it)
        }
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<NightSetMode> {
            return Arrays.asList(NightSetMode.DEFAULT, NightSetMode.LOCAL)
        }
    }
}
