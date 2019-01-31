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

package androidx.viewpager2.widget

import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_FAKE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING

fun scrollStateToString(@ViewPager2.ScrollState state: Int): String {
    return when (state) {
        SCROLL_STATE_IDLE -> "IDLE"
        SCROLL_STATE_DRAGGING -> "DRAGGING"
        SCROLL_STATE_SETTLING -> "SETTLING"
        SCROLL_STATE_FAKE_DRAGGING -> "FAKE_DRAG"
        else -> "UNKNOWN"
    }
}

fun scrollStateGlossary(): String {
    return "Scroll states: 0=${scrollStateToString(0)}, 1=${scrollStateToString(1)}, " +
            "2=${scrollStateToString(2)}, 3=${scrollStateToString(3)})"
}