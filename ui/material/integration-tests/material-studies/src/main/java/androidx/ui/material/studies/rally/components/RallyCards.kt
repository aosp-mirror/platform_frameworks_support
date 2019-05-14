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

package androidx.ui.material.studies.rally.components

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.material.surface.Card
import androidx.ui.material.themeTextStyle
import androidx.ui.painting.Color
import androidx.compose.composer
import androidx.ui.layout.FlexRow

// TODO: This is not integrated with the theme
val cardInternalColor = Color(0xFF373740.toInt())

@Composable
fun RallyCard(@Children children: @Composable() () -> Unit) {
    Card(color = cardInternalColor) {
        FlexRow {
            flexible(flex = 1.0f) {
                Column {
                    children()
                }
            }
        }
    }
}

@Composable
fun RallyCardTitle(title: String, amount: String) {
    Padding(padding = 12.dp) {
        Column(crossAxisAlignment = MainAxisAlignment.Start) {
            Text(text = title, style = +themeTextStyle { subtitle1 })
            Text(text = amount, style = +themeTextStyle { h3 })
        }
    }
}