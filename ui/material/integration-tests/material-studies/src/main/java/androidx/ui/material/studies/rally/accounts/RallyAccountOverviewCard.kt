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

import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.dp
import androidx.ui.layout.HeightSpacer
import androidx.ui.material.Colors
import androidx.ui.material.Divider
import androidx.ui.material.TransparentButton
import androidx.ui.material.studies.rally.components.RallyCard
import androidx.ui.material.studies.rally.components.RallyCardTitle
import androidx.ui.material.studies.rally.extensions.asDisplayString
import androidx.ui.material.studies.rally.rallyGreen

/**
 * The Accounts card within the Rally Overview screen.
 */
@Composable
fun RallyAccountOverviewCard() {
    val colors = +ambient(Colors)

    RallyCard {
        RallyCardTitle(title = "Accounts", amount = RallyAccountRepository.total.asDisplayString())
        Divider(color = rallyGreen, height = 1.dp)
        HeightSpacer(8.dp)

        RallyAccountList(RallyAccountRepository.accounts.take(3))
        Divider(color = colors.surface, height = 1.dp, indent = 12.dp)

        HeightSpacer(8.dp)
        TransparentButton(text = "SEE ALL", onClick = { })
        HeightSpacer(8.dp)
    }
}
