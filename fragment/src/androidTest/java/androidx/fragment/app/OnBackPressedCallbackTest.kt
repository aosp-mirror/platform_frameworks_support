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

package androidx.fragment.app

import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OnBackPressedCallbackTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @UiThreadTest
    @Test
    fun testOnBackPressed() {
        val activity = activityRule.activity
        var onBackPressed = false
        activity.addOnBackPressedCallback {
            onBackPressed = true
            false
        }
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
        val fragmentManager = activity.supportFragmentManager
        val fragment = StrictFragment()
        fragmentManager.beginTransaction()
=======
    }

    @Suppress("DEPRECATION")
    @Test
    fun testBackPressWithFrameworkFragment() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { fragmentManager }
            val fragment = android.app.Fragment()

            fragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            onActivity {
                fragmentManager.executePendingTransactions()
            }
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)

            withActivity { onBackPressed() }

            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isNull()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testBackPressWithFragmentOverFrameworkFragment() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { fragmentManager }
            val fragment = android.app.Fragment()

            fragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            onActivity {
                fragmentManager.executePendingTransactions()
            }
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)

            val supportFragmentManager = withActivity { supportFragmentManager }
            val supportFragment = StrictFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.content, supportFragment)
                .addToBackStack(null)
                .commit()
            onActivity {
                supportFragmentManager.executePendingTransactions()
            }
            assertThat(supportFragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(supportFragment)

            withActivity { onBackPressed() }

            assertThat(supportFragmentManager.findFragmentById(R.id.content))
                .isNull()
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testBackPressWithCallbackOverFrameworkFragment() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { fragmentManager }
            val fragment = android.app.Fragment()

            fragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            onActivity {
                fragmentManager.executePendingTransactions()
            }
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)

            val callback = CountingOnBackPressedCallback()
            withActivity {
                onBackPressedDispatcher.addCallback(callback)

                onBackPressed()
            }

            assertThat(callback.count)
                .isEqualTo(1)
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)
        }
    }

    @Test
    fun testBackPressWithCallbackOverFragment() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }
            val fragment = StrictFragment()
            fragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack("back_stack")
                .commit()
            onActivity {
                fragmentManager.executePendingTransactions()
            }
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)

            val callback = CountingOnBackPressedCallback()
            withActivity {
                onBackPressedDispatcher.addCallback(callback)

                onBackPressed()
            }

            assertWithMessage("OnBackPressedCallbacks should be called before FragmentManager")
                .that(callback.count)
                .isEqualTo(1)
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)
        }
    }

    @Test
    fun testBackPressFinishesActivityAfterFragmentPop() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }
            val fragment = StrictFragment()
            fragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack("back_stack")
                .commit()
            onActivity {
                fragmentManager.executePendingTransactions()
            }
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)

            withActivity {
                fragmentManager.popBackStack()

                onBackPressed()

                assertThat(fragmentManager.findFragmentById(R.id.content))
                    .isNull()
                assertWithMessage("Activity should be finishing after onBackPressed() " +
                        "on an empty back stack")
                    .that(isFinishing)
                    .isTrue()
            }
        }
    }

    @Test
    fun testBackPressWithFragmentCallbackOverFragmentManager() {
        with(ActivityScenario.launch(OnBackPressedFragmentActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }
            val fragment = withActivity { fragment }
            val fragmentCallback = fragment.onBackPressedCallback

            withActivity {
                onBackPressed()
            }

            assertWithMessage("Fragment callback should be called before FragmentManager")
                .that(fragmentCallback.count)
                .isEqualTo(1)
            assertThat(fragmentManager.findFragmentById(R.id.content))
                .isSameInstanceAs(fragment)
        }
    }
}

class CountingOnBackPressedCallback(enabled: Boolean = true) : OnBackPressedCallback(enabled) {
    var count = 0

    override fun handleOnBackPressed() {
        count++
    }
}

class OnBackPressedStrictFragment : StrictFragment() {
    val onBackPressedCallback = CountingOnBackPressedCallback()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
}

class OnBackPressedFragmentActivity : FragmentActivity(R.layout.activity_content) {
    val fragment: OnBackPressedStrictFragment get() =
        supportFragmentManager.findFragmentById(R.id.content) as OnBackPressedStrictFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment = OnBackPressedStrictFragment()
        supportFragmentManager.beginTransaction()
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
            .replace(R.id.content, fragment)
            .addToBackStack("back_stack")
            .commit()
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)
        activity.onBackPressed()
        assertWithMessage("OnBackPressedCallbacks should be called before FragmentManager")
            .that(onBackPressed)
            .isTrue()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isNull()
    }
}
