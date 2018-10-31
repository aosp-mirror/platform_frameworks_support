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
import static org.junit.Assert.assertThat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// TODO: This probably isn't a small test
@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewSmoothScrollToPositionTest {

    private static final int RV_WIDTH = 500;
    private static final int RV_HEIGHT = 500;
    private static final int ITEM_WIDTH = 500;
    private static final int ITEM_HEIGHT = 200;
    private static final int NUM_ITEMS = 100;

    RecyclerView mRecyclerView;

    @Rule
    public final ActivityTestRule<TestContentViewActivity> mActivityTestRule;

    public RecyclerViewSmoothScrollToPositionTest() {
        mActivityTestRule = new ActivityTestRule<>(TestContentViewActivity.class);
    }

    @Before
    public void setUp() throws Throwable {
        Context context = mActivityTestRule.getActivity();

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(RV_WIDTH, RV_HEIGHT));
        mRecyclerView.setBackgroundColor(0xAAAAAAFF);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(new MyAdapter());

        final TestContentView testContentView = mActivityTestRule.getActivity().getContentView();
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mRecyclerView);
            }
        });
        testContentView.awaitLayouts(2);
    }

    @Test
    public void smoothScrollToPosition_targetOffScreen_getsToPosition()
            throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    countDownWhenViewIsAtLocation(mRecyclerView, "2", 300, true, latch);
                }
            }
        });

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(2);
            }
        });

        assertThat(latch.await(2, TimeUnit.SECONDS), is(true));
    }

    /**
     * smoothScrollToPosition(int, int)  - This is the only thing that creates a smoothScroller
     * _targetOffScreen
     * __getsToPosition
     * __callsCallbacksCorrectly
     * _targetPartiallyOnScreen
     * __getsToPosition
     * __callsCallbacksCorrectly
     * _targetIsOnScreen
     * __doesNotMove
     * __callsNoCallbacks
     * _calledTwice
     * __getsToPosition
     * __callsCallbacksCorrectly
     *
     * _calledDuringSmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringSmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringUiFling
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstSmoothScrollerOnAnimation
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleSmoothScrollerOnAnimation
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastSmoothScrollerOnAnimation
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstScrollListenerOnScrolled_startedBySmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleScrollListenerOnScrolled_startedBySmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastScrollListenerOnScrolled_startedBySmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstScrollListenerOnScrolled_startedBySmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleScrollListenerOnScrolled_startedBySmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastScrollListenerOnScrolled_startedBySmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstScrollListenerOnScrolled_startedByFling
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleScrollListenerOnScrolled_startedByFling
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastScrollListenerOnScrolled_startedByFling
     * __changesDirections
     * __completesCallbacksCorrectly
     */

    private void measure() {
        mRecyclerView.measure(View.MeasureSpec.EXACTLY | RV_WIDTH,
                View.MeasureSpec.EXACTLY | RV_HEIGHT);
    }

    private void layout() {
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
    }


    private void countDownWhenViewIsAtLocation(ViewGroup viewGroup, CharSequence text, int position,
            boolean matchTop, CountDownLatch latch) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof TextView) {
                if (text.equals(((TextView) view).getText())) {
                    if ((matchTop && view.getTop() == position)
                            || (!matchTop && view.getBottom() == position)) {
                        latch.countDown();
                    }
                }
            }
        }
    }

    private static class MyAdapter extends RecyclerView.Adapter {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textview = new TextView(parent.getContext());
            textview.setMinWidth(ITEM_WIDTH);
            textview.setMinHeight(ITEM_HEIGHT);
            return new RecyclerView.ViewHolder(textview) {

            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(Integer.toString(position));
        }

        @Override
        public int getItemCount() {
            return NUM_ITEMS;
        }
    }
}
