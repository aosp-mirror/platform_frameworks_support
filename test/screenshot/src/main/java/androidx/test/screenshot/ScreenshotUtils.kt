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

package androidx.test.screenshot

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.View
import androidx.annotation.RequiresApi
import androidx.test.screenshot.proto.ScreenshotResultProto.ScreenshotResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ScreenshotUtils(
    private val view: View,
    private val activity: Activity
) {
    private lateinit var handler: Handler
    private val screenshotStorageLocation = File("/sdcard", "androidx_screenshots")

    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenshot(): Bitmap {
        return waitAndScreenShot()
    }

    private fun getGoldenImage(goldenImageName: String): Bitmap? {
        val context = activity.applicationContext

        val defType = "drawable"
        val drawableResourceId = context.resources.getIdentifier(
            goldenImageName, defType, context.packageName)
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }

        return BitmapFactory.decodeResource(context.resources, drawableResourceId, options)
    }

    private fun writeToStorage(screenshotName: String, screenshot: Bitmap) {
        if (!screenshotStorageLocation.exists() && !screenshotStorageLocation.mkdir()) {
            throw IOException("Could not create folder.")
        }

        val file = File(screenshotStorageLocation, "$screenshotName.png")

        try {
            val stream = FileOutputStream(file)
            screenshot.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, stream)
            stream.flush()
        } catch (e: Exception) {
            throw IOException("Could not write image to storage (path: ${file.absolutePath}). " +
                    " Stacktrace: " + e.stackTrace)
        }
    }

    private fun writeToStorage(messageName: String, message: ScreenshotResult) {
        if (!screenshotStorageLocation.exists() && !screenshotStorageLocation.mkdir()) {
            throw IOException("Could not create folder.")
        }

        val file = File(screenshotStorageLocation, "$messageName.txt")

        try {
            val stream = FileOutputStream(file)
            stream.write(message.toString().toByteArray())
        } catch (e: Exception) {
            throw IOException("Could not write file to storage (path: ${file.absolutePath}). " +
                    " Stacktrace: " + e.stackTrace)
        }
    }

    // TODO(b/129835519): create a backward compatible method
    @RequiresApi(Build.VERSION_CODES.O)
    private fun waitAndScreenShot(): Bitmap {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activity.runOnUiThread(runnable)

        val offset = intArrayOf(0, 0)
        val width = view.width
        val height = view.height

        val dest =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcRect = android.graphics.Rect(0, 0, width, height)
        srcRect.offset(offset[0], offset[1])
        val latch = CountDownLatch(1)
        var copyResult = 0
        val onCopyFinished = object : PixelCopy.OnPixelCopyFinishedListener {
            override fun onPixelCopyFinished(result: Int) {
                copyResult = result
                latch.countDown()
            }
        }
        PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler)
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, copyResult)
        return dest
    }

    /**
     * Takes a screenshot, retrieves the golden image and compares them
     *
     * @goldenScreenshot name of the golden image that the screenshot will be compared to
     *
     * @throws AssertionError if the comparison fails
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun assertScreenshotIsEqualTo(
        goldenScreenshotName: String
    ) {
        val screenshot = takeScreenshot()
        val golden = getGoldenImage(goldenScreenshotName)
        val partialResult = compareBitmaps(golden, screenshot)

        val result = ScreenshotResult.newBuilder(partialResult)

        result.imageLocationGolden = "$goldenScreenshotName.png"
        val screenshotName = goldenScreenshotName + "_test"

        if (result.result != ScreenshotResult.Status.PASSED) {
            writeToStorage(screenshotName, screenshot)
            result.imageLocationTest = "$screenshotName.png"
        }

        writeToStorage(goldenScreenshotName + "_result", result.build())

        if (result.result == ScreenshotResult.Status.MISSING_GOLDEN) {
            throw AssertionError("Missing golden image! " +
                    "Did you mean to check in a new image?")
        }

        if (result.result == ScreenshotResult.Status.FAILED &&
            golden!!.height != screenshot.height) {
            throw AssertionError("Sizes are different!")
        }

        if (result.result == ScreenshotResult.Status.FAILED) {
            throw AssertionError("Images are different!")
        }
    }
}
