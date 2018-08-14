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

package androidx.viewpager2;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

/**
 * TODO: layout layer (HW vs SW)
 * TODO: drawing order
 */
public class PageTransformAdapter {
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;

    private final LinearLayoutManager mLayoutManager;

    private ViewPager2.PageTransformer mPageTransformer;
    private int mDrawingOrder = DRAW_ORDER_FORWARD;

    public PageTransformAdapter(LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * Sets the PageTransformer. The only allowed value for {@code reverseDrawingOrder} is currently
     * {@code true}. The page transformer will be called for each attached page whenever the scroll
     * position is changed.
     *
     * @param reverseDrawingOrder Whether to draw the pages in reverse order. If {@code false}, will
     *                           draw the pages in forward order, otherwise in backward order.
     *                           Currently only forward order is supported.
     * @param transformer The PageTransformer
     */
    public void setPageTransformer(boolean reverseDrawingOrder,
            @Nullable ViewPager2.PageTransformer transformer) {
        mPageTransformer = transformer;
        mDrawingOrder = reverseDrawingOrder ? DRAW_ORDER_REVERSE : DRAW_ORDER_FORWARD;
        if (reverseDrawingOrder) {
            throw new UnsupportedOperationException("Reverse drawing order not yet supported");
        }
    }

    void onPageScrolled(int position, float positionOffset) {
        if (mPageTransformer == null) {
            return;
        }

        float transformOffset = -positionOffset;
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View view = mLayoutManager.getChildAt(i);
            if (view == null) {
                continue;
            }
            int currPos = mLayoutManager.getPosition(view);
            float viewOffset = transformOffset + (currPos - position);
            mPageTransformer.transformPage(view, viewOffset);
        }
    }
}
