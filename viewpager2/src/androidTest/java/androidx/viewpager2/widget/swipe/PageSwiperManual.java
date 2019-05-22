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

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager2.widget.ViewPager2;

public class PageSwiperManual implements PageSwiper {
    private final ViewPager2 mViewPager;
    private final SwipeAction mActionPrevious;
    private final SwipeAction mActionNext;

    public PageSwiperManual(ViewPager2 viewPager, boolean isRtl) {
        mViewPager = viewPager;
        int orientation = viewPager.getOrientation();
        mActionPrevious = orientation == ORIENTATION_HORIZONTAL
                ? (isRtl ? SWIPE_LEFT : SWIPE_RIGHT)
                : SWIPE_DOWN;
        mActionNext = orientation == ORIENTATION_HORIZONTAL
                ? (isRtl ? SWIPE_RIGHT : SWIPE_LEFT)
                : SWIPE_UP;
    }

    @Override
    public void swipeNext() {
        mActionNext.swipe(InstrumentationRegistry.getInstrumentation(), mViewPager);
    }

    @Override
    public void swipePrevious() {
        mActionPrevious.swipe(InstrumentationRegistry.getInstrumentation(), mViewPager);
    }

<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
=======
    public void swipeForward(float px, Interpolator interpolator) {
        swipe(px * mXForwardFactor, px * mYForwardFactor, interpolator);
    }

    public void swipeBackward(float px, Interpolator interpolator) {
        swipe(px * -mXForwardFactor, px * -mYForwardFactor, interpolator);
    }

    private void swipe(float xOffset, float yOffset, Interpolator interpolator) {
        new ManualSwipeInjector(
                offsetCenter(-xOffset / 2, -yOffset / 2),
                offsetCenter((xOffset + 1) / 2, (yOffset + 1) / 2),
                150, 20
        ).perform(InstrumentationRegistry.getInstrumentation(), mViewPager, interpolator);
    }

    private static CoordinatesProvider offsetCenter(final float dx, final float dy) {
        return new TranslatedCoordinatesProvider(CENTER, dx, dy);
    }

>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
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
