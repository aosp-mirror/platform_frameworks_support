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

package com.example.android.supportv7.widget;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;

/**
 * Just an app to repro a bug.
 */
public class MainActivity extends AppCompatActivity {

    private static final int[] PAGE_SEQUENCE = {2, 1};
    private static final int NUM_PAGES = 3;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private int mTargetPage = -1;
    private int mNextPageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        mRecyclerView = findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(new MyAdapter());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (mTargetPage == mLayoutManager.findFirstCompletelyVisibleItemPosition()) {
                    Log.i("TEST", String.format("Arrived at page %d", mTargetPage));
                    targetNext();
                }
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doit();
            }
        });
    }

    private void doit() {
        mNextPageIndex = 0;
        targetNext();
    }

    private void targetNext() {
        if (mNextPageIndex >= PAGE_SEQUENCE.length) {
            Log.i("TEST", "Done, no bug found!");
            return;
        }
        mRecyclerView.smoothScrollToPosition(mTargetPage = PAGE_SEQUENCE[mNextPageIndex++]);
        Log.i("TEST", String.format("Kicked off smooth scroll to page %d", mTargetPage));
    }

    private static class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.item_page, parent, false);
            return new RecyclerView.ViewHolder(itemView) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            textView.setText(String.format("Page %d", position));
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}
