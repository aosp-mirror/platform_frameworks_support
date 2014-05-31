/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

/**
 * PagerViewStrip is an interactive indicator of the current, next,
 * and previous pages of a {@link ViewPager}. It is intended to be used as a
 * child view of a ViewPager widget in your XML layout.
 * Add it as a child of a ViewPager in your layout file and set its
 * android:layout_gravity to TOP or BOTTOM to pin it to the top or bottom
 * of the ViewPager. The view from each page is supplied by the method
 * {@link PageViewTitleAdapter#getTitleView(int)} that your adapter supplied to
 * the ViewPager must implement.
 */
public class PagerViewStrip extends ViewGroup implements ViewPager.Decor {
    private static final String TAG = "PagerViewStrip";

    private ViewPager mPager;
    private FrameLayout mPreviousView;
    private FrameLayout mCurrentView;
    private FrameLayout mNextView;

    private int mLastKnownCurrentPage = -1;
    private float mLastKnownPositionOffset = -1;
    private int mScaledSpacing; // spacing between titles
    private int mGravity;

    private boolean mUpdatingViews;
    private boolean mUpdatingPositions;

    private WeakReference<PagerAdapter> mWatchingAdapter;

    private final PageListener mPageListener = new PageListener();

    /**
     * Default Attributes
     */
    private static final int[] ATTRS = new int[]{
            android.R.attr.gravity
    };

    /**
     * Default Spacing between Views
     */
    private static final int SPACING = 16; // dip

    /**
     * Value that the views fade out to on the edged
     */
    private float mMinAlphaValue = 0.5f;
    private float mCurrentViewAlpha;

    interface PagerViewStripImpl {
        void setAlpha(View view, float alpha);
    }

    static class PagerViewStripImplBase implements PagerViewStripImpl {
        public void setAlpha(View view, float alpha) {
            final AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
            animation.setDuration(0);
            animation.setFillAfter(true);
            view.startAnimation(animation);
        }
    }

    static class PagerViewStripImplHoneyHomb implements PagerViewStripImpl {
        @Override
        public void setAlpha(View view, float alpha) {
            view.setAlpha(alpha);
        }
    }

    private static final PagerViewStripImpl IMPL;
    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            IMPL = new PagerViewStripImplBase();
        } else {
            IMPL = new PagerViewStripImplHoneyHomb();
        }
    }

    public PagerViewStrip(Context context) {
        this(context, null);
    }

    public PagerViewStrip(Context context, AttributeSet attrs) {
        super(context, attrs);

        addView(mPreviousView = new FrameLayout(context));
        addView(mCurrentView = new FrameLayout(context));
        addView(mNextView = new FrameLayout(context));

        IMPL.setAlpha(mPreviousView, mMinAlphaValue);
        IMPL.setAlpha(mNextView, mMinAlphaValue);

        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);

        mGravity = a.getInteger(0, Gravity.BOTTOM);
        a.recycle();

        final float density = context.getResources().getDisplayMetrics().density;
        mScaledSpacing = (int) (SPACING * density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final ViewParent parent = getParent();
        if (!(parent instanceof ViewPager)) {
            throw new IllegalStateException(
                    "PagerViewTitleStrip must be a direct child of a ViewPager.");
        }

        final ViewPager pager = (ViewPager) parent;
        final PagerAdapter adapter = pager.getAdapter();

        pager.setInternalPageChangeListener(mPageListener);
        pager.setOnAdapterChangeListener(mPageListener);
        this.mPager = pager;
        updateAdapter(mWatchingAdapter != null ? mWatchingAdapter.get() : null, adapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPager != null) {
            updateAdapter(mPager.getAdapter(), null);
            mPager.setInternalPageChangeListener(null);
            mPager.setOnAdapterChangeListener(null);
            mPager = null;
        }
    }

    @Override
    public void requestLayout() {
        if (!mUpdatingViews) {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != View.MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Must measure with an exact width");
        }

        int childHeight = heightSize;
        int minHeight = getMinHeight();
        int padding = getPaddingTop() + getPaddingBottom();
        childHeight -= padding;

        final int childWidthSpec = View.MeasureSpec
                .makeMeasureSpec((int) (widthSize * 0.8f), View.MeasureSpec.AT_MOST);
        final int childHeightSpec = View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.AT_MOST);

        mPreviousView.measure(childWidthSpec, childHeightSpec);
        mCurrentView.measure(childWidthSpec, childHeightSpec);
        mNextView.measure(childWidthSpec, childHeightSpec);

        if (heightMode == View.MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize);
        } else {
            int textHeight = mCurrentView.getMeasuredHeight();
            setMeasuredDimension(widthSize, Math.max(minHeight, textHeight + padding));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mPager != null) {
            final float offset = mLastKnownPositionOffset >= 0 ? mLastKnownPositionOffset : 0;
            updateViewPosition(mLastKnownCurrentPage, offset, true);
        }
    }

    /**
     * Updates the view views. Generally called when the adapter changes or a current view switches
     * to be a previous view or the next view.
     *
     * @param currentItem the index into the current item
     * @param adapter     the PagerAdapter that is controlling the view mPager
     */
    protected void updateView(int currentItem, PagerAdapter adapter) {
        final int itemCount = adapter != null ? adapter.getCount() : 0;
        mUpdatingViews = true;

        PageViewTitleAdapter titleAdapter;
        try {
            titleAdapter = (PageViewTitleAdapter) adapter;
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "Adapter used with PagerViewTitleStrip MUST implement PageViewImageTitleAdapter");
        }

        mPreviousView.removeAllViews();
        mCurrentView.removeAllViews();
        mNextView.removeAllViews();

        View view = null;
        if (currentItem >= 1 && adapter != null) {
            view = titleAdapter.getTitleView(currentItem - 1);

        }
        if (view != null) {
            mPreviousView.addView(view);
        }

        mCurrentView.removeAllViews();
        view = currentItem < itemCount && adapter != null ? titleAdapter.getTitleView(currentItem) : null;
        if (view != null) {
            mCurrentView.addView(view);
        }

        view = null;
        if (currentItem + 1 < itemCount && adapter != null) {
            view = titleAdapter.getTitleView(currentItem + 1);
        }
        if (view != null) {
            mNextView.addView(view);
        }

        // Measure everything
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int childHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        final int childWidthSpec = View.MeasureSpec
                .makeMeasureSpec((int) (width * 0.8f), View.MeasureSpec.AT_MOST);
        final int childHeightSpec = View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.AT_MOST);

        mPreviousView.measure(childWidthSpec, childHeightSpec);
        mCurrentView.measure(childWidthSpec, childHeightSpec);
        mNextView.measure(childWidthSpec, childHeightSpec);

        mLastKnownCurrentPage = currentItem;

        if (!mUpdatingPositions) {
            updateViewPosition(currentItem, mLastKnownPositionOffset, false);
        }

        mUpdatingViews = false;
    }

    /**
     * Called to update the position of the views of this title strip.
     *
     * @param position       Position index of the first page currently being displayed.
     *                       Page position+1 will be visible if positionOffset is nonzero.
     * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
     * @param force          if true force an update of the view positions, false don't update unless
     *                       the current position is not the same as the last known current position
     */
    protected void updateViewPosition(int position, float positionOffset, boolean force) {
        if (position != mLastKnownCurrentPage) {
            updateView(position, mPager.getAdapter());
        } else if (!force && positionOffset == mLastKnownPositionOffset) {
            return;
        }

        mUpdatingPositions = true;

        final int prevWidth = mPreviousView.getMeasuredWidth();
        final int currWidth = mCurrentView.getMeasuredWidth();
        final int nextWidth = mNextView.getMeasuredWidth();
        final int halfCurrWidth = currWidth / 2;

        final int stripWidth = getWidth();
        final int stripHeight = getHeight();
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int textPaddedLeft = paddingLeft + halfCurrWidth;
        final int textPaddedRight = paddingRight + halfCurrWidth;
        final int contentWidth = stripWidth - textPaddedLeft - textPaddedRight;

        float currOffset = positionOffset + 0.5f;
        if (currOffset > 1.f) {
            currOffset -= 1.f;
        }
        final int currCenter = stripWidth - textPaddedRight - (int) (contentWidth * currOffset);
        final int currLeft = currCenter - currWidth / 2;
        final int currRight = currLeft + currWidth;

        final int prevBaseline = mPreviousView.getBaseline();
        final int currBaseline = mCurrentView.getBaseline();
        final int nextBaseline = mNextView.getBaseline();
        final int maxBaseline = Math.max(Math.max(prevBaseline, currBaseline), nextBaseline);
        final int prevTopOffset = maxBaseline - prevBaseline;
        final int currTopOffset = maxBaseline - currBaseline;
        final int nextTopOffset = maxBaseline - nextBaseline;
        final int alignedPrevHeight = prevTopOffset + mPreviousView.getMeasuredHeight();
        final int alignedCurrHeight = currTopOffset + mCurrentView.getMeasuredHeight();
        final int alignedNextHeight = nextTopOffset + mNextView.getMeasuredHeight();
        final int maxTextHeight = Math.max(Math.max(alignedPrevHeight, alignedCurrHeight),
                alignedNextHeight);

        final int vgrav = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        int prevTop;
        int currTop;
        int nextTop;
        switch (vgrav) {
            default:
            case Gravity.TOP:
                prevTop = paddingTop + prevTopOffset;
                currTop = paddingTop + currTopOffset;
                nextTop = paddingTop + nextTopOffset;
                break;
            case Gravity.CENTER_VERTICAL:
                final int paddedHeight = stripHeight - paddingTop - paddingBottom;
                final int centeredTop = (paddedHeight - maxTextHeight) / 2;
                prevTop = centeredTop + prevTopOffset;
                currTop = centeredTop + currTopOffset;
                nextTop = centeredTop + nextTopOffset;
                break;
            case Gravity.BOTTOM:
                final int bottomGravTop = stripHeight - paddingBottom - maxTextHeight;
                prevTop = bottomGravTop + prevTopOffset;
                currTop = bottomGravTop + currTopOffset;
                nextTop = bottomGravTop + nextTopOffset;
                break;
        }

        mCurrentView.layout(currLeft, currTop, currRight,
                currTop + mCurrentView.getMeasuredHeight());

        final int prevLeft = Math.min(paddingLeft, currLeft - mScaledSpacing - prevWidth);
        mPreviousView.layout(prevLeft, prevTop, prevLeft + prevWidth,
                prevTop + mPreviousView.getMeasuredHeight());
        float alpha = Math.abs(positionOffset - 0.5f) * 2;
        mCurrentViewAlpha = alpha < mMinAlphaValue ? mMinAlphaValue : alpha;
        IMPL.setAlpha(mCurrentView, mCurrentViewAlpha);
        final int nextLeft = Math.max(stripWidth - paddingRight - nextWidth,
                currRight + mScaledSpacing);
        mNextView.layout(nextLeft, nextTop, nextLeft + nextWidth,
                nextTop + mNextView.getMeasuredHeight());

        mLastKnownPositionOffset = positionOffset;
        mUpdatingPositions = false;
    }

    protected int getMinHeight() {
        int minHeight = 0;
        final Drawable bg = getBackground();
        if (bg != null) {
            minHeight = bg.getIntrinsicHeight();
        }
        return minHeight;
    }

    /**
     * Set the {@link android.view.Gravity} used to position text within the title strip.
     * Only the vertical mGravity component is used.
     *
     * @param mGravity {@link android.view.Gravity} constant for positioning title text
     */
    public void setmGravity(int mGravity) {
        this.mGravity = mGravity;
        requestLayout();
    }

    /**
     * Set the required spacing between title segments.
     *
     * @param spacingPixels Spacing between each title displayed in pixels
     */
    public void setSpacing(int spacingPixels) {
        mScaledSpacing = spacingPixels;
        requestLayout();
    }

    /**
     * @return The required spacing between title segments in pixels
     */
    public int getSpacing() {
        return mScaledSpacing;
    }

    protected FrameLayout getPreviousView() {
        return mPreviousView;
    }

    protected FrameLayout getCurrentView() {
        return mCurrentView;
    }

    protected FrameLayout getNextView() {
        return mNextView;
    }

    protected ViewPager getPager() {
        return mPager;
    }

    /**
     * Get the calculated alpha for the current view
     */
    protected float getCurrentViewAlpha() {
        return mCurrentViewAlpha;
    }

    private void updateAdapter(PagerAdapter oldAdapter, PagerAdapter newAdapter) {
        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(mPageListener);
            mWatchingAdapter = null;
        }
        if (newAdapter != null) {
            newAdapter.registerDataSetObserver(mPageListener);
            mWatchingAdapter = new WeakReference<PagerAdapter>(newAdapter);
        }
        if (mPager != null) {
            mLastKnownCurrentPage = -1;
            mLastKnownPositionOffset = -1;
            updateView(mPager.getCurrentItem(), newAdapter);
            requestLayout();
        }
    }

    /**
     * Interface the PageAdapter must implement in order to use PagerViewTitleStrip
     */
    public interface PageViewTitleAdapter {

        public View getTitleView(int position);
    }

    /**
     * Internal Listener for the PageView telling the PageViewTitleStrip when to update
     */
    private class PageListener extends DataSetObserver implements ViewPager.OnPageChangeListener,
            ViewPager.OnAdapterChangeListener {

        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (positionOffset > 0.5f) {
                // Consider ourselves to be on the next page when we're 50% of the way there.
                position++;
            }
            updateViewPosition(position, positionOffset, false);
        }

        @Override
        public void onPageSelected(int position) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                // Only update the text here if we're not dragging or settling.
                //updateText(mPager.getCurrentItem(), mPager.getAdapter());

                final float offset = mLastKnownPositionOffset >= 0 ? mLastKnownPositionOffset : 0;
                updateViewPosition(mPager.getCurrentItem(), offset, true);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;
        }

        @Override
        public void onAdapterChanged(PagerAdapter oldAdapter, PagerAdapter newAdapter) {
            updateAdapter(oldAdapter, newAdapter);
        }

        @Override
        public void onChanged() {
            updateView(mPager.getCurrentItem(), mPager.getAdapter());

            final float offset = mLastKnownPositionOffset >= 0 ? mLastKnownPositionOffset : 0;
            updateViewPosition(mPager.getCurrentItem(), offset, true);
        }
    }
}

