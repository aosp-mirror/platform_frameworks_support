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

package androidx.ui.material.demos

import android.util.Log
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.material.ContainedButton
import androidx.ui.material.MaterialTheme
import androidx.ui.material.samples.ContainedButtonSample
import androidx.ui.material.samples.OutlinedButtonSample
import androidx.ui.material.samples.SlottedContainedButtonSample
import androidx.ui.material.samples.SlottedOutlinedButtonSample
import androidx.ui.material.samples.SlottedTextButtonSample
import androidx.ui.material.samples.TextButtonSample
import androidx.ui.material.themeColor

@Composable
fun ButtonDemo() {
    val onClick: () -> Unit = { Log.e("ButtonDemo", "onClick") }
    CraneWrapper {
        MaterialTheme {
            Center {
                Column(mainAxisAlignment = MainAxisAlignment.SpaceEvenly) {
                    // Contained Buttons
                    ContainedButtonSample()
                    SlottedContainedButtonSample()
                    ContainedButton(
                        onClick = onClick,
                        color = +themeColor { secondary },
                        text = "SECONDARY COLOR")

                    // Outlined Buttons
                    OutlinedButtonSample()
                    SlottedOutlinedButtonSample()

                    // Text Buttons
                    TextButtonSample()
                    SlottedTextButtonSample()

                    // TODO(Andrey): Disabled button has wrong bg and text color for now.
                    // Need to figure out where will we store their styling. Not a part of
                    // MaterialColors right now and specs are not clear about this.
                    ContainedButton(text = "DISABLED. TODO")
                }
            }
        }
    }
}
