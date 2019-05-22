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

import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.MediumTest
import androidx.ui.layout.Column
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FindAllTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun checkBoxTest_twoComponents_togglesCreatesAnother() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    val (checked, onCheckedChange) = +state { false }

                    Column {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                onCheckedChange(it)
                            }
                        )
                        Checkbox(
                            checked = false,
                            onCheckedChange = null
                        )

                        if (checked) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = null
                            )
                        }
                    }
                }
            }
        }

        findAll { isCheckable() }.apply {
            get(0)
                .assertIsNotChecked()
                .doClick()
                .assertIsChecked()
        }

        findAll { isCheckable() }.apply {
            get(2)
                .assertIsNotChecked()
        }
    }

    @Test
    fun checkBoxTest_twoComponents_toggleDeletesOne() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    val (checked, onCheckedChange) = +state { false }

                    Column {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                onCheckedChange(it)
                            }
                        )
                        if (!checked) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = null
                            )
                        }
                    }
                }
            }
        }

        findAll { isCheckable() }.apply {
            get(0)
                .assertIsNotChecked()
                .doClick()
                .assertIsChecked()
            get(1)
                .assertNoLongerExists()
        }
    }
}