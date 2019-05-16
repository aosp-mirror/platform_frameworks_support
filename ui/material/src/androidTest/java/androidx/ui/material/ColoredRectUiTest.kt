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
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.layout.DpConstraints
import androidx.ui.graphics.Color
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.core.ipx
import androidx.compose.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ColoredRectUiTest : AndroidUiTestRunner() {

    private val color = Color(0xFFFF0000.toInt())

    @Test
    fun coloredRect_fixedSizes() {
        val width = 40.dp
        val height = 71.dp

        withDensity(density) {
            performSizeTest(
                width.toIntPx(),
                height.toIntPx()
            ) {
                ColoredRect(width = width, height = height, color = color)
            }
        }
    }

    @Test
    fun coloredRect_expand_LimitedSizes() {
        val width = 40.dp
        val height = 71.dp

        withDensity(density) {
            performSizeTest(
                width.toIntPx(),
                height.toIntPx(),
                DpConstraints.tightConstraints(width, height)
            ) {
                ColoredRect(color = color)
            }
        }
    }

    @Test
    fun coloredRect_expand_WholeScreenSizes() {
        val dm = activityTestRule.activity.resources.displayMetrics
        performSizeTest(dm.widthPixels.ipx, dm.heightPixels.ipx) {
            ColoredRect(color = color)
        }
    }
}