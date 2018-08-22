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

package androidx.navigation

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@MediumTest
@RunWith(AndroidJUnit4::class)
class ActivityNavigatorTest {
    companion object {
        const val TARGET_ID = 1
        const val TARGET_ACTION = "test_action"
        val TARGET_DATA: Uri = Uri.parse("http://www.example.com")
        const val TARGET_ARGUMENT_NAME = "test"
        const val TARGET_DATA_PATTERN = "http://www.example.com/{$TARGET_ARGUMENT_NAME}"
        const val TARGET_ARGUMENT_VALUE = "data_pattern"
    }

    @get:Rule
    val activityRule = ActivityTestRule(ActivityNavigatorActivity::class.java)

    @Before
    fun setup() {
        TargetActivity.instances = spy(ArrayList())
    }

    @After
    fun cleanup() {
        TargetActivity.instances.forEach { activity ->
            activity.finish()
        }
    }

    @Test
    fun navigate() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        activityNavigator.navigate(targetDestination, null, null)
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should not include FLAG_ACTIVITY_NEW_TASK",
                0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun navigateFromNonActivityContext() {
        val activityNavigator = ActivityNavigator(activityRule.activity.applicationContext)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        activityNavigator.navigate(targetDestination, null, null)
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should include FLAG_ACTIVITY_NEW_TASK",
                Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun navigateSingleTop() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        activityNavigator.navigate(targetDestination, null, navOptions {
            launchSingleTop = true
        })
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should include FLAG_ACTIVITY_SINGLE_TOP",
                Intent.FLAG_ACTIVITY_SINGLE_TOP, intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    @Test
    fun navigateWithArgs() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }

        val args = Bundle().apply {
            putString(TARGET_ARGUMENT_NAME, TARGET_ARGUMENT_VALUE)
        }
        activityNavigator.navigate(targetDestination, args, null)
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should have its arguments in its extras",
                TARGET_ARGUMENT_VALUE, intent.getStringExtra(TARGET_ARGUMENT_NAME))
    }

    @Test
    fun navigateAction() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            action = TARGET_ACTION
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        activityNavigator.navigate(targetDestination, null, null)
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should have action set",
                TARGET_ACTION, intent.action)
    }

    @Test
    fun navigateData() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            data = TARGET_DATA
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        activityNavigator.navigate(targetDestination, null, null)
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should have data set",
                TARGET_DATA, intent.data)
    }

    @Test
    fun navigateDataPattern() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            dataPattern = TARGET_DATA_PATTERN
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        val args = Bundle().apply {
            putString(TARGET_ARGUMENT_NAME, TARGET_ARGUMENT_VALUE)
        }
        activityNavigator.navigate(targetDestination, args, null)
        verify(onNavigatedListener).onNavigatorNavigated(activityNavigator, TARGET_ID,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(onNavigatedListener)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertNotNull(intent)
        assertEquals("Intent should have data set with argument filled in",
                TARGET_DATA_PATTERN.replace("{$TARGET_ARGUMENT_NAME}", TARGET_ARGUMENT_VALUE),
                intent.data?.toString())
        assertEquals("Intent should have its arguments in its extras",
                TARGET_ARGUMENT_VALUE, intent.getStringExtra(TARGET_ARGUMENT_NAME))
    }

    @Test
    fun navigateDataPatternMissingArgument() {
        val activityNavigator = ActivityNavigator(activityRule.activity)
        val onNavigatedListener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        activityNavigator.addOnNavigatorNavigatedListener(onNavigatedListener)

        val targetDestination = activityNavigator.createDestination().apply {
            id = TARGET_ID
            dataPattern = TARGET_DATA_PATTERN
            setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
        }
        try {
            val args = Bundle()
            activityNavigator.navigate(targetDestination, args, null)
            fail("navigate() should fail if required arguments are not included")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        verifyNoMoreInteractions(onNavigatedListener)
    }

    private fun waitForActivity(): TargetActivity {
        verify(TargetActivity.instances, timeout(3000)).add(any())
        verifyNoMoreInteractions(TargetActivity.instances)
        val targetActivity: ArrayList<TargetActivity> = ArrayList()
        activityRule.runOnUiThread {
            targetActivity.addAll(TargetActivity.instances)
        }
        assertTrue("Only expected a single TargetActivity", targetActivity.size == 1)
        return targetActivity[0]
    }
}

class ActivityNavigatorActivity : Activity()
class TargetActivity : Activity() {
    companion object {
        var instances: ArrayList<TargetActivity> = spy(ArrayList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instances.add(this)
    }
}
