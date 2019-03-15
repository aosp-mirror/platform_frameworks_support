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
package com.example.android.supportv7.widget.selection.fancy;

import android.graphics.Rect;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;

final class FancyHolder extends RecyclerView.ViewHolder {

    private final LinearLayout mContainer;
    public final TextView mSelector;
    public final TextView mLabel;
    public final TextView mMenu;
    private final ItemDetails<Uri> mDetails;

    private @Nullable Uri mKey;

    FancyHolder(LinearLayout layout) {
        super(layout);
        mContainer = layout.findViewById(R.id.container);
        mSelector = layout.findViewById(R.id.selector);
        mMenu = layout.findViewById(R.id.menu);
        mLabel = layout.findViewById(R.id.label);
        mMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mMenu.getContext(), "Tapped menu item", Toast.LENGTH_SHORT).show();
            }
        });
        mDetails = new ItemDetails<Uri>() {
            @Override
            public int getPosition() {
                return FancyHolder.this.getAdapterPosition();
            }

            @Override
            public Uri getSelectionKey() {
                return FancyHolder.this.mKey;
            }

            @Override
            public boolean allowHandleTap(@NonNull MotionEvent e) {
                return FancyHolder.this.allowHandleTap(e);
            }

            @Override
            public boolean inDragRegion(MotionEvent e) {
                return FancyHolder.this.inDragRegion(e);
            }

            @Override
            public boolean inSelectionHotspot(MotionEvent e) {
                return FancyHolder.this.inSelectRegion(e);
            }
        };
    }

    void update(Uri key, String label, boolean selected) {
        mKey = key;
        mLabel.setText(label);
        setSelected(selected);
    }

    private void setSelected(boolean selected) {
        mContainer.setActivated(selected);
        mSelector.setActivated(selected);
    }

    boolean allowHandleTap(MotionEvent event) {
        Rect iconRect = new Rect();
        mMenu.getGlobalVisibleRect(iconRect);
        return !iconRect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    boolean inSelectRegion(MotionEvent event) {
        Rect iconRect = new Rect();
        mSelector.getGlobalVisibleRect(iconRect);
        return iconRect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    boolean inDragRegion(MotionEvent event) {
        // If itemView is activated = selected, then whole region is interactive
        if (itemView.isActivated()) {
            return true;
        }

        // Do everything in global coordinates - it makes things simpler.
        int[] coords = new int[2];
        mSelector.getLocationOnScreen(coords);

        Rect textBounds = new Rect();
        mLabel.getPaint().getTextBounds(
                mLabel.getText().toString(), 0, mLabel.getText().length(), textBounds);

        Rect rect = new Rect(
                coords[0],
                coords[1],
                coords[0] + mSelector.getWidth() + textBounds.width(),
                coords[1] + Math.max(mSelector.getHeight(), textBounds.height()));

        // If the tap occurred inside icon or the text, these are interactive spots.
        return rect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    ItemDetails<Uri> getItemDetails() {
        return mDetails;
    }
}
