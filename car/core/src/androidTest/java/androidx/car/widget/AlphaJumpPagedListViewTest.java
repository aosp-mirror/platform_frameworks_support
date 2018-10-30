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

package androidx.car.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.Matchers.not;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/** Unit tests for {@link PagedListView}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class AlphaJumpPagedListViewTest {

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);
    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;


    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(androidx.car.test.R.id.paged_list_view);
    }



    /** Sets up {@link #mPagedListView} with the alpha jump adapter. */
    private void setUpPagedListViewWithAlphaJump() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                mPagedListView.setAdapter(new AlphaJumpTestAdapter());
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    @Test
    public void testScrollBarViewNoShowButtonWhenLimitedSpace() {
        setUpPagedListViewWithAlphaJump();
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(isDisplayed()));
        setLayoutRestrictions(2, 1, 1,
                0, 0, 1, -1);
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(not(isDisplayed())));
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(not(isDisplayed())));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollBarViewNoShowThumbWhenLimitedSpace() {
        setUpPagedListViewWithAlphaJump();
        Resources res = mPagedListView.getContext().getResources();
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(isDisplayed()));
        setLayoutRestrictions(2, 1, 1,
                1, 0, 2, -1);
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(not(isDisplayed())));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollBarViewNoShowAlphaWhenLimitedSpace() {
        setUpPagedListViewWithAlphaJump();
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(isDisplayed()));
        setLayoutRestrictions(2, 1, 1,
                 1, 1, 3, -1);
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollBarViewShowAllWhenLimitedSpace() {
        setUpPagedListViewWithAlphaJump();
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(isDisplayed()));
        setLayoutRestrictions(2, 1, 1,
                1, 1, 3, 0);
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(isDisplayed()));
    }

    private void setLayoutRestrictions(int numButtonHeight, int numPaddingTop, int numPaddingBottom,
            int numScrollThumb, int numAlphaJump, int numSeperateMargin, int bias) {
        Resources res = mPagedListView.getContext().getResources();
        ImageView upButton = mPagedListView.findViewById(androidx.car.test.R.id.page_up);
        Button alphaJump = mPagedListView.findViewById(androidx.car.test.R.id.alpha_jump);
        View scrollThumb = mPagedListView.findViewById(androidx.car.test.R.id.scrollbar_thumb);
        View scrollBarView = mPagedListView.findViewById(androidx.car.test.R.id.paged_scroll_view);
        ViewGroup.LayoutParams params = scrollBarView.getLayoutParams();
        params.height = numButtonHeight * upButton.getHeight()
                + numPaddingBottom * mPagedListView.getPaddingBottom()
                + numPaddingTop * mPagedListView.getPaddingTop()
                + numScrollThumb * scrollThumb.getHeight()
                + numAlphaJump * alphaJump.getHeight()
                + numSeperateMargin * res.getDimensionPixelSize(
                androidx.car.R.dimen.car_padding_4) + bias;
        try {
            mActivityRule.runOnUiThread(() -> {
                scrollBarView.setLayoutParams(params);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }


    public static class AlphaJumpTestAdapter
            extends RecyclerView.Adapter<AlphaJumpTestAdapter.ViewHolder>
            implements AlphaJumpAdapter {
        private String[] mStrings;
        private boolean mIsSorted;

        public final String[] DUMMY = new String[100];
        AlphaJumpTestAdapter() {
            for (int i = 0; i < 100; i++) {
                DUMMY[i] = Character.toString((char) (65 + i / 4));
            }
            // Start out not being sorted.
            mStrings = DUMMY;
            mIsSorted = false;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(androidx.car.test.R.layout.alpha_jump_list_item, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mStrings[position]);
        }

        @Override
        public int getItemCount() {
            return mStrings.length;
        }

        @Override
        public List<AlphaJumpBucket> getAlphaJumpBuckets() {
            if (!mIsSorted) {
                // We'll sort the first time we need to populate the buckets.
                mStrings = mStrings.clone();
                Arrays.sort(mStrings);
                mIsSorted = true;
                notifyDataSetChanged();
            }

            AlphaJumpBucketer bucketer = new AlphaJumpBucketer();
            return bucketer.createBuckets(mStrings);
        }

        @Override
        public void onAlphaJumpEnter() {
        }

        @Override
        public void onAlphaJumpLeave(AlphaJumpBucket bucket) {
        }

        /**
         * ViewHolder for CheeseAdapter.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(androidx.car.test.R.id.text);
            }
        }
    }
}

