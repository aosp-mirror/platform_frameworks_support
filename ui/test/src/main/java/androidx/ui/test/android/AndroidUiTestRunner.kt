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

package androidx.ui.test.android

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.CompositionContext
import androidx.compose.composer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.annotation.RequiresApi
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.test.UiTestRunner
import org.junit.After
import androidx.ui.test.compareBitmaps
import androidx.test.screenshot.ScreenshotResultProto.ScreenshotResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Allows to run ui tests on Android.
 *
 * Please not that you need to add the following activity
 * [androidx.ui.test.android.DefaultTestActivity] to you tests manifest file in order to use this.
 */
// TODO(pavlis): Move this to android specific directory
open class AndroidUiTestRunner : UiTestRunner {

    // we should not wait more than two frames, but two frames can be much more
    // than 32ms when we skip a few, so "better" 10x number should work here
    private val defaultRecomposeWaitTimeMs = 320L

    @get:Rule
    val activityTestRule = ActivityTestRule<DefaultTestActivity>(DefaultTestActivity::class.java)

    private lateinit var activity: DefaultTestActivity
    private lateinit var instrumentation: Instrumentation
    private lateinit var rootProvider: SemanticsTreeProvider
    private var compositionContext: CompositionContext? = null
    private lateinit var handler: Handler
    private val screenshotStorageLocation = File("/sdcard", "androidx_screenshots")

    val density: Density get() = Density(activity)

    @Before
    // TODO(pavlis): This is not great, if super forgets to call this (if redefining @before).
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    @After
    fun teardown() {
        compositionContext = null
    }

    private fun runOnUiThread(action: () -> Unit) {
        activity.runOnUiThread(object : Runnable {
            override fun run() {
                action.invoke()
            }
        })
    }

    @Composable
    fun TestWrapper(@Children children: @Composable() () -> Unit) {
        CraneWrapper {
            children()
        }
    }

    fun runOnUiAndWaitForRecompose(runnable: () -> Unit) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw Exception("Cannot be run on UI thread.")
        }
        val latch = CountDownLatch(1)
        runOnUiThread {
            runWithCompositionContext {
                addPostRecomposeObserver {
                    latch.countDown()
                }
                runnable.invoke()
            }
        }
        latch.await(defaultRecomposeWaitTimeMs, TimeUnit.MILLISECONDS)
    }

    private fun <T> runWithCompositionContext(action: CompositionContext.() -> T): T {
        val cc = compositionContext
        if (cc != null) {
            return cc.run(action)
        } else {
            // forcing exception here as we don't want to work here without context
            throw IllegalAccessException(
                "Cannot find composition context to wait for recompose. Did you substitute" +
                        "CraneWrapper for TestWrapper in your tests?"
            )
        }
    }

    private fun findCompositionRootProvider(): SemanticsTreeProvider {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return (findNode(contentViewGroup) {
            it is SemanticsTreeProvider
        }!!) as SemanticsTreeProvider
    }

    private fun findAndroidCraneView(): View {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findNode(contentViewGroup) { true/*it is AndroidCraneView*/ }!!
    }

    private fun findNode(parent: ViewGroup, matcher: (View) -> Boolean): View? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (matcher(child)) {
                return child
            } else if (child is ViewGroup) {
                val craneView = findNode(child, matcher)
                if (craneView != null) {
                    return craneView
                }
            }
        }
        return null
    }

    /**
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    fun setContent(composable: @Composable() () -> Unit) {
        val drawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawLatch.countDown()
                val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
                contentViewGroup.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        val runnable: Runnable = object : Runnable {
            override fun run() {
                setContentInternal(composable)
                val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
                contentViewGroup.viewTreeObserver.addOnGlobalLayoutListener(listener)
            }
        }
        activityTestRule.runOnUiThread(runnable)
        drawLatch.await(1, TimeUnit.SECONDS)

        // TODO(pavlis): There are several things missing
        // 1) What about multiple roots?
        // 2) What about the composition changing so much that current provider is no longer valid?
        // => Maybe the providers can't be cached like this?
        rootProvider = findCompositionRootProvider()
    }

    private fun setContentInternal(composable: @Composable() () -> Unit) {
        activity.setContentView(FrameLayout(activity).apply {
            compositionContext = Compose.composeInto(this, null, composable = {
                TestWrapper {
                    composable()
                }
            })
        })
    }

    override fun findSemantics(selector: (SemanticsTreeNode) -> Boolean): List<SemanticsTreeNode> {
        return rootProvider.getAllSemanticNodes().filter { selector(it) }.toList()
    }

    override fun performClick(x: Float, y: Float) {
        runOnUiAndWaitForRecompose {
            val eventDown = MotionEvent.obtain(
                SystemClock.uptimeMillis(), 10,
                MotionEvent.ACTION_DOWN, x, y, 0
            )
            rootProvider.sendEvent(eventDown)
            eventDown.recycle()

            val eventUp = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                10,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
            )
            rootProvider.sendEvent(eventUp)
            eventUp.recycle()
        }
    }

    override fun sendEvent(event: MotionEvent) {
        runOnUiAndWaitForRecompose {
            rootProvider.sendEvent(event)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun takeScreenshot(): Bitmap {
        return waitAndScreenShot()
    }

    override fun getGoldenImage(goldenImageName: String): Bitmap? {
        val context = activity.applicationContext

        val defType = "drawable"
        val drawableResourceId = context.resources.getIdentifier(
            goldenImageName, defType, context.packageName)
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }

        return BitmapFactory.decodeResource(context.resources, drawableResourceId, options)
    }

    override fun writeToStorage(screenshotName: String, screenshot: Bitmap) {
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

    // TODO: ported from ContainingViewTest, should refactor that test to use this
    // TODO: create a backward compatible method
    @RequiresApi(Build.VERSION_CODES.O)
    private fun waitAndScreenShot(): Bitmap {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        instrumentation = InstrumentationRegistry.getInstrumentation()
        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activityTestRule.runOnUiThread(runnable)

        val view = findAndroidCraneView()
        val flushListener = DrawCounterListener(view)
        val offset = intArrayOf(0, 0)
        val addPreDrawListener = object : Runnable {
            override fun run() {
                view.getLocationInWindow(offset)
                view.viewTreeObserver.addOnPreDrawListener(flushListener)
                view.invalidate()
            }
        }
        activityTestRule.runOnUiThread(addPreDrawListener)

        assertTrue(flushListener.latch.await(1, TimeUnit.SECONDS))
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

class DefaultTestActivity : Activity() {
    var hasFocusLatch = CountDownLatch(1)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hasFocusLatch.countDown()
        }
    }
}

fun doPollingCheck(canProceed: () -> Boolean, timeoutPeriod: Long = 3000) {
    val timeSlice: Long = 50
    var timeout = timeoutPeriod

    if (canProceed()) {
        return
    }

    while (timeout > 0) {
        try {
            Thread.sleep(timeSlice)
        } catch (e: InterruptedException) {
            throw Exception("Unexpected InterruptedException")
        }

        if (canProceed()) {
            return
        }

        timeout -= timeSlice
    }

    throw Exception("Unexpected timeout")
}

private class DrawCounterListener(private val view: View) :
    ViewTreeObserver.OnPreDrawListener {
    val latch = CountDownLatch(5)

    override fun onPreDraw(): Boolean {
        latch.countDown()
        if (latch.count > 0) {
            view.postInvalidate()
        } else {
            view.getViewTreeObserver().removeOnPreDrawListener(this)
        }
        return true
    }
}
