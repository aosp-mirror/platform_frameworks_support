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

package androidx.ui.material.studies.rally

import androidx.compose.composer
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.HeightSpacer
import androidx.ui.layout.Padding
import androidx.ui.layout.VerticalScroller
import androidx.ui.material.studies.R
import androidx.ui.material.studies.rally.accounts.RallyAccountOverviewCard
import androidx.ui.material.studies.rally.alerts.RallyAlertOverviewCard
import androidx.ui.material.studies.rally.bills.RallyBillOverviewCard
import androidx.ui.material.studies.rally.components.Tab

val overviewTab = Tab(
    title = "OVERVIEW",
    icon = R.drawable.ic_pie_chart
) {
    VerticalScroller {
        Padding(padding = 16.dp) {
            Column {
                RallyAlertOverviewCard()
                HeightSpacer(height = 10.dp)

                RallyAccountOverviewCard()
                HeightSpacer(height = 10.dp)

                RallyBillOverviewCard()
            }
        }
    }
}
