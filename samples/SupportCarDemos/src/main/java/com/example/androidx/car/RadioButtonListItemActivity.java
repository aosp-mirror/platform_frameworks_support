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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.RadioButtonListItem;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

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

        RadioButtonController controller = new RadioButtonController();

        MyAdapter adapter = new MyAdapter(this,
                new ListItemProvider.ListProvider(createItems(controller)), ListItemAdapter.BackgroundStyle.PANEL);
        adapter.setRadioButtonController(controller);

        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);
    }

    private List<? extends ListItem> createItems(RadioButtonController controller) {
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

        for (RadioButtonListItem i : items) {
            i.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = mPagedListView.getRecyclerView().findContainingViewHolder(buttonView).getAdapterPosition();
                controller.setChecked(position);
                // Refresh to update other radio button list items.
                mPagedListView.getAdapter().notifyDataSetChanged();
            });
            i.setOnClickListener(view -> {
                int position = mPagedListView.getRecyclerView().findContainingViewHolder(view).getAdapterPosition();
                controller.setChecked(position);
                // Refresh to update other radio button list items.
                mPagedListView.getAdapter().notifyDataSetChanged();
            });
        }
        return items;
    }

    private static class RadioButtonController {
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
    }

    private static class MyAdapter extends ListItemAdapter {

        private RadioButtonController mController;

        public MyAdapter(Context context, ListItemProvider itemProvider, int backgroundStyle) {
            super(context, itemProvider, backgroundStyle);

            setHasStableIds(true);
        }

        public void setRadioButtonController(RadioButtonController controller) {
            mController = controller;
        }

        @Override
        public void onBindViewHolder(ListItem.ViewHolder vh, int position) {
           super.onBindViewHolder(vh, position);

           if (vh instanceof RadioButtonListItem.ViewHolder) {
               ((RadioButtonListItem.ViewHolder) vh).getRadioButton().setChecked(mController.isChecked(position));
           }
        }

        @Override
        public long getItemId(int position) {
            // This requires items not changing position after adapter is initialized (or really,
            // after first time this method is called).
            return position;
        }
    }

    private static class MyDetailsLookup extends ItemDetailsLookup {

        private final RecyclerView mRecyclerView;

        public MyDetailsLookup(RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails getItemDetails(@NonNull MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                Log.i("yaoyx", "Looking up item detail for position " + holder.getAdapterPosition());
                if (holder instanceof ListItem.ViewBinder) {
                    return new ItemDetails() {
                        @Override
                        public int getPosition() {
                            return holder.getAdapterPosition();
                        }

                        @Nullable
                        @Override
                        public Object getSelectionKey() {
                            return holder.getItemId();
                        }
                    };
                }
            }
            return null;
        }
    }

    private static class SampleProvider extends ListItemProvider {

        private Context mContext;

        private ListItemProvider.ListProvider mListProvider;

        SampleProvider(Context context) {
            mContext = context;

            List<ListItem> items = new ArrayList<>();
            mListProvider = new ListItemProvider.ListProvider(items);
        }

        @Override
        public ListItem get(int position) {
            return mListProvider.get(position);
        }

        @Override
        public int size() {
            return mListProvider.size();
        }
    }
}
