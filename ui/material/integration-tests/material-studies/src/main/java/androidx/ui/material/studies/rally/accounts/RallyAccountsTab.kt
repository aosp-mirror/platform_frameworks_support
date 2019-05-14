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
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.VerticalScroller
import androidx.ui.material.studies.R
import androidx.ui.material.studies.rally.components.RallyCard
import androidx.ui.material.studies.rally.components.RallyCircleGraph
import androidx.ui.material.studies.rally.components.RallyCircleGraphItem
import androidx.ui.material.studies.rally.components.Tab
import androidx.ui.material.studies.rally.extensions.asDisplayString
import androidx.ui.material.themeTextStyle

val accountsTab = Tab(
    title = "ACCOUNTS",
    icon = R.drawable.ic_money_on
) {
    val graphItems = RallyAccountRepository.accounts.map {
        RallyCircleGraphItem(
            color = it.color,
            value = it.amount / RallyAccountRepository.total
        )
    }

    VerticalScroller {
        Column(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            RallyCircleGraph(graphItems) {
                Text(
                    text = "Total",
                    style = +themeTextStyle { subtitle1 }
                )
                Text(
                    text = RallyAccountRepository.total.asDisplayString(),
                    style = +themeTextStyle { h3 }
                )
            }

            RallyCard {
                RallyAccountList(RallyAccountRepository.accounts)
            }
        }
    }
}
