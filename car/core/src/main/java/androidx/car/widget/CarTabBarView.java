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

package androidx.car.widget;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.car.R;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * A view that represents selectable tabs for building car applications. The tab bar will
 * automatically keep track of the last selected tab and will update visually when a tab is
 * selected.
 *
 * <p>By default, the tabs are laid out starting from the starting end of the tab bar. The first
 * tab is considered selected. To change the configuration of these defaults, call
 * {@link #setTabAlignment(int)} and {@link #setSelectedTab(int)} respectively.
 *
 * <p>Pass a {@link OnTabSelectedListener} to this class to be notified of when tabs are selected.
 * Setting a listener will not affect the visual indicator of the selected tab.
 *
 * <p>The tab bar has a fixed height of {@code R.dimen.car_app_bar_height}.
 */
public class CarTabBarView extends ViewGroup {
    private static final String TAG = "CarTabBarView";

    /** The key for a tag on a tab that will store the value of its position. */
    private static final int POSITION_TAG_KEY = R.id.car_tab_position;

    private final List<CarTabItem> mTabItems = new ArrayList<>();
    private final List<View> mTabViews = new ArrayList<>();
    private final int mHeight;
    private final int mHorizontalMargins;
    private final int mTabSeparatingMargin;

    private final float mSelectedTabAlpha;
    private final float mInactiveTabAlpha;

    /** The tab that is currently selected. Only one tab can be selected at a time. */
    private int mSelectedTab;

    // A convenience field for the total width taken by the tabs. This value also includes the
    // separating margins between the buttons.
    private int mWidthTakenByTabs;

    @TabAlignment private int mTabAlignment = TabAlignment.START;

    @Nullable private OnTabSelectListener mTabListener;

    /**
     * Callback invoked when a tab is selected.
     */
    public interface OnTabSelectListener {
        /**
         * Called when a tab has been selected by the user.
         *
         * @param position A 0-based position indicating the tab that has been selected.
         */
        void onTabSelected(int position);
    }

    /**
     * Specifies the horizontal alignment of the individual tabs within the tab bar.
     */
    @IntDef({TabAlignment.START, TabAlignment.CENTER})
    @Retention(SOURCE)
    public @interface TabAlignment {
        /** Lay out the tabs from the starting end of the view. */
        int START = 0;

        /** Center the tabs horizontally within the tab bar. */
        int CENTER = 1;
    }

    public CarTabBarView(Context context) {
        this(context, /* attrs= */ null);
    }

    public CarTabBarView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.carTabBarStyle);
    }

    public CarTabBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_Car_CarTabBar);
    }

    public CarTabBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources res = getResources();
        mHeight = res.getDimensionPixelSize(R.dimen.car_app_bar_height);
        mHorizontalMargins = res.getDimensionPixelSize(R.dimen.car_keyline_1);
        mTabSeparatingMargin = res.getDimensionPixelSize(R.dimen.car_padding_4);

        TypedValue outValue = new TypedValue();
        res.getValue(R.dimen.selected_tab_alpha, outValue, true);
        mSelectedTabAlpha = outValue.getFloat();

        res.getValue(R.dimen.inactive_tab_alpha, outValue, true);
        mInactiveTabAlpha = outValue.getFloat();

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CarTabBarView, defStyleAttr, defStyleRes);

        try {
            setTabAlignment(a.getInt(
                    R.styleable.CarTabBarView_carTabAlignment, TabAlignment.START));
        } finally {
            a.recycle();
        }
    }

    /**
     * Sets the horizontal alignment of the tab bar. If not set, the default alignment is
     * {@link TabAlignment#START}.
     *
     * @param tabAlignment The alignment of the tabs.
     * @see TabAlignment
     */
    public void setTabAlignment(@TabAlignment int tabAlignment) {
        mTabAlignment = tabAlignment;
        requestLayout();
    }

    /**
     * Sets the listener that will be invoked when a tab is selected.
     *
     * @param listener The listener that will be invoked or {@code null} to clear any previously
     *                 set listeners.
     */
    public void setOnTabSelectListener(@Nullable OnTabSelectListener listener) {
        mTabListener = listener;
    }

    /**
     * Manually set the tab that is considered selected. Selected tabs will have an visual
     * indication of selection.
     *
     * <p>If the given position is more than the number of tabs, then no tab will have the visual
     * indication of being selected.
     *
     * <p>Note that the value set here is not necessarily the value that is returned by
     * {@link #getSelectedTab()}. The reason for this is that the selected tab will automatically
     * update as the tabs are interacted with.
     *
     * <p>The default value for this is 0.
     *
     * @param position A 0-based position of the currently selected tab. Passing a value of 0, for
     *                 example, will result in the first tab being selected.
     */
    public void setSelectedTab(@IntRange(from = 0) int position) {
        mSelectedTab = Math.max(position, 0);
        updateSelectedTab();
    }

    /**
     * Returns the position of the tab that is currently selected.
     *
     * <p>This value will not necessarily match the value passed to {@link #setSelectedTab(int)}.
     * It will always match the last-selected tab, as determined by user interaction.
     *
     * @return The position of the last-selected tab.
     */
    @IntRange(from = 0)
    public int getSelectedTab() {
        return mSelectedTab;
    }

    /**
     * Sets the tabs that will be shown on this tab bar.
     *
     * <p>Passing a value of {@code null} will clear all the tabs. Clearing all the tabs will also
     * reset the selected tab back to the first position.
     *
     * @param tabItems The tabs on the tab bar or {@code null} to clear the tabs.
     */
    public void setTabs(@Nullable List<CarTabItem> tabItems) {
        mTabItems.clear();
        mTabViews.clear();
        removeAllViews();

        if (tabItems == null) {
            setSelectedTab(0);
            requestLayout();
            return;
        }

        mTabItems.addAll(tabItems);

        for (int i = 0, size = mTabItems.size(); i < size; i++) {
            View tab = createAndBindTabView(mTabItems.get(i));
            tab.setTag(POSITION_TAG_KEY, i);

            tab.setOnClickListener(v -> {
                int position = (int) v.getTag(POSITION_TAG_KEY);

                if (mTabListener != null) {
                    mTabListener.onTabSelected(position);
                }
                setSelectedTab(position);
            });

            mTabViews.add(tab);
            addView(tab);
        }

        updateSelectedTab();
        requestLayout();
    }

    /**
     * Creates and returns a view that represents the tab based on the information in the given
     * {@code CarTabItem}.
     */
    private View createAndBindTabView(CarTabItem tabItem) {
        View tab = LayoutInflater.from(getContext())
                .inflate(R.layout.tab_view, /* root= */ this, /* attachToRoot= */ false);

        ImageView iconView = tab.findViewById(R.id.car_tab_icon);

        Icon icon = tabItem.getIcon();
        iconView.setImageIcon(icon);
        iconView.setVisibility(icon == null ? GONE : VISIBLE);

        TextView tabTextView = tab.findViewById(R.id.car_tab_text);
        CharSequence text = tabItem.getText();
        tabTextView.setText(text);
        tabTextView.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);

        return tab;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidthTakenByTabs = 0;
        int desiredHeight = getPaddingTop() + mHeight + getPaddingBottom();

        if (mTabViews == null || mTabViews.isEmpty()) {
            setMeasuredDimension(0, resolveSize(desiredHeight, heightMeasureSpec));
            return;
        }

        // Add starting and ending margins to begin with and any padding.
        int widthTaken = 2 * mHorizontalMargins + getPaddingStart() + getPaddingEnd();

        // Each tab should take up as much width as needed, but be constrained by the height of
        // the tab view.
        int wrapMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int tabHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.AT_MOST);

        for (View tabView : mTabViews) {
            tabView.measure(wrapMeasureSpec, tabHeightMeasureSpec);
            mWidthTakenByTabs += tabView.getMeasuredWidth();
        }

        // Add spacing between each time.
        mWidthTakenByTabs += (mTabViews.size() - 1) * mTabSeparatingMargin;
        widthTaken += mWidthTakenByTabs;

        setMeasuredDimension(
                resolveSize(widthTaken, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mTabViews == null || mTabViews.isEmpty()) {
            return;
        }

        int height = bottom - top - getPaddingTop() - getPaddingBottom();

        // The horizontal width available to lay out the tabs.
        int contentWidth =
                right - left - getPaddingStart() - getPaddingEnd() - (2 * mHorizontalMargins);
        int layoutLeft = getPaddingStart() + mHorizontalMargins;

        switch (mTabAlignment) {
            case TabAlignment.START:
                layOutTabsFromLeft(layoutLeft, height);
                break;
            case TabAlignment.CENTER:
                layoutLeft += (contentWidth - mWidthTakenByTabs) / 2;
                layOutTabsFromLeft(layoutLeft, height);
                break;
            default:
                Log.e(TAG, "Unknown tab alignment set: " + mTabAlignment);
        }
    }

    /** Updates the visual indication of which tab is currently selected. */
    private void updateSelectedTab() {
        int numOfTabs = mTabViews.size();

        if (mSelectedTab >= numOfTabs) {
            return;
        }

        for (int i = 0; i < numOfTabs; i++) {
            mTabViews.get(i).setAlpha(i == mSelectedTab
                    ? mSelectedTabAlpha
                    : mInactiveTabAlpha);
        }
    }

    /**
     * Lays out all the views in {@link #mTabViews} from the given {@code left} position.
     *
     * @param left The starting left position.
     * @param height The current height of the tab bar.
     */
    private void layOutTabsFromLeft(int left, int height) {
        for (View tabView : mTabViews) {
            int top = getPaddingTop() + ((height - tabView.getMeasuredHeight()) / 2);
            tabView.layout(left, top, left + tabView.getMeasuredWidth(),
                    top + tabView.getMeasuredHeight());

            left += tabView.getMeasuredWidth() + mTabSeparatingMargin;
        }
    }
}
