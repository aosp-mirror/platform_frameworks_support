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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Small integration tests that verify that {@link RecyclerView} interacts with a parent
 * {@link NestedScrollingParent3} correctly.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RecyclerViewNestedScrollingChildTest {

    private NestedScrollingSpyView mParentSpy;
    private RecyclerView mRecyclerView;

    private void setup(boolean vertical) {

        Context context = InstrumentationRegistry.getContext();

        // Create views

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setMinimumWidth(1000);
        mRecyclerView.setMinimumHeight(1000);

        mParentSpy = Mockito.spy(new NestedScrollingSpyView(context));
        mParentSpy.setMinimumWidth(1000);
        mParentSpy.setMinimumHeight(1000);

        // Setup RecyclerView
        int orienation = vertical ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, orienation, false));
        mRecyclerView.setAdapter(new TestAdapter(context, 1100, 100, vertical));

        // Create view hierarchy
        mParentSpy.addView(mRecyclerView);

        //  Measure and layout
        int measureSpecWidth = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        int measureSpecHeight = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        mParentSpy.measure(measureSpecWidth, measureSpecHeight);
        mParentSpy.layout(0, 0, 1000, 1000);
    }

    @Test
    public void uiFingerScroll_scrollsBeyondLimitVertical_remainderPassedToParent() {
        setup(true);
        int touchSlop =
                ViewConfiguration.get(InstrumentationRegistry.getContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300 - touchSlop, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 0, 100, 0, 100, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsBeyondLimitHorizontal_remainderPassedToParent() {
        setup(false);
        int touchSlop =
                ViewConfiguration.get(InstrumentationRegistry.getContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 300 - touchSlop, 500, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 100, 0, 100, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsWithinLimitVertical_remainderPassedToParent() {
        setup(true);
        int touchSlop =
                ViewConfiguration.get(InstrumentationRegistry.getContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 450 - touchSlop, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 0, 50, 0, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsWithinLimitHorizontal_remainderPassedToParent() {
        setup(false);
        int touchSlop =
                ViewConfiguration.get(InstrumentationRegistry.getContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 450 - touchSlop, 500, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 50, 0, 0, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerDown_parentHasNestedScrollingChildWithTypeTouch() {
        setup(true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);

        mRecyclerView.dispatchTouchEvent(down);

        assertThat(mParentSpy.isNestedScrollingParentForTypeTouch, is(true));
        assertThat(mParentSpy.isNestedScrollingParentForTypeNonTouch, is(false));
    }

    @Test
    public void uiFingerUp_parentDoesNotHaveNestedScrollingChild() {
        setup(true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent up = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 500, 500, 0);
        mRecyclerView.dispatchTouchEvent(down);

        mRecyclerView.dispatchTouchEvent(up);

        assertThat(mParentSpy.isNestedScrollingParentForTypeTouch, is(false));
        assertThat(mParentSpy.isNestedScrollingParentForTypeNonTouch, is(false));
    }

    @Test
    public void uiFling_parentHasNestedScrollingChildWithTypeFling() {
        setup(true);
        NestedScrollViewTestUtils
                .simulateFlingDown(InstrumentationRegistry.getContext(), mRecyclerView);

        assertThat(mParentSpy.isNestedScrollingParentForTypeTouch, is(false));
        assertThat(mParentSpy.isNestedScrollingParentForTypeNonTouch, is(true));
    }

    @Test
    public void uiFling_callsNestedFlingsCorrectly() {
        setup(true);
        NestedScrollViewTestUtils
                .simulateFlingDown(InstrumentationRegistry.getContext(), mRecyclerView);

        InOrder inOrder = Mockito.inOrder(mParentSpy);
        inOrder.verify(mParentSpy).onNestedPreFling(
                eq(mRecyclerView),
                eq(0f),
                anyFloat());
        inOrder.verify(mParentSpy).onNestedFling(
                eq(mRecyclerView),
                eq(0f),
                anyFloat(),
                eq(true));
    }

    /*
    @Test
    public void uiDown_duringFling_stopsNestedScrolling() {
        setup(true);
        final Context context = InstrumentationRegistry.getContext();
        final int[] targetFlingTimeAndDistance =
                NestedScrollViewTestUtils.getTargetFlingVelocityTimeAndDistance(context);
        final int targetTimePassed = targetFlingTimeAndDistance[1];
        final MotionEvent[] motionEvents =
                NestedScrollViewTestUtils.generateMotionEvents(targetFlingTimeAndDistance);
        NestedScrollViewTestUtils.dispatchMotionEventsToView(mRecyclerView, motionEvents);
        // Sanity check that onStopNestedScroll has not yet been called of type TYPE_NON_TOUCH.
        verify(mParentSpy, never())
                .onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);

        MotionEvent down = MotionEvent.obtain(
                0,
                targetTimePassed + 100, // Should be after fling events occurred.
                MotionEvent.ACTION_DOWN,
                500,
                500,
                0);
        mRecyclerView.dispatchTouchEvent(down);

        verify(mParentSpy).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);
    }
    */

    @Test
    public void uiFlings_parentReturnsTrueForOnNestedFling_dispatchNestedFlingCalled() {
        setup(true);
        when(mParentSpy.onNestedPreFling(eq(mRecyclerView), anyFloat(), anyFloat()))
                .thenReturn(false);

        NestedScrollViewTestUtils
                .simulateFlingDown(InstrumentationRegistry.getContext(), mRecyclerView);

        verify(mParentSpy).onNestedFling(eq(mRecyclerView), anyFloat(), anyFloat(), eq(true));
    }

    @Test
    public void uiFlings_parentReturnsFalseForOnNestedFling_dispatchNestedFlingNotCalled() {
        setup(true);
        when(mParentSpy.onNestedPreFling(eq(mRecyclerView), anyFloat(), anyFloat()))
                .thenReturn(true);

        NestedScrollViewTestUtils
                .simulateFlingDown(InstrumentationRegistry.getContext(), mRecyclerView);

        verify(mParentSpy, never())
                .onNestedFling(any(View.class), anyFloat(), anyFloat(), anyBoolean());
    }

    @Test
    public void smoothScrollBy_doesNotStartNestedScrolling() {
        setup(true);
        mRecyclerView.smoothScrollBy(0, 100);
        verify(mParentSpy, never()).onStartNestedScroll(
                any(View.class), any(View.class), anyInt(), anyInt());
    }

    /*@Test
    public void smoothScrollBy_stopsInProgressNestedScroll() {
        mRecyclerView.fling(0, 100);
        mRecyclerView.smoothScrollBy(0, 100);
        verify(mParentSpy).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);
    }*/

    /*@Test
    public void fling_startsNestedScrolling() {
        mRecyclerView.fling(0, 100);
        verify(mParentSpy).onStartNestedScroll(
                eq(mRecyclerView), eq(mRecyclerView), anyInt(), anyInt());
    }*/

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public boolean isNestedScrollingParentForTypeTouch;
        public boolean isNestedScrollingParentForTypeNonTouch;

        public NestedScrollingSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                isNestedScrollingParentForTypeNonTouch = true;
            } else {
                isNestedScrollingParentForTypeTouch = true;
            }
            return true;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                isNestedScrollingParentForTypeNonTouch = false;
            } else {
                isNestedScrollingParentForTypeTouch = false;
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
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @NonNull int[] consumed) {
        }
    }

    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private Context mContext;
        private int mOrientationSize;
        private int mItemCount;
        private boolean mVertical;

        TestAdapter(Context context, int orientationSize, int itemCount, boolean vertical) {
            mContext = context;
            mOrientationSize = orientationSize / itemCount;
            mItemCount = itemCount;
            mVertical = vertical;
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            View view = new View(mContext);

            int width;
            int height;
            if (mVertical) {
                width = ViewGroup.LayoutParams.MATCH_PARENT;
                height = mOrientationSize;
            } else {
                width = mOrientationSize;
                height = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
            view.setMinimumHeight(mOrientationSize);
            return new TestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

    private class TestViewHolder extends RecyclerView.ViewHolder {

        TestViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class NestedScrollViewTestUtils {

        static int[] getTargetFlingVelocityTimeAndDistance(Context context) {
            ViewConfiguration configuration =
                    ViewConfiguration.get(context);
            int touchSlop = configuration.getScaledTouchSlop();
            int mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
            int mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

            int targetVelocitySeconds = ((mMaximumVelocity - mMinimumVelocity) / 2)
                    + mMinimumVelocity;
            int targetDistanceTraveled = touchSlop * 2;
            int targetTimePassed = (targetDistanceTraveled * 1000) / targetVelocitySeconds;

            return new int[]{targetVelocitySeconds, targetTimePassed, targetDistanceTraveled};
        }

        static MotionEvent[] generateMotionEvents(int[] targetFlingVelocityTimeAndDistance) {
            int targetTimePassed = targetFlingVelocityTimeAndDistance[1];
            int targetDistanceTraveled = targetFlingVelocityTimeAndDistance[2];
            targetDistanceTraveled *= -1;

            MotionEvent down = MotionEvent.obtain(
                    0,
                    0,
                    MotionEvent.ACTION_DOWN,
                    500,
                    500,
                    0);
            MotionEvent move = MotionEvent.obtain(
                    0,
                    targetTimePassed,
                    MotionEvent.ACTION_MOVE,
                    500,
                    500 + targetDistanceTraveled,
                    0);
            MotionEvent up = MotionEvent.obtain(
                    0,
                    targetTimePassed,
                    MotionEvent.ACTION_UP,
                    500,
                    500 + targetDistanceTraveled,
                    0);

            return new MotionEvent[]{down, move, up};
        }

        static void dispatchMotionEventsToView(View view, MotionEvent[] motionEvents) {
            for (MotionEvent motionEvent : motionEvents) {
                view.dispatchTouchEvent(motionEvent);
            }
        }

        static void simulateFlingDown(Context context, View view) {
            int[] targetFlingTimeAndDistance =
                    NestedScrollViewTestUtils.getTargetFlingVelocityTimeAndDistance(context);
            MotionEvent[] motionEvents =
                    NestedScrollViewTestUtils.generateMotionEvents(targetFlingTimeAndDistance);
            NestedScrollViewTestUtils.dispatchMotionEventsToView(view, motionEvents);
        }
    }
}
