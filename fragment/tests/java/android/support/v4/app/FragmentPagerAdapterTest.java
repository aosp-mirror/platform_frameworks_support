/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.app;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.fragment.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FragmentPagerAdapterTest {

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<>(FragmentTestActivity.class);

    @Test
    public void restoreVisibility() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        final ViewGroup content = (ViewGroup) activity.findViewById(R.id.content);

        final ViewPager viewPager = new ViewPager(InstrumentationRegistry.getContext());
        viewPager.setId(R.id.pager);

        final TestFragmentPagerAdapter adapter = new TestFragmentPagerAdapter(fragmentManager);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                content.addView(viewPager);
                viewPager.setAdapter(adapter);
                fragmentManager.executePendingTransactions();
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        TestFragment firstItem = adapter.firstItem;
        TestFragment secondItem = adapter.secondItem;

        assertNotNull("first item not instantiated", firstItem);
        assertNotNull("second item not instantiated", secondItem);

        assertTrue("first item is not initially visible", firstItem.getUserVisibleHint());
        assertFalse("second item is initially visible", secondItem.getUserVisibleHint());

        // test capturing the toggle of visibility when changing items
        firstItem.captureVisibilityToggle();
        secondItem.captureVisibilityToggle();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(1);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue("first item visibility not toggled", firstItem.wasVisibilityToggled);
        assertTrue("second item visibility not toggled", secondItem.wasVisibilityToggled);

        assertTrue("second item not visible", secondItem.getUserVisibleHint());
        assertFalse("first item is visible", firstItem.getUserVisibleHint());

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                content.removeView(viewPager);
                fragmentManager.executePendingTransactions();
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // saving and restoring should not toggle visibility
        firstItem.captureVisibilityToggle();
        secondItem.captureVisibilityToggle();

        // save and restore
        final Parcel parcel = Parcel.obtain();
        Parcelable savedState = viewPager.onSaveInstanceState();
        savedState.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        final ViewPager restoredViewPager = new ViewPager(InstrumentationRegistry.getContext());
        restoredViewPager.setId(R.id.pager);

        final TestFragmentPagerAdapter restoredAdapter = new TestFragmentPagerAdapter(fragmentManager);


        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                content.addView(restoredViewPager);
                restoredViewPager.setAdapter(restoredAdapter);
                restoredViewPager.onRestoreInstanceState(ViewPager.SavedState.CREATOR.createFromParcel(parcel));
                fragmentManager.executePendingTransactions();
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals("first item is a new instance", firstItem, restoredAdapter.firstItem);
        assertEquals("second item is a new instance", secondItem, restoredAdapter.secondItem);

        assertFalse("first item visibility toggled when restoring", firstItem.wasVisibilityToggled);
        assertFalse("second item visibility toggled when restoring", secondItem.wasVisibilityToggled);

        assertEquals("second item is not restored selected", restoredViewPager.getCurrentItem(), 1);

        assertTrue("second item not restored visible", secondItem.getUserVisibleHint());
        assertFalse("first item is restored visible", firstItem.getUserVisibleHint());
    }

    static class TestFragmentPagerAdapter extends FragmentPagerAdapter {

        TestFragment firstItem;
        TestFragment secondItem;

        TestFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);

            switch (position) {
                case 0:
                    firstItem = (TestFragment) item;
                    break;

                case 1:
                    secondItem = (TestFragment) item;
                    break;
            }

            return item;
        }

        @Override
        public Fragment getItem(int position) {
            return new TestFragment();
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public static class TestFragment extends Fragment {

        private boolean shouldCaptureVisibilityToggle;

        public boolean wasVisibilityToggled;

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            if (shouldCaptureVisibilityToggle && !wasVisibilityToggled) {
                wasVisibilityToggled = getUserVisibleHint() != isVisibleToUser;
                shouldCaptureVisibilityToggle = false;
            }

            super.setUserVisibleHint(isVisibleToUser);
        }

        public void captureVisibilityToggle() {
            shouldCaptureVisibilityToggle = true;
            wasVisibilityToggled = false;
        }
    }
}
