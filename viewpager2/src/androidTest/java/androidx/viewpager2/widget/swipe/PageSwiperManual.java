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

package androidx.viewpager2.widget.swipe;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;

import android.app.Instrumentation;
import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

public class PageSwiperManual {

    private final SwipeAction mActionPrevious;
    private final SwipeAction mActionNext;

    public PageSwiperManual(@ViewPager2.Orientation int orientation, boolean isRtl) {
        mActionPrevious = orientation == ORIENTATION_HORIZONTAL
                ? (isRtl ? SWIPE_LEFT : SWIPE_RIGHT)
                : SWIPE_DOWN;
        mActionNext = orientation == ORIENTATION_HORIZONTAL
                ? (isRtl ? SWIPE_RIGHT : SWIPE_LEFT)
                : SWIPE_UP;
    }

    public void swipeNext(Instrumentation instrumentation, View view) {
        mActionNext.swipe(instrumentation, view);
    }

    public void swipePrevious(Instrumentation instrumentation, View view) {
        mActionPrevious.swipe(instrumentation, view);
    }

    private interface SwipeAction {
        void swipe(Instrumentation instrumentation, View view);
    }

    private static final SwipeAction SWIPE_LEFT = new SwipeAction() {
        @Override
        public void swipe(Instrumentation instrumentation, View view) {
            ManualSwipeInjector.swipeLeft().perform(instrumentation, view);
        }
    };

    private static final SwipeAction SWIPE_RIGHT = new SwipeAction() {
        @Override
        public void swipe(Instrumentation instrumentation, View view) {
            ManualSwipeInjector.swipeRight().perform(instrumentation, view);
        }
    };

    private static final SwipeAction SWIPE_UP = new SwipeAction() {
        @Override
        public void swipe(Instrumentation instrumentation, View view) {
            ManualSwipeInjector.swipeUp().perform(instrumentation, view);
        }
    };

    private static final SwipeAction SWIPE_DOWN = new SwipeAction() {
        @Override
        public void swipe(Instrumentation instrumentation, View view) {
            ManualSwipeInjector.swipeDown().perform(instrumentation, view);
        }
    };

}
