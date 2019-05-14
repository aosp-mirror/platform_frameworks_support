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

import android.app.Activity
import android.os.Bundle
import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.material.studies.R
import androidx.ui.material.studies.Scaffold
import androidx.ui.material.studies.rally.accounts.accountsTab
import androidx.ui.material.studies.rally.bills.billsTab
import androidx.ui.material.studies.rally.components.Tab
import androidx.ui.material.studies.rally.components.TabLayout

/**
 * This Activity recreates the Rally Material Study from
 * https://material.io/design/material-studies/rally.html
 */
class RallyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                RallyApp()
            }
        }
    }

    @Composable
    fun RallyApp() {
        RallyTheme {
            Scaffold(appBar = { /* Rally doesn't have an AppBar */ }) {
                RallyDashboardScreen()
            }
        }
    }

    @Composable
    fun RallyDashboardScreen() {
        val state = +state { 0 }

        val tabs = listOf(
            overviewTab,
            accountsTab,
            billsTab,
            Tab(
                title = "BUDGET",
                icon = R.drawable.ic_bar_graph
            ) {},
            Tab(
                title = "SETTINGS",
                icon = R.drawable.ic_settings
            ) {}
        )

        TabLayout(
            selectedItem = state.value,
            items = tabs,
            onSelected = {
                state.value = it
            }
        )
    }
}