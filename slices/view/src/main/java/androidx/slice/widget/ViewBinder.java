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

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;

@RequiresApi(19)
public class ViewBinder {
    public ViewBinder(Context context, int layout) {
    }

    public ViewBinder addChild(int layout) {
    }

    public void addChild(ViewBinder child) {
    }

    public ViewBinder createChild(int layout) {
    }

    public void setPadding(int l, int i, int r, int i1) {
    }

    public int getWidth() {
    }

    public int getHeight() {
    }

    public void addSizeListener(Runnable sizeListener) {
    }

    public void setLayoutDirection(int layoutDir) {
    }

    public void setTag(Object tagItem) {
    }

    public Object getTag() {
    }

    public void setContentDescription(CharSequence contentDescr) {
    }

    public void setGravity(int gravity) {
    }

    public int getChildCount() {
    }

    public ViewBinder getChildAt(int i) {
    }

    public void removeView(ViewBinder last) {
    }

    public ViewBinder findViewById(int id) {
    }

    public void setTextSize(int complexUnitPx, int gridTitleSize) {
    }

    public void setTextColor(int titleColor) {
    }

    public void setText(CharSequence string) {
    }

    public void setBackground(int background) {
    }

    public void clearClickListener() {
    }

    public void setClickable(boolean isClickable) {
    }

    public void setClickListener(SliceView.OnSliceActionListener observer, SliceItem sliceItem,
            EventInfo info) {
        if (sliceItem != null) {
            final SliceItem actionItem = SliceQuery.find(sliceItem,
                    FORMAT_ACTION, (String) null, null);
            if (actionItem != null) {
                try {
                    actionItem.fireAction(null, null);
                    if (observer != null) {
                        observer.onSliceAction(info, actionItem);
                    }
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "PendingIntent for slice cannot be sent", e);
                }
            }
        }
    }

    public void clearSizeListener(Runnable maxCellsUpdater) {
    }

    public void removeAllViews() {
    }

    public void setTouchListener(boolean isClickable) {
    }

    public void setImage(IconCompat icon) {
    }

    public void setColorFilter(int color) {
    }

    public void setImageBitmap(Bitmap circularBitmap) {
    }

    public void setImageDrawable(Drawable drawable) {
    }

    public ViewBinder createChildWithTintableBg(int view_version, int bg_version) {
    }

    public void setBackgroundTint(int accentColor) {
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
    }

    public void setForegroundTouches() {
    }
}
