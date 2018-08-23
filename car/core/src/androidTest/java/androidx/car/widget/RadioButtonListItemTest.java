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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.view.View;

import androidx.car.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the layout configuration in {@link SeekbarListItem}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RadioButtonListItemTest {

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Test
    public void testDisableItem() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setEnabled(false);
        setupPagedListView(Arrays.asList(item));

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isEnabled());
    }

    @Test
    public void testSetPrimaryActionIcon_textLeft() {
        RadioButtonListItem emptyIcon = new RadioButtonListItem(mActivity);
        emptyIcon.setPrimaryActionEmptyIcon();

        RadioButtonListItem noIcon = new RadioButtonListItem(mActivity);
        noIcon.setPrimaryActionNoIcon();

        RadioButtonListItem smallIcon = new RadioButtonListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        RadioButtonListItem mediumIcon = new RadioButtonListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        RadioButtonListItem largeIcon = new RadioButtonListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<RadioButtonListItem> items = Arrays.asList(
                largeIcon, mediumIcon, smallIcon, emptyIcon, noIcon);
        // Set text so we can verify the offset.
        for (RadioButtonListItem item : items) {
            item.setText("text");
        }
        List<Integer> expectedStartMargin = Arrays.asList(
                R.dimen.car_keyline_4,  // Large icon.
                R.dimen.car_keyline_3,  // Medium icon.
                R.dimen.car_keyline_3,  // Small icon.
                R.dimen.car_keyline_3,  // Empty icon.
                R.dimen.car_keyline_1); // No icon.
        setupPagedListView(items);

        for (int i = 0; i < items.size(); i++) {
            RadioButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(i);

            int expected = InstrumentationRegistry.getContext().getResources()
                    .getDimensionPixelSize(expectedStartMargin.get(i));
            assertThat("Item index is " + i,
                    viewHolder.getText().getLeft(), is(equalTo(expected)));
        }
    }

    @Test
    public void testSetText() {
        CharSequence text = "text";
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setText(text);
        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getText().getText(), is(equalTo(text)));
    }

    @Test
    public void testInitialStateIsUnchecked() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testSetChecked() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setChecked(true);
        setupPagedListView(Arrays.asList(item));

        assertTrue(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testSetChecked_uncheckWorks() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setChecked(true);
        setupPagedListView(Arrays.asList(item));

        item.setChecked(false);
        refreshUi();

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testSetShowDivider() {
        RadioButtonListItem show = new RadioButtonListItem(mActivity);
        show.setShowDivider(true);
        RadioButtonListItem noshow = new RadioButtonListItem(mActivity);
        noshow.setShowDivider(false);

        setupPagedListView(Arrays.asList(show, noshow));

        assertThat(getViewHolderAtPosition(0).getRadioButtonDivider().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getRadioButtonDivider().getVisibility(),
                is(equalTo(View.GONE)));
    }

    @Test
    public void testSetOnClickListener() {
        boolean[] clicked = new boolean[]{false};

        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setOnClickListener(v -> clicked[0] = true);
        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, click()));

        assertTrue(clicked[0]);
    }

    @Test
    public void testOnCheckedChangedListener() {
        boolean[] clicked = new boolean[]{false};
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setOnCheckedChangeListener((buttonView, isChecked) -> clicked[0] = true);
        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.radio_button)));

        assertTrue(clicked[0]);
    }

    @Test
    public void testClickInterceptor_doesNotTriggerItemClickListener() {
        boolean[] clicked = new boolean[]{false};
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setOnClickListener(v -> clicked[0] = true);
        setupPagedListView(Arrays.asList(item));

        RadioButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.click_interceptor)));

        assertFalse(clicked[0]);
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.getAdapter().notifyDataSetChanged();
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private RadioButtonListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (RadioButtonListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    private void setupPagedListView(List<? extends ListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider<ListItem.ViewHolder>(
                new ArrayList(items));
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setAdapter(new ListItemAdapter(mActivity, provider));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specific id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
