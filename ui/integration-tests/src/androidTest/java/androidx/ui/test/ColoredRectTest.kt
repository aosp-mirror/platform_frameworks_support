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

package androidx.ui.test

import androidx.compose.Recomposer
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


/**
 * Ensure correctness of [RectanglesInColumnTestCase].
 */
@MediumTest
@RunWith(Parameterized::class)
class ColoredRectTest(private val amountOfCheckboxes: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Collection<Array<Any>> {
            return listOf(
                arrayListOf(1).toArray(),
                arrayListOf(10).toArray())
        }
    }

    @get:Rule
    val activityRule = ActivityTestRule(DefaultTestActivity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    @get:Rule
    val choreographerRule = ComposeChoreographerRule()


    @Test
    @SuppressWarnings
    fun toggleRectangleColor_compose() {
        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                val testCase = RectanglesInColumnTestCase(activityRule.activity, amountOfCheckboxes)
                    .apply { runSetup() }

                // TODO: Why invocation of custom choreographer does not work and compose() does?
                //testCase.compositionContext.compose()
                choreographerRule.newFrame()

                // TODO: None of these asserts work properly with our choreographer? Why?
                // Initialize
                Truth.assertThat(Recomposer.hasPendingChanges()).isFalse()

                // Change state
                testCase.toggleState()
                Truth.assertThat(Recomposer.hasPendingChanges()).isTrue()

                // Recompose
                choreographerRule.newFrame()
                Truth.assertThat(Recomposer.hasPendingChanges()).isFalse()

                //grabViewBitmapAndPutToActivity(testCase.rootView, activityRule.activity)

                // TODO: Why invocation of custom choreographer does not work and compose() does?
                //testCase.compositionContext.compose()
                choreographerRule.newFrame()
            }
        })

        //Thread.sleep(2000)

    }

}
