/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityOnBackPressedTest {

    @get:Rule
    val activityRule = ActivityTestRule(OnBackPressedComponentActivity::class.java)

    @UiThreadTest
    @Test
    fun testAddOnBackPressedListener() {
        val activity = activityRule.activity

        val onBackPressedHandler = CountingOnBackPressedHandler()

        activity.addOnBackPressedHandler(activity, onBackPressedHandler)
        activity.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedHandler.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRemoveOnBackPressedListener() {
        val activity = activityRule.activity

        val onBackPressedHandler = CountingOnBackPressedHandler()

        activity.addOnBackPressedHandler(activity, onBackPressedHandler)
        activity.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedHandler.count)
            .isEqualTo(1)

        activity.removeOnBackPressedHandler(onBackPressedHandler)
        activity.onBackPressed()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedHandler.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testMultipleCalls() {
        val activity = activityRule.activity

        val onBackPressedHandler = CountingOnBackPressedHandler()

        activity.addOnBackPressedHandler(activity, onBackPressedHandler)
        activity.onBackPressed()
        activity.onBackPressed()
        assertWithMessage("Count should be incremented after each onBackPressed")
            .that(onBackPressedHandler.count)
            .isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testMostRecentGetsPriority() {
        val activity = activityRule.activity

        val onBackPressedHandler = CountingOnBackPressedHandler()
        val mostRecentOnBackPressedHandler = CountingOnBackPressedHandler()

        activity.addOnBackPressedHandler(activity, onBackPressedHandler)
        activity.addOnBackPressedHandler(activity, mostRecentOnBackPressedHandler)
        activity.onBackPressed()
        assertWithMessage("Most recent handler should be incremented")
            .that(mostRecentOnBackPressedHandler.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent handler should be incremented")
            .that(onBackPressedHandler.count)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testPassthroughListener() {
        val activity = activityRule.activity

        val onBackPressedHandler = CountingOnBackPressedHandler()
        val passThroughOnBackPressedHandler = CountingOnBackPressedHandler(returnValue = false)

        activity.addOnBackPressedHandler(activity, onBackPressedHandler)
        activity.addOnBackPressedHandler(activity, passThroughOnBackPressedHandler)
        activity.onBackPressed()
        assertWithMessage("Most recent handler should be incremented")
            .that(passThroughOnBackPressedHandler.count)
            .isEqualTo(1)
        assertWithMessage("Previous handlers should be incremented if more recent handlers " +
                "return false")
            .that(onBackPressedHandler.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testLifecycleHandler() {
        val activity = activityRule.activity

        val onBackPressedHandler = CountingOnBackPressedHandler()
        val lifecycleOnBackPressedHandler = CountingOnBackPressedHandler()
        val lifecycleOwner = object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)

            override fun getLifecycle() = lifecycleRegistry
        }

        activity.addOnBackPressedHandler(activity, onBackPressedHandler)
        activity.addOnBackPressedHandler(lifecycleOwner, lifecycleOnBackPressedHandler)
        activity.onBackPressed()
        assertWithMessage("Non-started handlers shouldn't have their count incremented")
            .that(lifecycleOnBackPressedHandler.count)
            .isEqualTo(0)
        assertWithMessage("Previous handlers should be incremented if more recent handlers " +
                "aren't started")
            .that(onBackPressedHandler.count)
            .isEqualTo(1)

        // Now start the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        activity.onBackPressed()
        assertWithMessage("Once the handlers is started, the count should increment")
            .that(lifecycleOnBackPressedHandler.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent handler should be incremented")
            .that(onBackPressedHandler.count)
            .isEqualTo(1)

        // Now stop the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        activity.onBackPressed()
        assertWithMessage("Non-started handlers shouldn't have their count incremented")
            .that(lifecycleOnBackPressedHandler.count)
            .isEqualTo(1)
        assertWithMessage("Previous handlers should be incremented if more recent handlers " +
                "aren't started")
            .that(onBackPressedHandler.count)
            .isEqualTo(2)

        // Now destroy the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        @Suppress("INACCESSIBLE_TYPE")
        assertWithMessage("onDestroy should trigger the removal of any associated handlers")
            .that(activity.mOnBackPressedHandlers)
            .hasSize(1)
        activity.onBackPressed()
        assertWithMessage("Non-started handlers shouldn't have their count incremented")
            .that(lifecycleOnBackPressedHandler.count)
            .isEqualTo(1)
        assertWithMessage("Previous handlers should be incremented if more recent handlers " +
                "aren't started")
            .that(onBackPressedHandler.count)
            .isEqualTo(3)
    }
}

class CountingOnBackPressedHandler(val returnValue: Boolean = true) : OnBackPressedHandler {
    var count = 0

    override fun onBackPressed(): Boolean {
        count++
        return returnValue
    }
}

class OnBackPressedComponentActivity : ComponentActivity() {
    val activityCallbackLifecycleOwner: LifecycleOwner = mock(LifecycleOwner::class.java)
    val lifecycleObserver: GenericLifecycleObserver = mock(GenericLifecycleObserver::class.java)
    val destroyCountDownLatch = CountDownLatch(1)

    init {
        lifecycle.addObserver(lifecycleObserver)
    }

    override fun onDestroy() {
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        destroyCountDownLatch.countDown()
    }
}
