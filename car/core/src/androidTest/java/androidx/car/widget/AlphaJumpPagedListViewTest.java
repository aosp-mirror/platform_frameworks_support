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

/** Unit tests for {@link PagedListView} with alpha jump button. */
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
        setLayoutRestrictions(false, false,
                1, true);
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
        setLayoutRestrictions(true, false,
                2, true);
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
        setLayoutRestrictions(true, true,
                3, true);
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
        setLayoutRestrictions(true, true,
                3, false);
        onView(withId(androidx.car.test.R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(androidx.car.test.R.id.alpha_jump)).check(matches(isDisplayed()));
    }

    /**
     *  Apply layout height restrictions to scroll bar view
     *
     * @param allowScrollThumb whether add thumb height or not
     * @param allowAlphaJump whether add alpha jump button height or not
     * @param numSeperateMargin number of seperate margin added
     * @param bias whether add bia or not
     */
    private void setLayoutRestrictions(boolean allowScrollThumb, boolean allowAlphaJump,
            int numSeperateMargin, boolean bias) {
        Resources res = mPagedListView.getContext().getResources();
        ImageView upButton = mPagedListView.findViewById(androidx.car.test.R.id.page_up);
        Button alphaJump = mPagedListView.findViewById(androidx.car.test.R.id.alpha_jump);
        View scrollThumb = mPagedListView.findViewById(androidx.car.test.R.id.scrollbar_thumb);
        View scrollBarView = mPagedListView.findViewById(androidx.car.test.R.id.paged_scroll_view);
        ViewGroup.LayoutParams params = scrollBarView.getLayoutParams();
        int restrictedHeight = 2 * upButton.getHeight()
                + mPagedListView.getPaddingBottom()
                + mPagedListView.getPaddingTop()
                + numSeperateMargin * res.getDimensionPixelSize(
                androidx.car.R.dimen.car_padding_4);
        if (allowScrollThumb) {
            restrictedHeight += scrollThumb.getHeight();
        }
        if (allowAlphaJump) {
            restrictedHeight += scrollThumb.getHeight();
        }
        if (bias) {
            restrictedHeight += -1;
        }
        params.height = restrictedHeight;
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
        private static final String[] DUMMY = new String[100];
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
            View view = inflater.inflate(androidx.car.test.R.layout.paged_list_item_column_card,
                    parent, false);
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
         * ViewHolder for AlphaJumpTestAdapter.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(androidx.car.test.R.id.text_view);
            }
        }
    }
}

