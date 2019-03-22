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

package androidx.fragment.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentFinishEarlyTest {

    @get:Rule
    val activityRule = ActivityTestRule(FragmentFinishEarlyTestActivity::class.java)

    /**
     * FragmentActivity should not raise the state of a Fragment while it is being destroyed.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    fun fragmentActivityFinishEarly() {
        val intent = Intent(
            activityRule.activity,
            FragmentFinishEarlyTestActivity::class.java
        )
        intent.putExtra("finishEarly", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val activity = InstrumentationRegistry.getInstrumentation()
            .startActivitySync(intent) as FragmentFinishEarlyTestActivity

        assertThat(activity.onDestroyLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    /**
     * A simple activity used for testing an Early Finishing Activity
     */
    class FragmentFinishEarlyTestActivity : FragmentActivity() {
        val onDestroyLatch = CountDownLatch(1)

        public override fun onCreate(icicle: Bundle?) {
            super.onCreate(icicle)
            if (intent?.getBooleanExtra("finishEarly", false) == true) {
                finish()
                supportFragmentManager.beginTransaction()
                    .add(AssertNotDestroyed(), "not destroyed")
                    .commit()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            onDestroyLatch.countDown()
        }

        class AssertNotDestroyed : Fragment() {
            override fun onActivityCreated(savedInstanceState: Bundle?) {
                super.onActivityCreated(savedInstanceState)
                assertThat(requireActivity().isDestroyed).isFalse()
            }
        }
    }
}