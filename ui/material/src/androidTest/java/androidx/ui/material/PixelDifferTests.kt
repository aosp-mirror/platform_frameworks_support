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

package androidx.ui.material.test

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.test.filters.MediumTest
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.compareBitmaps
import androidx.test.screenshot.ScreenshotResultProto.ScreenshotResult.Status
import org.junit.Assert
import org.junit.Test

class PixelDifferTests : AndroidUiTestRunner() {

    @Test
    @MediumTest
    fun pixelDiffer_imagesMatch() {
        Assert.assertTrue(
            "Images don't match",
            compareBitmaps(
                createImage(),
                createImage()
            ).result == Status.PASSED
        )
    }

    @Test
    @MediumTest
    fun pixelDiffer_colorsDontMatch() {
        Assert.assertTrue(
            "Image colors shouldn't match",
            compareBitmaps(
                createImage(Color.BLACK),
                createImage(Color.WHITE)
            ).result != Status.PASSED
        )
    }

    @Test
    @MediumTest
    fun pixelDiffer_sizesDontMatch() {
        Assert.assertTrue(
            "Image sizes shouldn't match",
            compareBitmaps(
                createImage(),
                createImage(size = 20)
            ).result != Status.PASSED
        )
    }

    @Test
    @MediumTest
    fun pixelDiffer_colorOffByOne() {
        Assert.assertTrue(
            "Rectangle sizes shouldn't match.",
            compareBitmaps(
                createImage(),
                createImage(origin = 1)
            ).result != Status.PASSED
        )
    }

    /**
     * Lays out rectangles in a 2 by 2 grid
     */
    private fun createImage(
        @ColorInt color1: Int = Color.BLACK,
        @ColorInt color2: Int = Color.RED,
        @ColorInt color3: Int = Color.WHITE,
        @ColorInt color4: Int = Color.TRANSPARENT,
        size: Int = 10,
        origin: Int = 0
    ) =
        Bitmap.createBitmap(size - 1, size - 1, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint()
            val offset = size / 2
            val newRect = {
                Rect(0, 0, offset - 1, offset - 1)
            }

            paint.color = color1
            canvas.drawRect(newRect().apply { offset(origin, origin) }, paint)

            paint.color = color2
            canvas.drawRect(newRect().apply { offset(offset, origin) }, paint)

            paint.color = color3
            canvas.drawRect(newRect().apply { offset(origin, offset) }, paint)

            paint.color = color4
            canvas.drawRect(newRect().apply { offset(offset, offset) }, paint)
        }
}