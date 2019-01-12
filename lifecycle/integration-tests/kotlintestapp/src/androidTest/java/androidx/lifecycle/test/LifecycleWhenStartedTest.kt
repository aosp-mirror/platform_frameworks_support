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

import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleDestroyedException
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.kotlintestapp.TestActivity
import androidx.lifecycle.whenStarted
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@SmallTest
@RunWith(AndroidJUnit4::class)
class LifecycleWhenStartedTest {
    private val testScope = CoroutineScope(Job() + Dispatchers.Default)

    @JvmField
    @Rule
    val rule = activityScenarioRule<TestActivity>()

    @After
    fun cancelScope() {
        testScope.cancel()
    }

    @Test
    fun alreadyResumed() = runBlocking {
        rule.scenario.moveToState(RESUMED)
        assertThat(
            owner().whenStarted {
                true
            }
        ).isTrue()
    }

    @Test
    fun alreadyStarted() = runBlocking {
        rule.scenario.moveToState(STARTED)
        assertThat(
            owner().whenStarted {
                true
            }
        ).isTrue()
    }

    @Test
    fun startOnMainThread() = runBlocking {
        rule.scenario.moveToState(RESUMED)
        val owner = owner()
        assertThat(
            testScope.async(Dispatchers.Main) {
                owner.whenStarted {
                    true
                }
            }.await()
        ).isTrue()
    }

    @Test
    fun alreadyDestroyed() = runBlocking {
        val owner = owner() // grab it before destroying
        rule.scenario.moveToState(DESTROYED)
        val action = testScope.async {
            owner.whenStarted {
                true
            }
        }
        action.join()
        assertThat(action.getCompletionExceptionOrNull())
            .isInstanceOf(LifecycleDestroyedException::class.java)
    }

    @Test
    @LargeTest
    fun waitUntilStarted(): Unit = runBlocking {
        rule.scenario.moveToState(CREATED)
        val action = testScope.async {
            owner().whenStarted {
                true
            }
        }
        // wait to ensure it is not running, we don't have dispatchers idling resource APIs yet
        delay(2_000)
        assertThat(action.isActive).isTrue()
        rule.scenario.moveToState(RESUMED)
        assertThat(action.await()).isTrue()
    }

    @Test
    @LargeTest
    fun waitUntilDestroyed(): Unit = runBlocking {
        rule.scenario.moveToState(CREATED)
        val action = testScope.async {
            owner().whenStarted {
                throw AssertionError("should've never run")
            }
        }
        // We don't have an API to check if all dispatchers are IDLE so we'll simply delay
        // until a better API is available.
        delay(2_000)
        assertThat(action.isActive).isTrue()
        rule.scenario.moveToState(DESTROYED)
        action.join()
        assertThat(action.isCancelled).isTrue()
    }

    private fun owner(): LifecycleOwner {
        lateinit var owner: LifecycleOwner
        rule.scenario.onActivity {
            owner = it
        }
        return owner
    }
}