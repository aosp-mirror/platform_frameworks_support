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

package androidx.ui.material.studies.rally.accounts

import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.unaryPlus
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.Colors
import androidx.ui.material.Divider
import androidx.ui.material.ripple.BoundedRipple
import androidx.ui.material.studies.Icon
import androidx.ui.material.studies.R
import androidx.ui.material.studies.rally.extensions.asDisplayString
import androidx.ui.material.themeTextStyle
import androidx.ui.painting.Color

@Composable
fun RallyAccountList(accounts: List<RallyAccountData>) {
    Column {
        val colors = +ambient(Colors)
        accounts.forEachIndexed { index, data ->
            if (index > 0) Divider(color = colors.surface, height = 1.dp, indent = 12.dp)
            RallyAccountRow(data)
        }
    }
}

/**
 * A row within the Accounts card in the Rally Overview screen.
 */
@Composable
fun RallyAccountRow(account: RallyAccountData) {
    BoundedRipple {
        Padding(12.dp) {
            Row(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
                Row {
                    AccountIndicator(color = account.color)
                    WidthSpacer(width = 8.dp)
                    Column(crossAxisAlignment = MainAxisAlignment.Start) {
                        Text(text = account.name, style = +themeTextStyle { subtitle1 })
                        Text(text = "•••••${account.number}", style = +themeTextStyle {
                            body2.copy(
                                letterSpacing = 0.2f
                            )
                        })
                    }
                }
                Row {
                    Text(text = "$", style = +themeTextStyle { body1 })
                    Container(
                        alignment = Alignment.CenterRight,
                        constraints = DpConstraints.tightConstraintsForWidth(125.dp)
                    ) {
                        Text(
                            text = account.amount.asDisplayString(),
                            style = +themeTextStyle {
                                body1.copy(
                                    fontSize = 20.0f,
                                    letterSpacing = 0.2f
                                )
                            })
                    }
                    WidthSpacer(width = 8.dp)
                    Container(width = 20.dp, height = 20.dp) {
                        Icon(R.drawable.ic_select_account, alpha = 0.65f)
                    }
                }
            }
        }
    }
}

/**
 * A vertical colored line that is used in a [RallyAccountRow] to differentiate accounts.
 */
@Composable
fun AccountIndicator(color: Color) {
    ColoredRect(color = color, width = 4.dp, height = 36.dp)
}