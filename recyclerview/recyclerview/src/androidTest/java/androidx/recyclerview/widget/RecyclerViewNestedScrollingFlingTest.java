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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewNestedScrollingFlingTest {

    private NestedScrollingParent mNestedScrollingParent;
    private RecyclerView mRecyclerView;

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);

    private void setup(boolean vertical, int scrollDistance)
            throws Throwable {

        Context context = ApplicationProvider.getApplicationContext();

        // Create views

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setMinimumWidth(1000);
        mRecyclerView.setMinimumHeight(1000);

        mNestedScrollingParent = new NestedScrollingParent(context);
        mNestedScrollingParent.setMinimumWidth(1000);
        mNestedScrollingParent.setMinimumHeight(1000);

        // Setup RecyclerView
        int orientation = vertical ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, orientation, false));
        mRecyclerView.setAdapter(new TestAdapter(context, 1000 + scrollDistance, 1, vertical));

        // Create view hierarchy
        mNestedScrollingParent.addView(mRecyclerView);

        final TestedFrameLayout testedFrameLayout = mActivityRule.getActivity().getContainer();
        testedFrameLayout.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testedFrameLayout.addView(mNestedScrollingParent);
            }
        });
        testedFrameLayout.waitForLayout(2);
    }

    @Test
    public void uiFingerFling_up_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_down_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        uiFingerFling_flingVelocityIsCorrect(true, 200, 20, -10000);
    }

    @Test
    public void uiFingerFling_left_flingVelocityIsCorrect() throws Throwable {
        setup(false, 100);
        uiFingerFling_flingVelocityIsCorrect(false, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_right_flingVelocityIsCorrect() throws Throwable {
        setup(false, 100);
        uiFingerFling_flingVelocityIsCorrect(false, 200, 20, -10000);
    }

    @Test
    public void uiFingerFling_parentPreQuarter_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.preScrollY = 25;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPreHalf_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.preScrollY = 50;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPreAll_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.preScrollY = 100;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPostQuarter_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.postScrollY = 25;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPostHalf_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.postScrollY = 50;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPostAll_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.postScrollY = 100;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPrePostQuarter_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.preScrollY = 12;
        mNestedScrollingParent.postScrollY = 13;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPrePostHalf_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.preScrollY = 25;
        mNestedScrollingParent.postScrollY = 25;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    @Test
    public void uiFingerFling_parentPrePostAll_flingVelocityIsCorrect() throws Throwable {
        setup(true, 100);
        mNestedScrollingParent.preScrollY = 50;
        mNestedScrollingParent.postScrollY = 50;
        uiFingerFling_flingVelocityIsCorrect(true, -200, 20, 10000);
    }

    private void uiFingerFling_flingVelocityIsCorrect(boolean vertical, int directionalDistance,
            int elapsedTime, int expectedVelocity) {
        final int[] velocities = new int[]{1, 1};
        mRecyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                velocities[0] = velocityX;
                velocities[1] = velocityY;
                return false;
            }
        });

        int xChange1 = vertical ? 0 : directionalDistance / 2;
        int yChange1 = vertical ? directionalDistance / 2 : 0;
        int xChange2 = vertical ? 0 : directionalDistance;
        int yChange2 = vertical ? directionalDistance : 0;
        int time1 = elapsedTime / 2;
        int time2 = elapsedTime;

        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move1 = MotionEvent.obtain(0, time1, MotionEvent.ACTION_MOVE, 500 + xChange1,
                500 + yChange1, 0);
        MotionEvent move2 = MotionEvent.obtain(0, time2, MotionEvent.ACTION_MOVE, 500 + xChange2,
                500 + yChange2, 0);
        MotionEvent up = MotionEvent.obtain(0, time2, MotionEvent.ACTION_UP, 500 + xChange2,
                500 + yChange2, 0);

        mNestedScrollingParent.dispatchTouchEvent(down);
        mNestedScrollingParent.dispatchTouchEvent(move1);
        mNestedScrollingParent.dispatchTouchEvent(move2);
        mNestedScrollingParent.dispatchTouchEvent(up);

        int expectedVelocityX = vertical ? 0 : expectedVelocity;
        int expectedVelocityY = vertical ? expectedVelocity : 0;

        assertThat(velocities, is(new int[]{expectedVelocityX, expectedVelocityY}));
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
        public boolean onStartNestedScroll(View child, View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(View target) {

        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        }

        @Override
        public boolean onNestedFling(View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
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

    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private Context mContext;
        private int mOrientationSize;
        private int mItemCount;
        private boolean mVertical;

        TestAdapter(Context context, float orientationSize, int itemCount, boolean vertical) {
            mContext = context;
            mOrientationSize = (int) Math.floor(orientationSize / itemCount);
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
}
