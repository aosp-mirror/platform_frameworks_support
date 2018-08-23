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

package com.example.androidx.car;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.RadioButtonListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for {@link androidx.car.widget.RadioButtonListItem}.
 */
public class RadioButtonListItemActivity extends Activity {

    PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        mPagedListView = findViewById(R.id.paged_list_view);
        MyAdapter adapter = new MyAdapter(this,
                new ListItemProvider.ListProvider(createItems()),
                ListItemAdapter.BackgroundStyle.PANEL);

        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);
    }

    private List<? extends ListItem> createItems() {
        List<RadioButtonListItem> items = new ArrayList<>();

        RadioButtonListItem item;

        item = new RadioButtonListItem(this);
        item.setPrimaryActionEmptyIcon();
        item.setText("Empty icon");
        items.add(item);

        item = new RadioButtonListItem(this);
        item.setPrimaryActionNoIcon();
        item.setText("No icon");
        items.add(item);

        item = new RadioButtonListItem(this);
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);
        item.setText("Avatar sized icon");
        items.add(item);

        return items;
    }

    private static class MyAdapter extends ListItemAdapter {

        private static final int UNCHECKED = -1;
        private int mLastCheckedPosition = UNCHECKED;

        public void setChecked(int position) {
            mLastCheckedPosition = position;
        }

        public boolean isChecked(int position) {
            return position == mLastCheckedPosition;
        }

        public void clear() {
            mLastCheckedPosition = UNCHECKED;
        }

        MyAdapter(Context context, ListItemProvider itemProvider, int backgroundStyle) {
            super(context, itemProvider, backgroundStyle);
        }

        @Override
        public void onBindViewHolder(ListItem.ViewHolder vh, int position) {
            super.onBindViewHolder(vh, position);

            if (vh instanceof RadioButtonListItem.ViewHolder) {
                RadioButtonListItem.ViewHolder viewHolder = (RadioButtonListItem.ViewHolder) vh;

                viewHolder.getRadioButton().setChecked(isChecked(position));
                viewHolder.getRadioButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
                    mLastCheckedPosition = position;
                    // Refresh other radio button list items.
                    notifyDataSetChanged();
                });
            }
        }
    }
}
