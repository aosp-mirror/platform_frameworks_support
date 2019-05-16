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

import android.graphics.Bitmap
import androidx.test.filters.MediumTest
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.painting.Image
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.compose.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FloatingActionButtonUiTest : AndroidUiTestRunner() {

    @Test
    fun defaultFabHasSizeFromSpec() = withDensity(density) {
        val expectedSize = 56.dp.toIntPx()
        performSizeTest(expectedSize, expectedSize) {
            FloatingActionButton(icon = createImage())
        }
    }

    @Test
    fun extendedFabHasHeightFromSpec() = withDensity(density) {
        performHeightTest(48.dp.toIntPx()) {
            FloatingActionButton(icon = createImage(), text = "Extended")
        }
    }

    private fun createImage() = withDensity(density) {
        val size = 24.dp.toIntPx().value
        Image(Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888))
    }
}
