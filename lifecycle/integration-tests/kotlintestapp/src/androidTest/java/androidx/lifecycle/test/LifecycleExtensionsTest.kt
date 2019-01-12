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

package androidx.lifecycle.test

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.kotlintestapp.TestActivity
import androidx.lifecycle.whenStarted
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class LifecycleExtensionsTest {
    @JvmField
    @Rule
    val rule = activityScenarioRule<TestActivity>()


    @Test
    fun simple() {
        val latch = CountDownLatch(1)
        GlobalScope.launch {
            activity().whenStarted {
                latch.countDown()
            }
        }
        Truth.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @LargeTest
    fun waitUntilStarted() {
        rule.scenario.moveToState(Lifecycle.State.CREATED)
        val latch = CountDownLatch(1)
        GlobalScope.launch {
            activity().whenStarted {
                latch.countDown()
            }
        }
        Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse()
        rule.scenario.moveToState(Lifecycle.State.RESUMED)
        Truth.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    private fun activity() : LifecycleOwner {
        lateinit var owner: LifecycleOwner
        rule.scenario.onActivity {
            owner = it
        }
        return owner
    }
}