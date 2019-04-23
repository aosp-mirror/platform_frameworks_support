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

package androidx.viewpager2.widget.swipe

import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import kotlin.math.max
import kotlin.math.roundToInt

class PageSwiperFakeDrag(private val viewPager: ViewPager2) : PageSwiper {
    companion object {
        // 60 fps
        private const val FRAME_LENGTH_MS = 1000L / 60
        private const val FLING_DURATION_MS = 100L
    }

    private val needsRtlModifier
        get() = viewPager.orientation == ORIENTATION_HORIZONTAL &&
                ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL

    override fun swipeNext() {
        fakeDrag(.5f, interpolator = AccelerateInterpolator())
    }

    override fun swipePrevious() {
        fakeDrag(-.5f, interpolator = AccelerateInterpolator())
    }

    fun fakeDrag(
        relativeDragDistance: Float,
        duration: Long = FLING_DURATION_MS,
        interpolator: Interpolator = LinearInterpolator()
    ) {
        // Calculate the actual distance that fakeDragBy() will cover
        val rtlModifier = if (needsRtlModifier) -1 else 1
        val steps = max(1, (duration / FRAME_LENGTH_MS.toFloat()).roundToInt())
        val distancePx = viewPager.pageSize * -relativeDragDistance * rtlModifier

        // Send the fakeDrag events
        // Needs to be done from the UI thread, but the time when our scheduled
        // callbacks are run is unpredictable. Therefore, dynamically decide the
        // drag delta and whether to send an event at all.
        val fakeDragDispatcher = FakeDragDispatcher(viewPager, distancePx, steps, interpolator)
        fakeDragDispatcher.postCallbacks()
    }

    private class FakeDragDispatcher(
        private val viewPager: ViewPager2,
        private val distancePx: Float,
        private val steps: Int,
        private val interpolator: Interpolator
    ) {
        private val dragTime = steps * FRAME_LENGTH_MS
        private var startTime = 0L
        private var endTime = 0L
        private var lastOffset = 0f

        fun postCallbacks() {
            var postTimeMs = SystemClock.uptimeMillis()
            val postDelayMs = { postTimeMs - SystemClock.uptimeMillis() }
            viewPager.post { beginFakeDrag() }
            repeat(steps) {
                postTimeMs += FRAME_LENGTH_MS
                viewPager.postDelayed({ fakeDragStep() }, postDelayMs())
            }
            postTimeMs++
            viewPager.postDelayed({ endFakeDrag() }, postDelayMs())
        }

        fun beginFakeDrag() {
            startTime = SystemClock.uptimeMillis()
            endTime = startTime + dragTime
            viewPager.beginFakeDrag()
        }

        fun fakeDragStep() {
            val x = (SystemClock.uptimeMillis() - startTime).toFloat() / dragTime
            val currOffset = interpolator.getInterpolation(x) * distancePx
            viewPager.fakeDragBy(currOffset - lastOffset)
            lastOffset = currOffset
        }

        fun endFakeDrag() {
            viewPager.endFakeDrag()
        }
    }
}