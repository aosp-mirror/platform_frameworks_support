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

package androidx.viewpager2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Work in progress: go/viewpager2
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class TabLayoutMediator {

    private final @NonNull TabLayout mTabLayout;
    private final @NonNull ViewPager2 mViewPager;
    private RecyclerView.Adapter mAdapter;

    private TabLayoutOnPageChangeListener mOnPageChangeListener;
    private TabLayout.OnTabSelectedListener mOnTabSelectedListener;
    private RecyclerView.AdapterDataObserver mPagerAdapterObserver;

    public TabLayoutMediator(@NonNull TabLayout tabLayout, @NonNull ViewPager2 viewPager) {
        this(tabLayout, viewPager, true, true);
    }

    public TabLayoutMediator(@NonNull TabLayout tabLayout, @NonNull ViewPager2 viewPager,
            boolean autoRefresh) {
        this(tabLayout, viewPager, autoRefresh, true);
    }

    public TabLayoutMediator(@NonNull TabLayout tabLayout, @NonNull ViewPager2 viewPager,
            boolean autoRefresh, boolean attachNow) {
        mTabLayout = tabLayout;
        mViewPager = viewPager;
        if (attachNow) {
            attach(autoRefresh);
        }
    }

    /**
     * Link the TabLayout and the ViewPager together
     *
     * @param autoRefresh
     */
    public void attach(boolean autoRefresh) {
        mAdapter = mViewPager.getAdapter();
        if (mAdapter == null) {
            throw new IllegalStateException("TabLayoutMediator attached before ViewPager2 has an "
                    + "adapter");
        }

        // Add our custom OnPageChangeListener to the ViewPager
        mOnPageChangeListener = new TabLayoutOnPageChangeListener(mTabLayout);
        mOnPageChangeListener.reset();
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);

        // Now we'll add a tab selected listener to set ViewPager's current item
        mOnTabSelectedListener = new ViewPagerOnTabSelectedListener(mViewPager);
        mTabLayout.addOnTabSelectedListener(mOnTabSelectedListener);

        // Now we'll populate ourselves from the pager adapter, adding an observer if
        // autoRefresh is enabled
        if (autoRefresh) {
            // Register our observer on the new adapter
            mPagerAdapterObserver = new PagerAdapterObserver();
            mAdapter.registerAdapterDataObserver(mPagerAdapterObserver);
        }

        populateFromPagerAdapter();

        // Now update the scroll position to match the ViewPager's current item
        mTabLayout.setScrollPosition(mViewPager.getCurrentItem(), 0f, true);
    }

    /**
     * Unlink the TabLayout and the ViewPager
     */
    public void detach() {
        mAdapter.unregisterAdapterDataObserver(mPagerAdapterObserver);
        mTabLayout.removeOnTabSelectedListener(mOnTabSelectedListener);
        mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
        mPagerAdapterObserver = null;
        mOnTabSelectedListener = null;
        mOnPageChangeListener = null;
    }

    /**
     * Called to configure the tab for the page at the specified position. Typically calls {@link
     * TabLayout.Tab#setText(CharSequence)}, but any form of styling can be applied.
     *
     * @param tab The Tab which should be configured to represent the title of the item at the given
     *        position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    public abstract void onConfigureTab(@NonNull TabLayout.Tab tab, int position);

    @SuppressWarnings("WeakerAccess")
    void populateFromPagerAdapter() {
        mTabLayout.removeAllTabs();

        if (mAdapter != null) {
            int adapterCount = mAdapter.getItemCount();
            for (int i = 0; i < adapterCount; i++) {
                TabLayout.Tab tab = mTabLayout.newTab();
                onConfigureTab(tab, i);
                mTabLayout.addTab(tab, false);
            }

            // Make sure we reflect the currently set ViewPager item
            if (mViewPager != null && adapterCount > 0) {
                int currItem = mViewPager.getCurrentItem();
                if (currItem != mTabLayout.getSelectedTabPosition()
                        && currItem < mTabLayout.getTabCount()) {
                    mTabLayout.getTabAt(currItem).select();
                }
            }
        }
    }

    /**
     * A {@link ViewPager2.OnPageChangeListener} class which contains the necessary calls back to
     * the provided {@link TabLayout} so that the tab position is kept in sync.
     *
     * <p>This class stores the provided TabLayout weakly, meaning that you can use {@link
     * ViewPager2#addOnPageChangeListener(ViewPager2.OnPageChangeListener)}
     * addOnPageChangeListener(OnPageChangeListener)} without removing the listener and not cause a
     * leak.
     */
    public static class TabLayoutOnPageChangeListener implements ViewPager2.OnPageChangeListener {
        private final WeakReference<TabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListener(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                // Only update the text selection if we're not settling, or we are settling after
                // being dragged
                boolean updateText = mScrollState != SCROLL_STATE_SETTLING
                        || mPreviousScrollState == SCROLL_STATE_DRAGGING;
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
                boolean updateIndicator = !(mScrollState == SCROLL_STATE_SETTLING
                        && mPreviousScrollState == SCROLL_STATE_IDLE);
//                tabLayout.setScrollPosition(position, positionOffset, updateText);
                setScrollPosition(tabLayout, position, positionOffset, updateText, updateIndicator);
            }
        }

        @Override
        public void onPageSelected(final int position) {
            TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null
                    && tabLayout.getSelectedTabPosition() != position
                    && position < tabLayout.getTabCount()) {
                // Select the tab, only updating the indicator if we're not being dragged/settled
                // (since onPageScrolled will handle that).
                boolean updateIndicator = mScrollState == SCROLL_STATE_IDLE
                        || (mScrollState == SCROLL_STATE_SETTLING
                        && mPreviousScrollState == SCROLL_STATE_IDLE);
//                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator);
                selectTab(tabLayout, tabLayout.getTabAt(position), updateIndicator);
//                tabLayout.getTabAt(position).select();
            }
        }

        void reset() {
            mPreviousScrollState = mScrollState = SCROLL_STATE_IDLE;
        }
    }

    private static Method sSetScrollPosition;
    private static Method sSelectTab;
    private static final String SET_SCROLL_POSITION_NAME = "TabLayout.setScrollPosition(int, float,"
            + " boolean, boolean)";
    private static final String SELECT_TAB_NAME = "TabLayout.selectTab(TabLayout.Tab, boolean)";

    static {
        try {
            sSetScrollPosition = TabLayout.class.getDeclaredMethod("setScrollPosition", int.class,
                    float.class, boolean.class, boolean.class);
            sSetScrollPosition.setAccessible(true);

            sSelectTab = TabLayout.class.getDeclaredMethod("selectTab", TabLayout.Tab.class,
                    boolean.class);
            sSelectTab.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Can't reflect into method TabLayout"
                    + ".setScrollPosition(int, float, boolean, boolean)");
        }
    }

    @SuppressWarnings("WeakerAccess")
    static void setScrollPosition(TabLayout tabLayout, int position, float positionOffset,
            boolean updateSelectedText, boolean updateIndicatorPosition) {
        try {
            if (sSetScrollPosition != null) {
                sSetScrollPosition.invoke(tabLayout, position, positionOffset, updateSelectedText,
                        updateIndicatorPosition);
            } else {
                throwMethodNotFound(SET_SCROLL_POSITION_NAME);
            }
        } catch (Exception e) {
            throwInvokeFailed(SET_SCROLL_POSITION_NAME);
        }
    }

    static void selectTab(TabLayout tabLayout, TabLayout.Tab tab, boolean updateIndicator) {
        try {
            if (sSelectTab != null) {
                sSelectTab.invoke(tabLayout, tab, updateIndicator);
            } else {
                throwMethodNotFound(SELECT_TAB_NAME);
            }
        } catch (Exception e) {
            throwInvokeFailed(SELECT_TAB_NAME);
        }
    }

    private static void throwMethodNotFound(String method) {
        throw new IllegalStateException("Method " + method + " not found");
    }

    private static void throwInvokeFailed(String method) {
        throw new IllegalStateException("Couldn't invoke method " + method);
    }

    /**
     * A {@link TabLayout.OnTabSelectedListener} class which contains the necessary calls back to
     * the provided {@link ViewPager2} so that the tab position is kept in sync.
     */
    public static class ViewPagerOnTabSelectedListener implements TabLayout.OnTabSelectedListener {
        private final ViewPager2 mViewPager;

        public ViewPagerOnTabSelectedListener(ViewPager2 viewPager) {
            this.mViewPager = viewPager;
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition(), true);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // No-op
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // No-op
        }
    }

    private class PagerAdapterObserver extends RecyclerView.AdapterDataObserver {
        PagerAdapterObserver() {}

        @Override
        public void onChanged() {
            populateFromPagerAdapter();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            populateFromPagerAdapter();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            populateFromPagerAdapter();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            populateFromPagerAdapter();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            populateFromPagerAdapter();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            populateFromPagerAdapter();
        }
    }
}
