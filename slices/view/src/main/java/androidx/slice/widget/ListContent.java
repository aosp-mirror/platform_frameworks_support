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

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.widget.SliceView.MODE_LARGE;
import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information required to present content in a list format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(19)
public class ListContent extends SliceContent {

    private RowContent mHeaderContent;
    private RowContent mSeeMoreContent;
    private ArrayList<SliceContent> mRowItems = new ArrayList<>();
    private List<SliceAction> mSliceActions;
    private Context mContext;
    private int mMinScrollHeight;
    private int mLargeHeight;
    private int mMaxSmallHeight;

    private int mGridTopPadding;
    private int mGridBottomPadding;

    public ListContent(Context context, @NonNull Slice slice) {
        this(context, slice, null, 0, 0);
    }

    public ListContent(Context context, @NonNull Slice slice, SliceStyle styles) {
        super(new SliceItem(slice, FORMAT_SLICE, null, slice.getHints()));
        init(context, styles);
        populate(slice);
    }

    public ListContent(Context context, Slice slice, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(new SliceItem(slice, FORMAT_SLICE, null, slice.getHints()));
        init(context, null);

        if (context != null) {
            Resources.Theme theme = context.getTheme();
            if (theme != null) {
                TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.SliceView,
                        defStyleAttr, defStyleRes);
                try {
                    mGridTopPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding, 0);
                    mGridBottomPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding,
                            0);
                } finally {
                    a.recycle();
                }
            }
        }

        populate(slice);
    }

    private void init(Context context, SliceStyle styles) {
        if (mSliceItem == null) {
            return;
        }
        mContext = context;
        mGridTopPadding = styles != null ? styles.getGridTopPadding() : 0;
        mGridBottomPadding = styles != null ? styles.getGridBottomPadding() : 0;
        if (context != null) {
            mMinScrollHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
            mLargeHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.abc_slice_large_height);
        }
    }

    public void setMaxSmallHeight(int maxSmallHeight) {
        mMaxSmallHeight = maxSmallHeight;
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    private boolean populate(Slice slice) {
        if (slice == null) return false;
        mSliceActions = SliceMetadata.getSliceActions(slice);
        final SliceItem headerItem = findHeaderItem(slice);
        if (headerItem != null) {
            mHeaderContent = new RowContent(mContext, headerItem, true);
            mRowItems.add(mHeaderContent);
        }
        final SliceItem seeMoreItem = getSeeMoreItem(slice);
        if (seeMoreItem != null) {
            mSeeMoreContent = new RowContent(mContext, seeMoreItem, false);
        }

        // Filter + create row items
        List<SliceItem> children = slice.getItems();
        for (int i = 0; i < children.size(); i++) {
            final SliceItem child = children.get(i);
            final String format = child.getFormat();
            boolean isNonRowContent = child.hasAnyHints(HINT_ACTIONS, HINT_SEE_MORE, HINT_KEYWORDS,
                    HINT_TTL, HINT_LAST_UPDATED);
            if (!isNonRowContent && (FORMAT_ACTION.equals(format) || FORMAT_SLICE.equals(format))) {
                if (mHeaderContent == null && !child.hasHint(HINT_LIST_ITEM)) {
                    mHeaderContent = new RowContent(mContext, child, true);
                    mRowItems.add(0, mHeaderContent);
                } else if (child.hasHint(HINT_LIST_ITEM)) {
                    if (child.hasHint(HINT_HORIZONTAL)) {
                        mRowItems.add(new GridContent(mContext, child));
                    } else {
                        mRowItems.add(new RowContent(mContext, child, false));
                    }
                }
            }
        }
        // Ensure we have something for the header -- use first row
        if (mHeaderContent == null && mRowItems.size() >= 1) {
            // TODO.... might need to check for RowContent vs GridContent here, we're protected
            // on builder side but not view side.
            mHeaderContent = (RowContent) mRowItems.get(0);
        }
        return isValid();
    }

    /**
     * @return height of this list when displayed in small mode.
     */
    public int getSmallHeight() {
        return getHeight(mHeaderContent, 0 /* rowIndex */, 1 /* rowCount */, MODE_SMALL);
    }

    /**
     * @param maxHeight max height we can be, -1 if no max.
     * @param scrollable whether scrolling is allowed.
     * @return height of the list when displayed in large mode.
     */
    public int getLargeHeight(int maxHeight, boolean scrollable) {
        int desiredHeight = getListHeight(mRowItems);
        if (maxHeight > 0) {
            // Always ensure we're at least the height of our small version.
            maxHeight = Math.max(getSmallHeight(), maxHeight);
        }
        int maxLargeHeight = maxHeight > 0
                ? maxHeight
                : mLargeHeight;
        // Do we have enough content to reasonably scroll in our max?
        boolean bigEnoughToScroll = desiredHeight - maxLargeHeight >= mMinScrollHeight;

        // Adjust for scrolling
        int height = bigEnoughToScroll ? maxLargeHeight
                : maxHeight <= 0 ? desiredHeight
                : Math.min(maxLargeHeight, desiredHeight);
        if (!scrollable) {
            height = getListHeight(getItemsForNonScrollingList(height));
        }
        return height;
    }

    /**
     * Expects the provided list of items to be filtered (i.e. only things that can be turned into
     * GridContent or RowContent) and in order (i.e. first item could be a header).
     *
     * @return the total height of all the rows contained in the provided list.
     */
    public int getListHeight(List<SliceContent> listItems) {
        if (listItems == null || mContext == null) {
            return 0;
        }
        int height = 0;
        SliceContent maybeHeader = null;
        if (!listItems.isEmpty()) {
            maybeHeader = listItems.get(0);
        }
        if (listItems.size() == 1 && !maybeHeader.getSliceItem().hasHint(HINT_HORIZONTAL)) {
            return getHeight(maybeHeader, 0, 1, MODE_LARGE);
        }
        int rowCount = listItems.size();
        for (int i = 0; i < listItems.size(); i++) {
            height += getHeight(listItems.get(i), i, rowCount, MODE_LARGE);
        }
        return height;
    }

    /**
     * Returns a list of items that can be displayed in the provided height. If this list
     * has a see more item this will be displayed in the list if appropriate.
     *
     * @param height to use to determine the row items to return.
     *
     * @return the list of items that can be displayed in the provided height.
     */
    @NonNull
    public ArrayList<SliceContent> getItemsForNonScrollingList(int height) {
        ArrayList<SliceContent> visibleItems = new ArrayList<>();
        if (mRowItems == null || mRowItems.size() == 0) {
            return visibleItems;
        }
        final int minItemCount = hasHeader() ? 2 : 1;
        int visibleHeight = 0;
        // Need to show see more
        if (mSeeMoreContent != null) {
            visibleHeight += mSeeMoreContent.getActualHeight(mMaxSmallHeight);
        }
        int rowCount = mRowItems.size();
        for (int i = 0; i < rowCount; i++) {
            int itemHeight = getHeight(mRowItems.get(i), i, rowCount, MODE_LARGE);
            if (height > 0 && visibleHeight + itemHeight > height) {
                break;
            } else {
                visibleHeight += itemHeight;
                visibleItems.add(mRowItems.get(i));
            }
        }
        if (mSeeMoreContent != null && visibleItems.size() >= minItemCount
                && visibleItems.size() != rowCount) {
            // Only add see more if we're at least showing one item and it's not the header
            visibleItems.add(mSeeMoreContent);
        }
        if (visibleItems.size() == 0) {
            // Didn't have enough space to show anything; should still show something
            visibleItems.add(mRowItems.get(0));
        }
        return visibleItems;
    }

    /**
     * Determines the height of the provided {@link SliceItem}.
     */
    private int getHeight(SliceContent content, int index, int count, int mode) {
        if (mContext == null || content == null || !content.isValid()) {
            return 0;
        }
        if (content instanceof GridContent) {
            GridContent gc = (GridContent) content;
            int topPadding = gc.isAllImages() && index == 0 ? mGridTopPadding : 0;
            int bottomPadding = gc.isAllImages() && index == count - 1 ? mGridBottomPadding : 0;
            return gc.getHeight(mContext, mode) + topPadding + bottomPadding;
        } else {
            return content.getHeight(mContext, mode);
        }
    }

    /**
     * @return whether this list has content that is valid to display.
     */
    public boolean isValid() {
        boolean ret = super.isValid() && mRowItems.size() > 0;
        Log.w("mady", "LC valid? " + ret);
        return ret;
    }

    @Nullable
    public SliceItem getColorItem() {
        return mColorItem;
    }

    @Nullable
    public RowContent getHeader() {
        return mHeaderContent;
    }

    @Nullable
    public List<SliceAction> getSliceActions() {
        return mSliceActions;
    }

    @NonNull
    public ArrayList<SliceContent> getRowItems() {
        return mRowItems;
    }

    /**
     * @return whether this list has an explicit header (i.e. row item without HINT_LIST_ITEM)
     */
    public boolean hasHeader() {
        return mHeaderContent != null && isValidHeader(mHeaderContent.getSliceItem());
    }

    /**
     * @return the type of template that the header represents.
     */
    public int getHeaderTemplateType() {
        return getRowType(mHeaderContent, true, mSliceActions);
    }

    /**
     * The type of template that the provided row item represents.
     *
     * @param context context used for this slice.
     * @param rowItem the row item to determine the template type of.
     * @param isHeader whether this row item is used as a header.
     * @param actions the actions associated with this slice, only matter if this row is the header.
     * @return the type of template the provided row item represents.
     */
    public static int getRowType(SliceContent rowItem, boolean isHeader,
                                 List<SliceAction> actions) {
        if (rowItem != null) {
            if (rowItem instanceof GridContent) {
                return EventInfo.ROW_TYPE_GRID;
            } else {
                RowContent rc = (RowContent) rowItem;
                SliceItem actionItem = rc.getPrimaryAction();
                SliceAction primaryAction = null;
                if (actionItem != null) {
                    primaryAction = new SliceActionImpl(actionItem);
                }
                if (rc.getRange() != null) {
                    return FORMAT_ACTION.equals(rc.getRange().getFormat())
                            ? EventInfo.ROW_TYPE_SLIDER
                            : EventInfo.ROW_TYPE_PROGRESS;
                } else if (primaryAction != null && primaryAction.isToggle()) {
                    return EventInfo.ROW_TYPE_TOGGLE;
                } else if (isHeader && actions != null) {
                    for (int i = 0; i < actions.size(); i++) {
                        if (actions.get(i).isToggle()) {
                            return EventInfo.ROW_TYPE_TOGGLE;
                        }
                    }
                    return EventInfo.ROW_TYPE_LIST;
                } else {
                    return rc.getToggleItems().size() > 0
                            ? EventInfo.ROW_TYPE_TOGGLE
                            : EventInfo.ROW_TYPE_LIST;
                }
            }
        }
        return EventInfo.ROW_TYPE_LIST;
    }

    /**
     * @return the primary action for this list; i.e. action on the header or first row.
     */
    @Nullable
    public SliceItem getPrimaryAction() {
        SliceItem action = null;
        if (mHeaderContent != null) {
            action = mHeaderContent.getPrimaryAction();
        }
        if (action == null) {
            String[] hints = new String[]{HINT_SHORTCUT, HINT_TITLE};
            action = SliceQuery.find(mSliceItem, FORMAT_ACTION, hints, null);
        }
        if (action == null) {
            action = SliceQuery.find(mSliceItem, FORMAT_ACTION, (String) null, null);
        }
        return action;
    }

    @Nullable
    public ShortcutContent getShortcutContent() {
        return new ShortcutContent(this);
    }

    @Nullable
    private static SliceItem findHeaderItem(@NonNull Slice slice) {
        // See if header is specified
        String[] nonHints = new String[] {HINT_LIST_ITEM, HINT_SHORTCUT, HINT_ACTIONS,
                HINT_KEYWORDS, HINT_TTL, HINT_LAST_UPDATED, HINT_HORIZONTAL};
        SliceItem header = SliceQuery.find(slice, FORMAT_SLICE, null, nonHints);
        if (header != null && isValidHeader(header)) {
            return header;
        }
        return null;
    }

    @Nullable
    private static SliceItem getSeeMoreItem(@NonNull Slice slice) {
        SliceItem item = SliceQuery.findTopLevelItem(slice, null, null,
                new String[] {HINT_SEE_MORE}, null);
        if (item != null) {
            if (FORMAT_SLICE.equals(item.getFormat())) {
                List<SliceItem> items = item.getSlice().getItems();
                if (items.size() == 1 && FORMAT_ACTION.equals(items.get(0).getFormat())) {
                    return items.get(0);
                }
                return item;
            }
        }
        return null;
    }

    /**
     * @return whether the provided slice item is a valid header.
     */
    public static boolean isValidHeader(SliceItem sliceItem) {
        if (FORMAT_SLICE.equals(sliceItem.getFormat()) && !sliceItem.hasAnyHints(HINT_LIST_ITEM,
                HINT_ACTIONS, HINT_KEYWORDS, HINT_SEE_MORE)) {
             // Minimum valid header is a slice with text
            SliceItem item = SliceQuery.find(sliceItem, FORMAT_TEXT, (String) null, null);
            return item != null;
        }
        return false;
    }
}
