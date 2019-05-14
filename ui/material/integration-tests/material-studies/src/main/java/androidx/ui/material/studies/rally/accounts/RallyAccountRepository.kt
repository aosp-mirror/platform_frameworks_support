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

import androidx.ui.painting.Color

object RallyAccountRepository {
    val accounts = listOf(
            RallyAccountData(
                name = "Daily",
                number = "1234",
                amount = 2215.13f,
                color = Color(0xFF005D57.toInt())
            ),
            RallyAccountData(
                name = "Home Savings",
                number = "5678",
                amount = 8676.88f,
                color = Color(0xFF04B97F.toInt())
            ),
            RallyAccountData(
                name = "Car Savings",
                number = "9012",
                amount = 987.48f,
                color = Color(0xFF37EFBA.toInt())
            ),
            RallyAccountData(
                name = "Bills",
                number = "3455",
                amount = 1815.04f,
                color = Color(0xFF04B97F.toInt())
            ),
            RallyAccountData(
                name = "Holiday Savings",
                number = "4311",
                amount = 544.43f,
                color = Color(0xFF37EFBA.toInt())
            )
        )

    val total get() = accounts.map { it.amount }.sum()
}