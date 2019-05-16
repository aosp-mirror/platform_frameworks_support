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

package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.core.ipx
import androidx.compose.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DividerUiTest : AndroidUiTestRunner() {

    private val defaultHeight = 1.dp

    @Test
    fun divider_DefaultSizes() {
        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            performSizeTest(dm.widthPixels.ipx, defaultHeight.toIntPx()) {
                Divider()
            }
        }
    }

    @Test
    fun divider_CustomSizes() {
        val height = 20.dp

        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            performSizeTest(dm.widthPixels.ipx, height.toIntPx()) {
                Divider(height = height)
            }
        }
    }

    @Test
    fun divider_SizesWithIndent_DoesNotChanged() {
        val indent = 75.dp
        val height = 21.dp

        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            performSizeTest(dm.widthPixels.ipx, height.toIntPx()) {
                Divider(indent = indent, height = height)
            }
        }
    }
}