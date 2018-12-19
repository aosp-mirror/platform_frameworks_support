/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.testutils

import android.app.Activity
import android.os.Build
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.test.rule.ActivityTestRule
import org.junit.Assert
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun <T : Activity> ActivityTestRule<T>.waitForActivityDrawn() {
    val latch = java.util.concurrent.CountDownLatch(1)
    val view = activity.window.decorView
    val viewTreeObserver = activity.window.decorView.viewTreeObserver

    runOnUiThread {
        viewTreeObserver.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    view.post {
                        viewTreeObserver.removeOnDrawListener(this)
                        latch.countDown()
                    }
                }
            }
        )
        view.invalidate()
    }

    try {
        Assert.assertTrue(
            "Draw pass did not occur within 5 seconds",
            latch.await(5, TimeUnit.SECONDS)
        )
    } catch (e: InterruptedException) {
        throw RuntimeException(e)
    }
}
