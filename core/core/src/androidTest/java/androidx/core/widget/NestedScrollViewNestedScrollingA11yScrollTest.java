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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.test.R;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Large integration test that verifies that NestedScrollView participates in nested scrolling when
 * scrolling occurs due to a11y actions.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewNestedScrollingA11yScrollTest extends
        BaseInstrumentationTestCase<TestContentViewActivity> {

    private static final int CHILD_HEIGHT = 300;
    private static final int NSV_HEIGHT = 100;
    private static final int PARENT_HEIGHT = 300;
    private static final int WIDTH = 400;
    // A11Y scroll only scrolls the height of the NestedScrollView at max.
    private static final int TOTAL_SCROLL_OFFSET = 100;

    private NestedScrollView mNestedScrollView;
    private NestedScrollingSpyView mParent;

    public NestedScrollViewNestedScrollingA11yScrollTest() {
        super(TestContentViewActivity.class);
    }

    @Before
    public void setup() throws Throwable {
        Context context = mActivityTestRule.getActivity();

        View child = new View(context);
        child.setMinimumWidth(WIDTH);
        child.setMinimumHeight(CHILD_HEIGHT);
        child.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, CHILD_HEIGHT));
        child.setBackgroundDrawable(
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFF0000, 0xFF00FF00}));

        mNestedScrollView = new NestedScrollView(context);
        mNestedScrollView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, NSV_HEIGHT));
        mNestedScrollView.setBackgroundColor(0xFF0000FF);
        mNestedScrollView.addView(child);

        mParent = spy(new NestedScrollingSpyView(context));
        mParent.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, PARENT_HEIGHT));
        mParent.setBackgroundColor(0xFF0000FF);
        mParent.addView(mNestedScrollView);

        // Attach to activity and wait for layouts.
        final TestContentView testContentView =
                mActivityTestRule.getActivity().findViewById(R.id.testContentView);
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(() -> testContentView.addView(mParent));
        testContentView.awaitLayouts(2);
    }

    @Test
    public void a11yActionScrollForward_fullyParticipatesInNestedScrolling() throws Throwable {
        a11yScroll_fullyParticipatesInNestedScrolling(true);
    }

    @Test
    public void a11yActionScrollBackward_fullyParticipatesInNestedScrolling() throws Throwable {
        a11yScroll_fullyParticipatesInNestedScrolling(false);
    }

    private void a11yScroll_fullyParticipatesInNestedScrolling(final boolean forward)
            throws Throwable {

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        mActivityTestRule.runOnUiThread(() -> {
            doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                    anyInt(), anyInt());

            int action;
            if (forward) {
                action = AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
            } else {
                mNestedScrollView.scrollTo(0, 200);
                action = AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;
            }

            mParent.mOnStopNestedScrollListener =
                    type -> {
                        if (type == ViewCompat.TYPE_NON_TOUCH) {
                            countDownLatch.countDown();
                        }
                    };

            mNestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX,
                            oldScrollY) -> {
                        if (scrollY == TOTAL_SCROLL_OFFSET) {
                            countDownLatch.countDown();
                        }
                    });

            ViewCompat.performAccessibilityAction(mNestedScrollView, action, null);
        });
        assertThat(countDownLatch.await(2, TimeUnit.SECONDS), is(true));

        // Verify that none of the following TYPE_TOUCH nested scrolling methods are called.
        verify(mParent, never()).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        verify(mParent, never()).onNestedScrollAccepted(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        verify(mParent, never()).onNestedPreScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                anyInt(), anyInt(), eq(ViewCompat.TYPE_TOUCH), any(int[].class));
        verify(mParent, never()).onNestedPreFling(eq(mNestedScrollView), anyFloat(),
                anyFloat());
        verify(mParent, never()).onNestedFling(eq(mNestedScrollView), anyFloat(), anyFloat(),
                eq(true));
        verify(mParent, never()).onStopNestedScroll(mNestedScrollView, ViewCompat.TYPE_TOUCH);

        // Verify all of the following TYPE_NON_TOUCH nested scrolling methods are called
        verify(mParent, atLeastOnce()).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                anyInt(),
                anyInt(), eq(ViewCompat.TYPE_NON_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mNestedScrollView,
                ViewCompat.TYPE_NON_TOUCH);
    }

    public static class NestedScrollingSpyView extends FrameLayout implements
            NestedScrollingParent3 {

        public OnStopNestedScrollListener mOnStopNestedScrollListener;

        public NestedScrollingSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {
            if (mOnStopNestedScrollListener != null) {
                mOnStopNestedScrollListener.onStopNestedScroll(type);
            }
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @Nullable int[] consumed) {
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

        interface OnStopNestedScrollListener {
            void onStopNestedScroll(int type);
        }
    }
}
