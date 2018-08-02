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

package androidx.slice.widget;

import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.widget.SliceViewUtil.resolveLayoutDirection;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;

/**
 * Base class representing content that can be displayed.
 */
public class SliceContent {

    protected SliceItem mSliceItem;
    protected SliceItem mColorItem;
    protected SliceItem mLayoutDirItem;
    protected SliceItem mContentDescr;

    public SliceContent() {
    }

    public SliceContent(SliceItem item) {
        if (item == null) return;
        mSliceItem = item;
        if (FORMAT_SLICE.equals(item.getFormat()) || FORMAT_ACTION.equals(item.getFormat())) {
            mColorItem = SliceQuery.findTopLevelItem(item.getSlice(), FORMAT_INT, SUBTYPE_COLOR,
                    null, null);
            mLayoutDirItem = SliceQuery.findTopLevelItem(item.getSlice(), FORMAT_INT,
                    SUBTYPE_LAYOUT_DIRECTION, null, null);
        }
        mContentDescr = SliceQuery.findSubtype(item, FORMAT_TEXT, SUBTYPE_CONTENT_DESCRIPTION);
    }

    /**
     * @return the slice item used to construct this content.
     */
    @Nullable
    public SliceItem getSliceItem() {
        return mSliceItem;
    }

    /**
     * @return the accent color to use for this content or -1 if no color is set.
     */
    public int getAccentColor() {
        return mColorItem != null ? mColorItem.getInt() : -1;
    }

    /**
     * @return the layout direction to use for this content or -1 if no direction set.
     */
    public int getLayoutDir() {
        return mLayoutDirItem != null ? resolveLayoutDirection(mLayoutDirItem.getInt()) : -1;
    }

    /**
     * @return the content description to use for this row if set.
     */
    @Nullable
    public CharSequence getContentDescription() {
        return mContentDescr != null ? mContentDescr.getText() : null;
    }

    /**
     * @return the desired height of this content based on the provided mode and context or the
     * default height if context is null.
     */
    public int getHeight(Context context, @SliceView.SliceMode int mode) {
        return 0;
    }

    /**
     * @return whether this content is valid to display or not.
     */
    public boolean isValid() {
        return mSliceItem != null;
    }
}
