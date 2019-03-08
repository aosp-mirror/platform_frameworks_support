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

package androidx.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedHandlerTest {

    lateinit var dispatcher: OnBackPressedDispatcher

    @Before
    fun setup() {
        dispatcher = OnBackPressedDispatcher()
    }

    @UiThreadTest
    @Test
    fun testAddOnBackPressedListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addOnBackPressedCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRemoveOnBackPressedListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addOnBackPressedCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        dispatcher.removeOnBackPressedCallback(onBackPressedCallback)
        assertWithMessage("Handler should return false when no OnBackPressedCallbacks " +
                "are registered")
            .that(dispatcher.onBackPressed())
            .isFalse()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testMultipleCalls() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addOnBackPressedCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Count should be incremented after each onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testMostRecentGetsPriority() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val mostRecentOnBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addOnBackPressedCallback(onBackPressedCallback)
        dispatcher.addOnBackPressedCallback(mostRecentOnBackPressedCallback)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Most recent callback should be incremented")
            .that(mostRecentOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent callback should be incremented")
            .that(onBackPressedCallback.count)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testPassthroughListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val passThroughOnBackPressedCallback = CountingOnBackPressedCallback(returnValue = false)

        dispatcher.addOnBackPressedCallback(onBackPressedCallback)
        dispatcher.addOnBackPressedCallback(passThroughOnBackPressedCallback)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Most recent callback should be incremented")
            .that(passThroughOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "return false")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallback() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOnBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOwner = object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)

            override fun getLifecycle() = lifecycleRegistry
        }

        dispatcher.addOnBackPressedCallback(onBackPressedCallback)
        dispatcher.observeOnBackPressed(lifecycleOwner, lifecycleOnBackPressedCallback)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(0)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "aren't started")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        // Now start the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Once the callbacks is started, the count should increment")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent callback should be incremented")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        // Now stop the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "aren't started")
            .that(onBackPressedCallback.count)
            .isEqualTo(2)

        // Now destroy the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        @Suppress("INACCESSIBLE_TYPE")
        assertWithMessage("onDestroy should trigger the removal of any associated callbacks")
            .that(dispatcher.mObservers[lifecycleOnBackPressedCallback])
            .isNull()
        assertWithMessage("Handler should return true when handling onBackPressed")
            .that(dispatcher.onBackPressed())
            .isTrue()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "aren't started")
            .that(onBackPressedCallback.count)
            .isEqualTo(3)
    }
}

class CountingOnBackPressedCallback(val returnValue: Boolean = true) :
    OnBackPressedCallback {
    var count = 0

    override fun handleOnBackPressed(): Boolean {
        count++
        return returnValue
    }
}
