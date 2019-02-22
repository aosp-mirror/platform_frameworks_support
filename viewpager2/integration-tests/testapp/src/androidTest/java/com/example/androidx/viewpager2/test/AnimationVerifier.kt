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

package com.example.androidx.viewpager2.test

import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.viewpager2.widget.ViewPager2
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assume
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class AnimationVerifier(private val viewPager: ViewPager2) : ViewPager2.OnPageChangeCallback() {
    private val epsilon = 0.00001f
    private var foundAnimatedFrame = false

    val latch = CountDownLatch(1)
    var hasRotation = false
        private set
    var hasTranslation = false
        private set
    var hasScale = false
        private set

    init {
        viewPager.registerOnPageChangeCallback(this)
    }

    fun verify(
        expectRotation: Boolean,
        expectTranslation: Boolean,
        expectScale: Boolean
    ) {
        Assume.assumeThat(
            "Couldn't get hold of an animated frame, so couldn't verify if animation worked",
            latch.await(2, TimeUnit.SECONDS),
            equalTo(true)
        )
        assertThat(hasRotation, equalTo(expectRotation))
        assertThat(hasTranslation, equalTo(expectTranslation))
        assertThat(hasScale, equalTo(expectScale))
    }

    override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
        if (!foundAnimatedFrame && offsetPx != 0) {
            foundAnimatedFrame = true
            // Page transformations are done after OnPageChangeCallbacks are called, so postpone
            // the actual verification
            viewPager.post {
                recordAnimationProperties(position)
            }
        }
    }

    private fun recordAnimationProperties(position: Int) {
        // Get hold of the page at the specified position
        val recyclerView = viewPager.getChildAt(0) as RecyclerView
        val lm = recyclerView.layoutManager as LinearLayoutManager
        val page = lm.findViewByPosition(position)

        // Get the animation values to verify
        hasRotation = !isZero(page!!.rotation) || !isZero(page.rotationX) ||
                !isZero(page.rotationY)
        hasTranslation = !isZero(page.translationX) || !isZero(page.translationY) ||
                !isZero(ViewCompat.getTranslationZ(page))
        hasScale = !isOne(page.scaleX) || !isOne(page.scaleY)

        // Mark verification as done
        latch.countDown()
        viewPager.unregisterOnPageChangeCallback(this)
    }

    private fun isZero(f: Float): Boolean {
        return abs(f) < epsilon
    }

    private fun isOne(f: Float): Boolean {
        return isZero(f - 1)
    }
}