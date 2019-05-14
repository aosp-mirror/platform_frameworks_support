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

package androidx.ui.material.studies.rally.bills

import androidx.compose.composer
import androidx.compose.Composable
import androidx.ui.material.studies.rally.components.RallyCard
import androidx.ui.material.studies.rally.components.RallyCardTitle

/**
 * The Bills card within the Rally Overview screen.
 */
@Composable
fun RallyBillOverviewCard() {
    RallyCard {
        RallyCardTitle(title = "Bills", amount = "$1,810.00")
    }
}