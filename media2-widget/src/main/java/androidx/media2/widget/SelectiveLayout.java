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

package androidx.media2.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;

class SelectiveLayout extends ViewGroup {
    SelectiveLayout(@NonNull Context context) {
        super(context);
    }

    SelectiveLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    SelectiveLayout(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(21)
    SelectiveLayout(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return lp;
        }
        return new LayoutParams(lp);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        // Measure its children of which doNotMeasure is false
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child.getVisibility() != View.GONE && !lp.doNotMeasure) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = childState | child.getMeasuredState();
            }
        }

        // Account for padding too
        maxWidth += getPositivePaddingLeft() + getPositivePaddingRight();
        maxHeight += getPositivePaddingTop() + getPositivePaddingBottom();

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        if (Build.VERSION.SDK_INT >= 23) {
            // Check against our foreground's minimum height and width
            final Drawable drawable = getForeground();
            if (drawable != null) {
                maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
                maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
            }
        }

        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << View.MEASURED_HEIGHT_STATE_SHIFT));

        // Measure its children of which doNotMeasure is true
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child.getVisibility() != View.GONE && lp.doNotMeasure) {
                child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();

        final int parentLeft = getPositivePaddingLeft();
        final int parentRight = right - left - getPositivePaddingRight();

        final int parentTop = getPositivePaddingTop();
        final int parentBottom = bottom - top - getPositivePaddingBottom();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                final int childLeft = parentLeft + (parentRight - parentLeft - width) / 2;
                final int childTop = parentTop + (parentBottom - parentTop - height) / 2;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private int getPositivePaddingLeft() {
        return Math.max(getPaddingLeft(), 0);
    }

    private int getPositivePaddingRight() {
        return Math.max(getPaddingRight(), 0);
    }

    private int getPositivePaddingTop() {
        return Math.max(getPaddingTop(), 0);
    }

    private int getPositivePaddingBottom() {
        return Math.max(getPaddingBottom(), 0);
    }

    static class LayoutParams extends ViewGroup.LayoutParams {
        public boolean doNotMeasure;

        LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        LayoutParams(int width, int height) {
            super(width, height);
        }

        LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        LayoutParams(boolean doNotMeasure) {
            super(MATCH_PARENT, MATCH_PARENT);
            this.doNotMeasure = doNotMeasure;
        }
    }
}
