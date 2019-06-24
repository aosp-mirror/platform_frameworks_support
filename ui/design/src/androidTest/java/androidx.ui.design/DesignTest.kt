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

package androidx.ui.design

import android.os.Handler
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.setContent
import androidx.compose.composer
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.TimeUnit

open class DesignTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    lateinit var activity: TestActivity
    lateinit var handler: Handler

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)

        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    internal fun show(@Children composable: @Composable() () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.setContent {
                    CraneWrapper {
                        composable()
                    }
                }
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }
}