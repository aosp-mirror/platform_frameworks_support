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
package androidx.ui.material

import androidx.compose.composer
import androidx.test.filters.MediumTest
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.baseui.selection.ToggleableState.Checked
import androidx.ui.baseui.selection.ToggleableState.Indeterminate
import androidx.ui.baseui.selection.ToggleableState.Unchecked
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.TestTag
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.test.assertChecked
import androidx.ui.test.assertNotChecked
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.copyWith
import androidx.ui.test.createComposeRule
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.isCheckable
import com.google.common.truth.Truth
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.test.assertDoesNotExist
import androidx.ui.test.expectExactly
import androidx.ui.test.find
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class CheckboxUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    // TODO(b/126881459): this should be the default semantic for checkbox
    private val defaultCheckboxCheckedSemantics = createFullSemantics(
        isEnabled = true,
        isChecked = true
    )

    private val defaultCheckboxUncheckedSemantics = defaultCheckboxCheckedSemantics.copyWith {
        isChecked = false
    }

    private val defaultTag = "myCheckbox"

    private val bigConstraints = DpConstraints(
        minWidth = 0.dp,
        minHeight = 0.dp,
        maxHeight = 1000.dp,
        maxWidth = 1000.dp
    )

    private val materialCheckboxSize = 24.dp

    @Test
    fun checkBoxTest_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Column {
                TestTag(tag = "checkboxUnchecked") {
                    Checkbox(false, {})
                }
                TestTag(tag = "checkboxChecked") {
                    Checkbox(true, {})
                }
            }
        }

        findByTag("checkboxUnchecked")
            .assertSemanticsIsEqualTo(defaultCheckboxUncheckedSemantics)

        findByTag("checkboxChecked")
            .assertSemanticsIsEqualTo(defaultCheckboxCheckedSemantics)
    }

    @Test
    fun checkBoxTest_toggle() {
        composeTestRule.setMaterialContent {
            val (checked, onCheckedChange) = +state { false }
            TestTag(tag = defaultTag) {
                Checkbox(checked, onCheckedChange)
            }
        }

        findByTag(defaultTag)
            .assertNotChecked()
            .doClick()
            .assertChecked()
    }

    @Test
    fun checkBoxTest_toggle_twice() {
        composeTestRule.setMaterialContent {
            val (checked, onCheckedChange) = +state { false }
            TestTag(tag = defaultTag) {
                Checkbox(checked, onCheckedChange)
            }
        }

        findByTag(defaultTag)
            .assertNotChecked()
            .doClick()
            .assertChecked()
            .doClick()
            .assertNotChecked()
    }

    @Test
    fun checkBoxTest_untoggleable_whenNoLambda() {

        composeTestRule.setMaterialContent {
            val (checked, _) = +state { false }
            TestTag(tag = defaultTag) {
                Checkbox(checked, null)
            }
        }

        findByTag(defaultTag)
            .assertNotChecked()
            .doClick()
            .assertNotChecked()
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenChecked() {
        materialSizeTestForValue(Checked)
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenUnchecked() {
        materialSizeTestForValue(Unchecked)
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenIndeterminate() {
        materialSizeTestForValue(Indeterminate)
    }

    private fun materialSizeTestForValue(checkboxValue: ToggleableState) {
        var checkboxSize: PxSize? = null

        composeTestRule.setMaterialContent {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { coordinates ->
                    checkboxSize = coordinates.size
                }) {
                    TriStateCheckbox(value = checkboxValue, onClick = null)
                }
            }
        }
        withDensity(composeTestRule.density) {
            Truth.assertThat(checkboxSize?.width?.round()).isEqualTo(materialCheckboxSize.toIntPx())
            Truth.assertThat(checkboxSize?.height?.round())
                .isEqualTo(materialCheckboxSize.toIntPx())
        }
    }

    @Test
    fun checkBoxTest_twoComponents_areChecked() {
        composeTestRule.setMaterialContent {
            Column {
                Checkbox(checked = true, onCheckedChange = null)
                Checkbox(checked = true, onCheckedChange = null)
            }
        }

        find(expectExactly(2)) { isCheckable() && isChecked == true }
            .assertChecked()
    }

    @Test
    fun checkBoxTest_twoComponents_toggle() {
        composeTestRule.setMaterialContent {
            val (checked1, onCheckedChange1) = +state { false }
            val (checked2, onCheckedChange2) = +state { false }

            Column {
                Checkbox(
                    checked = checked1,
                    onCheckedChange = onCheckedChange1
                )
                Checkbox(
                    checked = checked2,
                    onCheckedChange = onCheckedChange2
                )
            }
        }

        find(expectExactly(2)) { isCheckable() }
            .doClick()
            .assertChecked()
    }

    @Test
    fun checkBoxTest_doesNotExist() {
        composeTestRule.setMaterialContent {
            Column {
                Checkbox(checked = true, onCheckedChange = null)
                Checkbox(checked = true, onCheckedChange = null)
            }
        }

        find { isCheckable() && isChecked == false }
            .assertDoesNotExist()
    }
}