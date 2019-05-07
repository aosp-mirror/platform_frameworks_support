/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.viewpager2.widget;

import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Adds space between pages via the {@link ViewPager2.PageTransformer} API.
 * <p>
 * Internally relies on {@link View#setTranslationX} and {@link View#setTranslationY}.
 * <p>
 * Note: translations on pages are not reset when this adapter is changed for another one, so you
 * might want to set them manually to 0 when dynamically switching to another transformer, or
 * when switching ViewPager2 orientation.
 *
 * @see ViewPager2#setPageTransformer
 * @see CompositePageTransformer
 */
public final class MarginPageTransformer implements ViewPager2.PageTransformer {
    private final ViewPager2 mViewPager;
    private int mMarginPx = 0;

    /**
     * Creates a {@link MarginPageTransformer}.
     * <p>
     * Margin values are to be set via {@link #setMarginDp} or {@link #setMarginPx}.
     */
    public MarginPageTransformer(ViewPager2 viewPager) {
        mViewPager = viewPager;
    }

    /** Sets the margin in pixels. */
    public void setMarginPx(int marginPx) {
        mMarginPx = marginPx;
    }

    /** Sets the margin in dp (dip) units. */
    public void setMarginDp(int marginDp) {
        mMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginDp,
                mViewPager.getResources().getDisplayMetrics());
    }

    @Override
    public void transformPage(@NonNull View page, float position) {
        float offset = mMarginPx * position;

        if (mViewPager.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL) {
            page.setTranslationX(mViewPager.isLayoutRtl() ? -offset : offset);
        } else {
            page.setTranslationY(offset);
        }
    }
}
