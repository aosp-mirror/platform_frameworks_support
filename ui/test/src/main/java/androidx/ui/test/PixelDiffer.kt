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

package androidx.ui.test

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.screenshot.ScreenshotResultProto.ScreenshotResult
import androidx.test.screenshot.ScreenshotResultProto.ScreenshotResult.Status

/**
 * Compares the two given bitmaps pixel by pixel.
 * Returns true if the images match, false otherwise
 */
// TODO(b/129061636): add support for 'similar' pixels
fun compareBitmaps(reference: Bitmap?, test: Bitmap): ScreenshotResult {
    val result = ScreenshotResult.newBuilder()

    if (reference == null) {
        result.result = Status.MISSING_GOLDEN
        return result.build()
    }

    val maxWidth = Math.max(reference.width, test.width)
    val minWidth = Math.min(reference.width, test.width)
    val maxHeight = Math.max(reference.height, test.height)
    val minHeight = Math.min(reference.height, test.height)

    var different = getImageSizeDifferenceInPixels(minWidth, maxWidth, minHeight, maxHeight)

    if (different > 0) {
        result.result = Status.FAILED
        return result.build()
    }

    var same = 0
    val diff = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)

    var x = 0
    var y = 0
    while (x < minWidth) {
        while (y < minHeight) {
            val referenceColor = reference.getPixel(x, y)
            val testColor = test.getPixel(x, y)

            if (referenceColor == testColor) {
                diff.setPixel(x, y, Color.TRANSPARENT)
                ++same
            } else {
                diff.setPixel(x, y, Color.MAGENTA)
                ++different
            }

            ++y
        }
        ++x
    }

    result.setComparisonStatistics(ScreenshotResult.ComparisonStatistics.newBuilder().apply {
        numberPixelsIdentical = same
        numberPixelsDifferent = different
    })

    if (different > 0) {
        result.result = Status.FAILED
    } else {
        result.result = Status.PASSED
    }

    return result.build()
}

private fun getImageSizeDifferenceInPixels(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int
): Int {
    val deltaX = maxWidth - minWidth
    val deltaY = maxHeight - minHeight
    return deltaX * maxHeight + deltaY * maxWidth - deltaX * deltaY
}
