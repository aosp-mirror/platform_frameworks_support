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

package androidx.ui.material.test

import android.os.Build
import androidx.compose.composer
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.TestTag
import androidx.ui.material.Checkbox
import androidx.ui.material.CheckboxState
import androidx.ui.material.setMaterialContent
import androidx.ui.test.DisableTransitions
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class CheckboxScreenshotTest : AndroidUiTestRunner() {

    @get:Rule
    val disableTransitions = DisableTransitions()

    private val defaultTag = "myCheckbox"

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // TODO(b/129835519)
    fun checkBoxTest_uncheckedScreenshot() {
        setMaterialContent {
            Checkbox(value = ToggleableState.Unchecked)
        }

        assertScreenshotIsEqualTo("checkbox_unchecked_golden_image")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // TODO(b/129835519)
    fun checkBoxTest_uncheckedThenTapScreenshot() {
        val state = CheckboxState(value = ToggleableState.Unchecked)

        setMaterialContent {
            TestTag(tag = defaultTag) {
                Checkbox(
                    value = state.value,
                    onClick = {
                        state.toggle()
                    })
            }
        }

        findByTag(defaultTag)
            .doClick()

        assertScreenshotIsEqualTo("checkbox_checked_golden_image")
    }
}