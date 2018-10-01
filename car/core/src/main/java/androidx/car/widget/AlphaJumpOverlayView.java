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

package androidx.car.widget;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.R;
import androidx.gridlayout.widget.GridLayout;

import java.util.List;

/**
 * This view shows a grid of alphabetic letters that you can tap on to advance a list to the
 * beginning of that list.
 */
public class AlphaJumpOverlayView extends GridLayout {
    private AlphaJumpAdapter mAdapter;
    private PagedListView mPagedListView;
    private List<AlphaJumpBucket> mBuckets;
    private LayoutInflater mInflater;
    private Animation mOpenAnimation;
    private Animation mCloseAnimation;

    public AlphaJumpOverlayView(@NonNull Context context) {
        super(context);
        setOrientation(HORIZONTAL);
    }

    void init(PagedListView plv, AlphaJumpAdapter adapter) {
        Context context = getContext();
        mInflater = LayoutInflater.from(context);
        mPagedListView = plv;
        mAdapter = adapter;
        mBuckets = adapter.getAlphaJumpBuckets();

        setBackgroundResource(R.color.car_card);

        mOpenAnimation = AnimationUtils
                .loadAnimation(getContext(), R.anim.car_alpha_picker_enter);
        mCloseAnimation = AnimationUtils
                .loadAnimation(getContext(), R.anim.car_alpha_picker_exit);

        setupGrid(context);
    }

    private void setupGrid(Context context) {
        Resources res = context.getResources();

        //Subtract the buttons' margin from the side margin.
        float carMargin = res.getDimension(R.dimen.car_margin);
        float buttonMargin = res.getDimension(R.dimen.car_padding_0);
        int padding = (int) (carMargin - buttonMargin);
        setPadding(padding, 0, padding, 0);

        createButtons();
    }

    private void createButtons() {
        removeAllViews();
        for (AlphaJumpBucket bucket : mBuckets) {
            View container = mInflater
                    .inflate(R.layout.car_alpha_jump_picker_button, this, false);
            TextView btn = container.findViewById(R.id.button);
            btn.setText(bucket.getLabel());
            btn.setOnClickListener(this::onButtonClick);
            btn.setTag(bucket);
            if (bucket.isEmpty()) {
                btn.setEnabled(false);
                btn.setBackgroundResource(R.color.alpha_picker_unavailable_bg);
            }
            addView(container);
        }
    }

    private void onButtonClick(View v) {
        close();
        AlphaJumpBucket bucket = (AlphaJumpBucket) v.getTag();
        if (bucket != null) {
            mAdapter.onAlphaJumpLeave(bucket);

            mPagedListView.snapToPosition(bucket.getIndex());
        }
    }

    /**
     * Open the Alpha Jump Overlay using an animation
     */
    public void open() {
        mAdapter.onAlphaJumpEnter();
        setVisibility(View.VISIBLE);
        startAnimation(mOpenAnimation);
    }

    /**
     * Close the Alpha Jump Overlay using an animation
     */
    public void close() {
        startAnimation(mCloseAnimation);
        setVisibility(View.GONE);
    }
}
