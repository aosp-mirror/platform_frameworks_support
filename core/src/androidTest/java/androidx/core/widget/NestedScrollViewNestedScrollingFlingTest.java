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

package androidx.core.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewNestedScrollingFlingTest {

    private NestedScrollingParent mNestedScrollingParent;
    private NestedScrollViewUnderTest mNestedScrollView;

    @Rule
    public ActivityTestRule<TestContentViewActivity> mActivityRule =
            new ActivityTestRule<>(TestContentViewActivity.class);

    @SuppressWarnings("SameParameterValue")
    private void setup(int scrollDistance)
            throws Throwable {

        Context context = ApplicationProvider.getApplicationContext();

        // Create views

        View child = new View(context);
        child.setMinimumHeight(1000 + scrollDistance);
        child.setMinimumWidth(1000);

        mNestedScrollView = new NestedScrollViewUnderTest(context);
        mNestedScrollView.setMinimumWidth(1000);
        mNestedScrollView.setMinimumHeight(1000);

        mNestedScrollingParent = new NestedScrollingParent(context);
        mNestedScrollingParent.setMinimumWidth(1000);
        mNestedScrollingParent.setMinimumHeight(1000);

        // Create view hierarchy
        mNestedScrollView.addView(child);
        mNestedScrollingParent.addView(mNestedScrollView);

        final TestContentView testContentView =
                mActivityRule.getActivity().findViewById(androidx.core.test.R.id.testContentView);
        testContentView.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mNestedScrollingParent);
            }
        });
        testContentView.awaitLayouts(2);
    }

    @Test
    public void uiFingerFling_up_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_down_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        uiFingerFling_flingVelocityIsCorrect(200, 20, -10000);
    }

    @Test
    public void uiFingerFling_left_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_right_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        uiFingerFling_flingVelocityIsCorrect(200, 20, -10000);
    }

    @Test
    public void uiFingerFling_downParentPreQuarter_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.preScrollY = 25;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPreHalf_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.preScrollY = 50;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPreAll_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.preScrollY = 100;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPostQuarter_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.postScrollY = 25;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPostHalf_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.postScrollY = 50;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPostAll_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.postScrollY = 100;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPrePostQuarter_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.preScrollY = 12;
        mNestedScrollingParent.postScrollY = 13;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPrePostHalf_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.preScrollY = 25;
        mNestedScrollingParent.postScrollY = 25;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @Test
    public void uiFingerFling_downParentPrePostAll_flingVelocityIsCorrect() throws Throwable {
        setup(100);
        mNestedScrollingParent.preScrollY = 50;
        mNestedScrollingParent.postScrollY = 50;
        uiFingerFling_flingVelocityIsCorrect(-200, 20, 10000);
    }

    @SuppressWarnings("SameParameterValue")
    private void uiFingerFling_flingVelocityIsCorrect(int directionalDistance, int elapsedTime,
            int expectedVelocity) {

        int halfDirectionalDistance = directionalDistance / 2;
        int halfTime = elapsedTime / 2;

        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move1 = MotionEvent.obtain(0, halfTime, MotionEvent.ACTION_MOVE, 500,
                500 + halfDirectionalDistance, 0);
        MotionEvent move2 = MotionEvent.obtain(0, elapsedTime, MotionEvent.ACTION_MOVE, 500,
                500 + directionalDistance, 0);
        MotionEvent up = MotionEvent.obtain(0, elapsedTime, MotionEvent.ACTION_UP, 500,
                500 + directionalDistance, 0);

        mNestedScrollingParent.dispatchTouchEvent(down);
        mNestedScrollingParent.dispatchTouchEvent(move1);
        mNestedScrollingParent.dispatchTouchEvent(move2);
        mNestedScrollingParent.dispatchTouchEvent(up);

        assertThat(mNestedScrollView.flungVelocity, is(expectedVelocity));
    }

    public class NestedScrollViewUnderTest extends NestedScrollView {

        public int flungVelocity = 0;

        public NestedScrollViewUnderTest(@NonNull Context context) {
            super(context);
        }

        public NestedScrollViewUnderTest(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public NestedScrollViewUnderTest(@NonNull Context context, @Nullable AttributeSet attrs,
                int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void fling(int velocityY) {
            flungVelocity = velocityY;
        }
    }

    public class NestedScrollingParent extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public int preScrollX;
        public int preScrollY;
        public int postScrollX;
        public int postScrollY;

        public NestedScrollingParent(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return true;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {
            int toScrollX = amountOfScrollToConsume(dx, preScrollX);
            preScrollX -= toScrollX;
            consumed[0] += toScrollX;

            int toScrollY = amountOfScrollToConsume(dy, preScrollY);
            preScrollY -= toScrollY;
            consumed[1] += toScrollY;

            scrollBy(toScrollX, toScrollY);
        }

        @Override
        public boolean startNestedScroll(int axes, int type) {
            return false;
        }

        @Override
        public void stopNestedScroll(int type) {

        }

        @Override
        public boolean hasNestedScrollingParent(int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {

            int toScrollX = amountOfScrollToConsume(dxUnconsumed, postScrollX);
            postScrollX -= toScrollX;
            consumed[0] += toScrollX;

            int toScrollY = amountOfScrollToConsume(dyUnconsumed, postScrollY);
            postScrollY -= toScrollY;
            consumed[1] += toScrollY;

            scrollBy(toScrollX, toScrollY);
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @NonNull int[] consumed) {
        }

        @Override
        public void setNestedScrollingEnabled(boolean enabled) {

        }

        @Override
        public boolean isNestedScrollingEnabled() {
            return false;
        }

        @Override
        public boolean startNestedScroll(int axes) {
            return false;
        }

        @Override
        public void stopNestedScroll() {

        }

        @Override
        public boolean hasNestedScrollingParent() {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed,
                int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onStartNestedScroll(@NotNull View child, @NotNull View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NotNull View child, @NotNull View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(@NotNull View target) {

        }

        @Override
        public void onNestedScroll(@NotNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed,
                int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(@NotNull View target, int dx, int dy,
                @NotNull int[] consumed) {

        }

        @Override
        public boolean onNestedFling(@NotNull View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(@NotNull View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }

        private int amountOfScrollToConsume(int d, int max) {
            if (d < 0 && max < 0) {
                return Math.max(d, max);
            } else if (d > 0 && max > 0) {
                return Math.min(d, max);
            }
            return 0;
        }
    }
}
