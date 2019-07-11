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

@file:JvmName("LifecycleOwnerUtils")
package androidx.testutils

import android.app.Activity
import android.app.Instrumentation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility methods for testing LifecycleOwners
 */
private const val TIMEOUT_MS: Long = 5000

/**
 * Waits until the given [LifecycleOwner] has the specified
 * [androidx.lifecycle.Lifecycle.State]. If the owner has not hit that state within a
 * suitable time period, it asserts that the current state equals the given state.
 */
fun <T> waitUntilState(
    owner: T,
    state: Lifecycle.State
) where T : Activity, T : LifecycleOwner {
    val latch = CountDownLatch(1)
    owner.runOnUiThreadBlocking {
        val currentState = owner.lifecycle.currentState
        if (currentState == state) {
            latch.countDown()
            return@runOnUiThreadBlocking
        }
        owner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(
                provider: LifecycleOwner,
                event: Lifecycle.Event
            ) {
                if (provider.lifecycle.currentState == state) {
                    latch.countDown()
                    provider.lifecycle.removeObserver(this)
                }
            }
        })
    }
    val latchResult = latch.await(30, TimeUnit.SECONDS)

    assertThat(
        "Expected $state never happened to $owner. Current state:${owner.lifecycle.currentState}",
        latchResult,
        `is`(true)
    )

    // wait for another loop to ensure all observers are called
    owner.waitForExecution()
}

/**
 * Waits until the given [Activity] and [LifecycleOwner] has been recreated, and
 * the new instance is resumed.
 */
@Suppress("UNCHECKED_CAST")
fun <T> waitForRecreation(
    activity: T
): T where T : Activity, T : LifecycleOwner {
    val monitor = Instrumentation.ActivityMonitor(
        activity.javaClass.canonicalName, null, false
    )
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.addMonitor(monitor)

    var result: T?

    // this guarantee that we will reinstall monitor between notifications about onDestroy
    // and onCreate

    synchronized(monitor) {
        do {
            // The documentation says "Block until an Activity is created
            // that matches this monitor." This statement is true, but there are some other
            // true statements like: "Block until an Activity is destroyed" or
            // "Block until an Activity is resumed"...
            // this call will release synchronization monitor's monitor
            result = monitor.waitForActivityWithTimeout(TIMEOUT_MS) as T
            if (result == null) {
                throw RuntimeException("Timeout. Activity was not recreated.")
            }
        } while (result === activity)
    }

    // Finally wait for the recreated Activity to be resumed
    waitUntilState(result!!, Lifecycle.State.RESUMED)

    return result as T
}
