/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.widget;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class GridRowView extends SliceChildView implements View.OnClickListener,
        View.OnTouchListener {

    private static final String TAG = "GridRowView";

    private static final int TITLE_TEXT_LAYOUT = R.layout.abc_slice_title;
    private static final int TEXT_LAYOUT = R.layout.abc_slice_secondary_text;

    // Max number of text items that can show in a cell
    private static final int MAX_CELL_TEXT = 2;
    // Max number of text items that can show in a cell if the mode is small
    private static final int MAX_CELL_TEXT_SMALL = 1;
    // Max number of images that can show in a cell
    private static final int MAX_CELL_IMAGES = 1;

    private int mRowIndex;
    private int mRowCount;

    private int mLargeImageHeight;
    private int mSmallImageSize;
    private int mSmallImageMinWidth;
    private int mIconSize;
    private int mGutter;
    private int mTextPadding;

    private GridContent mGridContent;
    private ViewBinder mViewContainer;
    private ViewBinder mForeground;
    int mMaxCells = -1;

    boolean mMaxCellUpdateScheduled;

    public GridRowView(Context context) {
        super(context);
        final Resources res = getContext().getResources();
        mViewContainer = mRootBinder.addChild(R.layout.abc_grid_container);

        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mSmallImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        mLargeImageHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
        mSmallImageMinWidth = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_min_width);
        mGutter = res.getDimensionPixelSize(R.dimen.abc_slice_grid_gutter);
        mTextPadding = res.getDimensionPixelSize(R.dimen.abc_slice_grid_text_padding);

        mForeground = mRootBinder.addChild(R.layout.abc_foreground);
    }

    @Override
    public void setInsets(int l, int t, int r, int b) {
        super.setInsets(l, t, r, b);
        mViewContainer.setPadding(l, t + getExtraTopPadding(), r, b + getExtraBottomPadding());
    }

    private int getExtraTopPadding() {
        if (mGridContent != null && mGridContent.isAllImages()) {
            // Might need to add padding if in first or last position
            if (mRowIndex == 0) {
                return mSliceStyle != null ? mSliceStyle.getGridTopPadding() : 0;
            }
        }
        return 0;
    }

    private int getExtraBottomPadding() {
        if (mGridContent != null && mGridContent.isAllImages()) {
            if (mRowIndex == mRowCount - 1 || getMode() == MODE_SMALL) {
                return mSliceStyle != null ? mSliceStyle.getGridBottomPadding() : 0;
            }
        }
        return 0;
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int height = mGridContent.getHeight(mSliceStyle, mViewPolicy);
//        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
//        mViewContainer.getLayoutParams().height = height;
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    }

    @Override
    public void setTint(@ColorInt int tintColor) {
        super.setTint(tintColor);
        if (mGridContent != null) {
            // TODO -- could be smarter about this
            resetView();
            populateViews();
        }
    }

    /**
     * This is called when GridView is being used as a component in a larger template.
     */
    @Override
    public void setSliceItem(SliceContent slice, boolean isHeader, int rowIndex,
            int rowCount, SliceView.OnSliceActionListener observer) {
        resetView();
        setSliceActionListener(observer);
        mRowIndex = rowIndex;
        mRowCount = rowCount;
        mGridContent = (GridContent) slice;

        if (!scheduleMaxCellsUpdate()) {
            populateViews();
        }
        mViewContainer.setPadding(mInsetStart, mInsetTop + getExtraTopPadding(), mInsetEnd,
                mInsetBottom + getExtraBottomPadding());
    }

    /**
     * Schedules update to determine the max number of cells that can be shown in this grid.
     *
     * @return true if update was scheduled, false if it wasn't needed
     */
    private boolean scheduleMaxCellsUpdate() {
        if (mGridContent == null || !mGridContent.isValid()) {
            return true;
        }
        if (mRootBinder.getWidth() == 0) {
            // Need to wait for layout pass so we know how much width we have for the grid items.
            mMaxCellUpdateScheduled = true;
            mRootBinder.addSizeListener(mMaxCellsUpdater);
            //getViewTreeObserver().addOnPreDrawListener(mMaxCellsUpdater);
            return true;
        } else {
            mMaxCells = getMaxCells();
            return false;
        }
    }

    int getMaxCells() {
        if (mGridContent == null || !mGridContent.isValid() || mRootBinder.getWidth() == 0) {
            return -1;
        }
        ArrayList<GridContent.CellContent> cells = mGridContent.getGridContent();
        if (cells.size() > 1) {
            int desiredCellWidth = mGridContent.getLargestImageMode() == LARGE_IMAGE
                    ? mLargeImageHeight
                    : mSmallImageMinWidth;
            return mRootBinder.getWidth() / (desiredCellWidth + mGutter);
        } else {
            return 1;
        }
    }

    void populateViews() {
        if (mGridContent == null || !mGridContent.isValid()) {
            resetView();
            return;
        }
        if (scheduleMaxCellsUpdate()) {
            return;
        }
        if (mGridContent.getLayoutDir() != -1) {
            mRootBinder.setLayoutDirection(mGridContent.getLayoutDir());
        }
        if (mGridContent.getContentIntent() != null) {
            EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_CONTENT,
                    EventInfo.ROW_TYPE_GRID, mRowIndex);
            Pair<SliceItem, EventInfo> tagItem = new Pair<>(mGridContent.getContentIntent(), info);
            mViewContainer.setTag(tagItem);
            makeEntireGridClickable(true);
        }
        CharSequence contentDescr = mGridContent.getContentDescription();
        if (contentDescr != null) {
            mViewContainer.setContentDescription(contentDescr);
        }
        ArrayList<GridContent.CellContent> cells = mGridContent.getGridContent();
        if (mGridContent.getLargestImageMode() == LARGE_IMAGE) {
            mViewContainer.setGravity(Gravity.TOP);
        } else {
            mViewContainer.setGravity(Gravity.CENTER_VERTICAL);
        }
        int maxCells = mMaxCells;
        boolean hasSeeMore = mGridContent.getSeeMoreItem() != null;
        for (int i = 0; i < cells.size(); i++) {
            if (mViewContainer.getChildCount() >= maxCells) {
                if (hasSeeMore) {
                    addSeeMoreCount(cells.size() - maxCells);
                }
                break;
            }
            addCell(cells.get(i), i, Math.min(cells.size(), maxCells));
        }
    }

    private void addSeeMoreCount(int numExtra) {
        // Remove last element
        ViewBinder last = mViewContainer.getChildAt(mViewContainer.getChildCount() - 1);
        mViewContainer.removeView(last);

        SliceItem seeMoreItem = mGridContent.getSeeMoreItem();
        int index = mViewContainer.getChildCount();
        int total = mMaxCells;
        if ((FORMAT_SLICE.equals(seeMoreItem.getFormat())
                || FORMAT_ACTION.equals(seeMoreItem.getFormat()))
                && seeMoreItem.getSlice().getItems().size() > 0) {
            // It's a custom see more cell, add it
            addCell(new GridContent.CellContent(seeMoreItem), index, total);
            return;
        }

        // Default see more, create it
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewBinder extraText;
        ViewBinder seeMoreView;
        if (mGridContent.isAllImages()) {
            seeMoreView = mViewContainer.addChild(R.layout.abc_slice_grid_see_more_overlay);
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);
        } else {
            seeMoreView = mViewContainer.addChild(R.layout.abc_slice_grid_see_more);
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);

            // Update text appearance
            ViewBinder moreText = seeMoreView.findViewById(R.id.text_see_more);
            if (mSliceStyle != null) {
                moreText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSliceStyle.getGridTitleSize());
                moreText.setTextColor(mSliceStyle.getTitleColor());
            }
        }
        extraText.setText(
                getContext().getResources().getString(R.string.abc_slice_more_content, numExtra));

        // Make it clickable
        EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_SEE_MORE,
                EventInfo.ROW_TYPE_GRID, mRowIndex);
        info.setPosition(EventInfo.POSITION_CELL, index, total);
        Pair<SliceItem, EventInfo> tagItem = new Pair<>(seeMoreItem, info);
        seeMoreView.setTag(tagItem);
        makeClickable(seeMoreView, true);
    }

    /**
     * Adds a cell to the grid view based on the provided {@link SliceItem}.
     */
    private void addCell(GridContent.CellContent cell, int index, int total) {
        final int maxCellText = getMode() == MODE_SMALL && mGridContent.hasImage()
                ? MAX_CELL_TEXT_SMALL : MAX_CELL_TEXT;
        ViewBinder cellContainer = mRootBinder.createChild(R.layout.abc_cell_container);

        ArrayList<SliceItem> cellItems = cell.getCellItems();
        SliceItem contentIntentItem = cell.getContentIntent();

        int textCount = 0;
        int imageCount = 0;
        boolean added = false;
        boolean isSingleItem = cellItems.size() == 1;
        List<SliceItem> textItems = null;
        // In small format we display one text item and prefer titles
        if (!isSingleItem && getMode() == MODE_SMALL) {
            // Get all our text items
            textItems = new ArrayList<>();
            for (SliceItem cellItem : cellItems) {
                if (FORMAT_TEXT.equals(cellItem.getFormat())) {
                    textItems.add(cellItem);
                }
            }
            // If we have more than 1 remove non-titles
            Iterator<SliceItem> iterator = textItems.iterator();
            while (textItems.size() > maxCellText) {
                SliceItem item = iterator.next();
                if (!item.hasAnyHints(HINT_TITLE, HINT_LARGE)) {
                    iterator.remove();
                }
            }
        }
        SliceItem prevItem = null;
        for (int i = 0; i < cellItems.size(); i++) {
            SliceItem item = cellItems.get(i);
            final String itemFormat = item.getFormat();
            int padding = determinePadding(prevItem);
            if (textCount < maxCellText && (FORMAT_TEXT.equals(itemFormat)
                    || FORMAT_LONG.equals(itemFormat))) {
                if (textItems != null && !textItems.contains(item)) {
                    continue;
                }
                if (addItem(item, mTintColor, cellContainer, padding, isSingleItem)) {
                    prevItem = item;
                    textCount++;
                    added = true;
                }
            } else if (imageCount < MAX_CELL_IMAGES && FORMAT_IMAGE.equals(item.getFormat())) {
                if (addItem(item, mTintColor, cellContainer, 0, isSingleItem)) {
                    prevItem = item;
                    imageCount++;
                    added = true;
                }
            }
        }
        if (added) {
            CharSequence contentDescr = cell.getContentDescription();
            if (contentDescr != null) {
                cellContainer.setContentDescription(contentDescr);
            }
            mViewContainer.addChild(cellContainer);
            // TODO: Layout margins.
            //if (index != total - 1) {
            //    // If we're not the last or only element add space between items
            //    MarginLayoutParams lp =
            //            (LinearLayout.MarginLayoutParams) cellContainer.getLayoutParams();
            //    lp.setMarginEnd(mGutter);
            //    cellContainer.setLayoutParams(lp);
            //}
            if (contentIntentItem != null) {
                EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_BUTTON,
                        EventInfo.ROW_TYPE_GRID, mRowIndex);
                info.setPosition(EventInfo.POSITION_CELL, index, total);
                Pair<SliceItem, EventInfo> tagItem = new Pair<>(contentIntentItem, info);
                cellContainer.setTag(tagItem);
                makeClickable(cellContainer, true);
            }
        }
    }

    /**
     * Adds simple items to a container. Simple items include icons, text, and timestamps.
     *
     * @param item      item to add to the container.
     * @param container the container to add to.
     * @param padding   the padding to apply to the item.
     * @param isSingle  whether this is the only item in the cell or not.
     * @return Whether an item was added.
     */
    private boolean addItem(SliceItem item, int color, ViewBinder container, int padding,
            boolean isSingle) {
        final String format = item.getFormat();
        ViewBinder addedView = null;
        if (FORMAT_TEXT.equals(format) || FORMAT_LONG.equals(format)) {
            boolean isTitle = SliceQuery.hasAnyHints(item, HINT_LARGE, HINT_TITLE);
            ViewBinder tv = container.addChild(isTitle
                    ? TITLE_TEXT_LAYOUT : TEXT_LAYOUT);
            if (mSliceStyle != null) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, isTitle
                        ? mSliceStyle.getGridTitleSize() : mSliceStyle.getGridSubtitleSize());
                tv.setTextColor(isTitle
                        ? mSliceStyle.getTitleColor() : mSliceStyle.getSubtitleColor());
            }
            CharSequence text = FORMAT_LONG.equals(format)
                    ? SliceViewUtil.getTimestampString(getContext(), item.getLong())
                    : item.getText();
            tv.setText(text);
            tv.setPadding(0, padding, 0, 0);
            addedView = tv;
        } else if (FORMAT_IMAGE.equals(format) && item.getIcon() != null) {
            Drawable d = item.getIcon().loadDrawable(getContext());
            if (d != null) {
                LinearLayout.LayoutParams lp;
                int layout = item.hasHint(HINT_LARGE)
                        ? (isSingle ? R.layout.abc_single_large_image : R.layout.abc_large_image)
                        : (item.hasHint(HINT_NO_TINT) ? R.layout.abc_grid_small_image
                                : R.layout.abc_grid_icon_image);
                ViewBinder iv = container.addChild(layout);
                iv.setImage(item.getIcon());
                if (color != -1 && !item.hasHint(HINT_NO_TINT)) {
                    iv.setColorFilter(color);
                }
                addedView = iv;
            }
        }
        return addedView != null;
    }

    private int determinePadding(SliceItem prevItem) {
        if (prevItem == null) {
            // No need for top padding
            return 0;
        } else if (FORMAT_IMAGE.equals(prevItem.getFormat())) {
            return mTextPadding;
        } else if (FORMAT_TEXT.equals(prevItem.getFormat())
                || FORMAT_LONG.equals(prevItem.getFormat())) {
            return mSliceStyle != null ? mSliceStyle.getVerticalGridTextPadding() : 0;
        }
        return 0;
    }

    private void makeEntireGridClickable(boolean isClickable) {
        mViewContainer.setTouchListener(isClickable);
        Pair<SliceItem, EventInfo> tagItem = (Pair<SliceItem, EventInfo>) mViewContainer.getTag();
        mViewContainer.setClickListener(mObserver, tagItem.first, tagItem.second);
        int backgroundAttr = android.R.attr.selectableItemBackground;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            backgroundAttr = android.R.attr.selectableItemBackgroundBorderless;
        }
        mForeground.setBackground(isClickable
                ? backgroundAttr
                : 0);
        mViewContainer.setClickable(isClickable);
    }

    private void makeClickable(ViewBinder layout, boolean isClickable) {
        if (isClickable) {
            Pair<SliceItem, EventInfo> tagItem = (Pair<SliceItem, EventInfo>) layout.getTag();
            layout.setClickListener(mObserver, tagItem.first, tagItem.second);
        } else {
            layout.clearClickListener();
        }
        int backgroundAttr = android.R.attr.selectableItemBackground;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            backgroundAttr = android.R.attr.selectableItemBackgroundBorderless;
        }
        layout.setBackground(isClickable
                ? backgroundAttr
                : 0);
        layout.setClickable(isClickable);
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        onForegroundActivated(event);
        return false;
    }

    private void onForegroundActivated(MotionEvent event) {
    }

    @Override
    public void resetView() {
        if (mMaxCellUpdateScheduled) {
            mMaxCellUpdateScheduled = false;
            mRootBinder.clearSizeListener(mMaxCellsUpdater);
        }
        mViewContainer.removeAllViews();
        mRootBinder.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
        makeEntireGridClickable(false);
    }

    private Runnable mMaxCellsUpdater = new Runnable() {
        @Override
        public void run() {
            mMaxCells = getMaxCells();
            populateViews();
            mMaxCellUpdateScheduled = false;
        }
    };
}
