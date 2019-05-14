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

package androidx.ui.material.studies.rally.alerts

import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.material.Colors
import androidx.ui.material.Divider
import androidx.ui.material.TransparentButton
import androidx.ui.material.studies.Icon
import androidx.ui.material.studies.R
import androidx.ui.material.studies.rally.components.RallyCard
import androidx.ui.material.themeTextStyle

/**
 * The Alerts card within the Rally Overview screen.
 */
@Composable
fun RallyAlertOverviewCard() {
    RallyCard {
        Padding(padding = 12.dp) {
            Column {
                Row(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
                    Text(text = "Alerts", style = +themeTextStyle { subtitle1 })
                    TransparentButton(text = "SEE ALL", onClick = { })
                }
                Padding(padding = EdgeInsets(0.dp, 12.dp, 0.dp, 12.dp)) {
                    val colors = +ambient(Colors)
                    Divider(color = colors.surface, height = 1.dp)
                }
                FlexRow(crossAxisAlignment = CrossAxisAlignment.Start) {
                    expanded(flex = 1.0f) {
                        val text = "Heads up, you've used up 90% of your " +
                                "Shopping budget for this month."
                        Text(
                            style = +themeTextStyle { subtitle1 },
                            text = text
                        )
                    }
                    inflexible {
                        Container(width = 24.dp, height = 24.dp) {
                            Icon(R.drawable.ic_sort)
                        }
                    }
                }
            }
        }
    }
}