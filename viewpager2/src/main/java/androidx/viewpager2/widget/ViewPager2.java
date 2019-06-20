/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.lang.annotation.RetentionPolicy.CLASS;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.viewpager2.CompositeOnPageChangeListener;
import androidx.viewpager2.PageTransformerAdapter;
import androidx.viewpager2.R;
import androidx.viewpager2.ScrollEventAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.lang.annotation.Retention;

/**
 * Work in progress: go/viewpager2
 *
 * @hide
 */
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
@RestrictTo(LIBRARY_GROUP)
public class ViewPager2 extends ViewGroup {
=======
public final class ViewPager2 extends ViewGroup {
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(SOURCE)
    @IntDef({ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL})
    public @interface Orientation {
    }

    public static final int ORIENTATION_HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int ORIENTATION_VERTICAL = RecyclerView.VERTICAL;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(SOURCE)
    @IntDef({SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING})
    public @interface ScrollState {
    }

    /** @hide */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(SOURCE)
    @IntDef({OFFSCREEN_PAGE_LIMIT_DEFAULT})
    @IntRange(from = 1)
    public @interface OffscreenPageLimit {
    }

    /**
     * Indicates that the ViewPager2 is in an idle, settled state. The current page
     * is fully in view and no animation is in progress.
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * Indicates that the ViewPager2 is currently being dragged by the user, or programmatically
     * via fake drag functionality.
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * Indicates that the ViewPager2 is in the process of settling to a final position.
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    /**
     * Value to indicate that the default caching mechanism of RecyclerView should be used instead
     * of explicitly prefetch and retain pages to either side of the current page.
     * @see #setOffscreenPageLimit(int)
     */
    public static final int OFFSCREEN_PAGE_LIMIT_DEFAULT = -1;

    /** Feature flag while stabilizing enhanced a11y */
    static boolean sFeatureEnhancedA11yEnabled = false;

>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    // reused in layout(...)
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();

    private CompositeOnPageChangeListener mExternalPageChangeListeners =
            new CompositeOnPageChangeListener(3);

    RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ScrollEventAdapter mScrollEventAdapter;
    private PageTransformerAdapter mPageTransformerAdapter;
    int mCurrentItem;

    public ViewPager2(Context context) {
        super(context);
        initialize(context, null);
    }

    public ViewPager2(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public ViewPager2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    @RequiresApi(21)
    public ViewPager2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // TODO(b/70663531): handle attrs, defStyleAttr, defStyleRes
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setId(ViewCompat.generateViewId());

        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        setOrientation(context, attrs);

        mRecyclerView.setLayoutParams(
                new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mRecyclerView.addOnChildAttachStateChangeListener(enforceChildFillListener());
        new PagerSnapHelper().attachToRecyclerView(mRecyclerView);

        mScrollEventAdapter = new ScrollEventAdapter(mLayoutManager);
        mRecyclerView.addOnScrollListener(mScrollEventAdapter);

        CompositeOnPageChangeListener dispatcher = new CompositeOnPageChangeListener(3);
        mScrollEventAdapter.setOnPageChangeListener(dispatcher);

<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        // Add mOnPageChangeListener before mExternalPageChangeListeners, because we need to update
=======
        // Callback that updates mCurrentItem after swipes. Also triggered in other cases, but in
        // all those cases mCurrentItem will only be overwritten with the same value.
        final OnPageChangeCallback currentItemUpdater = new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (mCurrentItem != position) {
                    mCurrentItem = position;
                    mAccessibilityProvider.onSetNewCurrentItem();
                }
            }
        };

        // Add currentItemUpdater before mExternalPageChangeCallbacks, because we need to update
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        // internal state first
        dispatcher.addOnPageChangeListener(mOnPageChangeListener);
        dispatcher.addOnPageChangeListener(mExternalPageChangeListeners);

        // Add mPageTransformerAdapter after mExternalPageChangeListeners, because page transform
        // events must be fired after scroll events
        mPageTransformerAdapter = new PageTransformerAdapter(mLayoutManager);
        dispatcher.addOnPageChangeListener(mPageTransformerAdapter);

        attachViewToParent(mRecyclerView, 0, mRecyclerView.getLayoutParams());
    }

    /**
     * A lot of places in code rely on an assumption that the page fills the whole ViewPager2.
     *
     * TODO(b/70666617) Allow page width different than width/height 100%/100%
     * TODO(b/70666614) Revisit the way we enforce width/height restriction of 100%/100%
     */
    private RecyclerView.OnChildAttachStateChangeListener enforceChildFillListener() {
        return new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                RecyclerView.LayoutParams layoutParams =
                        (RecyclerView.LayoutParams) view.getLayoutParams();
                if (layoutParams.width != LayoutParams.MATCH_PARENT
                        || layoutParams.height != LayoutParams.MATCH_PARENT) {
                    throw new IllegalStateException(
                            "Pages must fill the whole ViewPager2 (use match_parent)");
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // nothing
            }
        };
    }

    private void setOrientation(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewPager2);
        try {
            setOrientation(
                    a.getInt(R.styleable.ViewPager2_android_orientation, Orientation.HORIZONTAL));
        } finally {
            a.recycle();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.mRecyclerViewId = mRecyclerView.getId();
        ss.mOrientation = getOrientation();
        ss.mCurrentItem = mCurrentItem;
        ss.mScrollInProgress =
                mLayoutManager.findFirstCompletelyVisibleItemPosition() != mCurrentItem;

<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        Adapter adapter = mRecyclerView.getAdapter();
        if (adapter instanceof FragmentStateAdapter) {
            ss.mAdapterState = ((FragmentStateAdapter) adapter).saveState();
=======
        if (mPendingAdapterState != null) {
            ss.mAdapterState = mPendingAdapterState;
        } else {
            Adapter<?> adapter = mRecyclerView.getAdapter();
            if (adapter instanceof StatefulAdapter) {
                ss.mAdapterState = ((StatefulAdapter) adapter).saveState();
            }
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        }

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setOrientation(ss.mOrientation);
        mCurrentItem = ss.mCurrentItem;
        if (ss.mScrollInProgress) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.scrollToPosition(mCurrentItem);
                }
            });
        }

<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        if (ss.mAdapterState != null) {
            Adapter adapter = mRecyclerView.getAdapter();
            if (adapter instanceof FragmentStateAdapter) {
                ((FragmentStateAdapter) adapter).restoreState(ss.mAdapterState);
=======
    private void restorePendingState() {
        if (mPendingCurrentItem == NO_POSITION) {
            // No state to restore, or state is already restored
            return;
        }
        Adapter<?> adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (mPendingAdapterState != null) {
            if (adapter instanceof StatefulAdapter) {
                ((StatefulAdapter) adapter).restoreState(mPendingAdapterState);
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
            }
        }
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // RecyclerView changed an id, so we need to reflect that in the saved state
        Parcelable state = container.get(getId());
        if (state instanceof SavedState) {
            final int previousRvId = ((SavedState) state).mRecyclerViewId;
            final int currentRvId = mRecyclerView.getId();
            container.put(currentRvId, container.get(previousRvId));
            container.remove(previousRvId);
        }

        super.dispatchRestoreInstanceState(container);
    }

    static class SavedState extends BaseSavedState {
        int mRecyclerViewId;
        @Orientation int mOrientation;
        int mCurrentItem;
        boolean mScrollInProgress;
        Parcelable[] mAdapterState;

        @RequiresApi(24)
        SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            readValues(source, loader);
        }

        SavedState(Parcel source) {
            super(source);
            readValues(source, null);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        private void readValues(Parcel source, ClassLoader loader) {
            mRecyclerViewId = source.readInt();
            mOrientation = source.readInt();
            mCurrentItem = source.readInt();
            mScrollInProgress = source.readByte() != 0;
            mAdapterState = source.readParcelableArray(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mRecyclerViewId);
            out.writeInt(mOrientation);
            out.writeInt(mCurrentItem);
            out.writeByte((byte) (mScrollInProgress ? 1 : 0));
            out.writeParcelableArray(mAdapterState, flags);
        }

        static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source, ClassLoader loader) {
                return Build.VERSION.SDK_INT >= 24
                        ? new SavedState(source, loader)
                        : new SavedState(source);
            }

            @Override
            public SavedState createFromParcel(Parcel source) {
                return createFromParcel(source, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * TODO(b/70663708): decide on an Adapter class. Here supporting subclasses of {@link Adapter}.
     *
     * @see androidx.viewpager2.adapter.FragmentStateAdapter
     * @see RecyclerView#setAdapter(Adapter)
     */
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
    public void setAdapter(Adapter adapter) {
=======
    public void setAdapter(@Nullable @SuppressWarnings("rawtypes") Adapter adapter) {
        mAccessibilityProvider.onDetachAdapter(mRecyclerView.getAdapter());
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        mRecyclerView.setAdapter(adapter);
    }

<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
    public Adapter getAdapter() {
=======
    @SuppressWarnings("rawtypes")
    public @Nullable Adapter getAdapter() {
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        return mRecyclerView.getAdapter();
    }

    @Override
    public void onViewAdded(View child) {
        // TODO(b/70666620): consider adding a support for Decor views
        throw new IllegalStateException(
                getClass().getSimpleName() + " does not support direct child views");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO(b/70666622): consider margin support
        // TODO(b/70666626): consider delegating all this to RecyclerView
        // TODO(b/70666625): write automated tests for this

        measureChild(mRecyclerView, widthMeasureSpec, heightMeasureSpec);
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();
        int childState = mRecyclerView.getMeasuredState();

        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState),
                resolveSizeAndState(height, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();

        // TODO(b/70666626): consider delegating padding handling to the RecyclerView to avoid
        // an unnatural page transition effect: http://shortn/_Vnug3yZpQT
        mTmpContainerRect.left = getPaddingLeft();
        mTmpContainerRect.right = r - l - getPaddingRight();
        mTmpContainerRect.top = getPaddingTop();
        mTmpContainerRect.bottom = b - t - getPaddingBottom();

        Gravity.apply(Gravity.TOP | Gravity.START, width, height, mTmpContainerRect, mTmpChildRect);
        mRecyclerView.layout(mTmpChildRect.left, mTmpChildRect.top, mTmpChildRect.right,
                mTmpChildRect.bottom);
    }

    @Retention(CLASS)
    @IntDef({Orientation.HORIZONTAL, Orientation.VERTICAL})
    public @interface Orientation {
        int HORIZONTAL = RecyclerView.HORIZONTAL;
        int VERTICAL = RecyclerView.VERTICAL;
    }

    /**
     * @param orientation @{link {@link ViewPager2.Orientation}}
     */
    public void setOrientation(@Orientation int orientation) {
        mLayoutManager.setOrientation(orientation);
    }

    public @Orientation int getOrientation() {
        return mLayoutManager.getOrientation();
    }

    /**
     * Set the currently selected page.
     *
     * @param item Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
=======

        // 1. Preprocessing (check state, validate item, decide if update is necessary, etc)

        if (isFakeDragging()) {
            throw new IllegalStateException("Cannot change current item when ViewPager2 is fake "
                    + "dragging");
        }
        Adapter<?> adapter = getAdapter();
        if (adapter == null) {
            // Update the pending current item if we're still waiting for the adapter
            if (mPendingCurrentItem != NO_POSITION) {
                mPendingCurrentItem = Math.max(item, 0);
            }
            return;
        }
        if (adapter.getItemCount() <= 0) {
            // Adapter is empty
            return;
        }
        item = Math.max(item, 0);
        item = Math.min(item, adapter.getItemCount() - 1);

        if (item == mCurrentItem && mScrollEventAdapter.isIdle()) {
            // Already at the correct page
            return;
        }
        if (item == mCurrentItem && smoothScroll) {
            // Already scrolling to the correct page, but not yet there. Only handle instant scrolls
            // because then we need to interrupt the current smooth scroll.
            return;
        }

        // 2. Update the item internally

>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        float previousItem = mCurrentItem;
        if (previousItem == item) {
            return;
        }
        mCurrentItem = item;

        if (!mScrollEventAdapter.isIdle()) {
            // Scroll in progress, overwrite previousItem with actual current position
            previousItem = mScrollEventAdapter.getRelativeScrollPosition();
        }

        mScrollEventAdapter.notifyProgrammaticScroll(item, smoothScroll);
        if (!smoothScroll) {
            mRecyclerView.scrollToPosition(item);
            return;
        }

        // For smooth scroll, pre-jump to nearby item for long jumps.
        if (Math.abs(item - previousItem) > 3) {
            mRecyclerView.scrollToPosition(item > previousItem ? item - 3 : item + 3);
            // TODO(b/114361680): call smoothScrollToPosition synchronously (blocked by b/114019007)
            mRecyclerView.post(new SmoothScrollToPosition(item));
        } else {
            mRecyclerView.smoothScrollToPosition(item);
        }
    }

    /**
     * @return Currently selected page.
     */
    public int getCurrentItem() {
        return mCurrentItem;
    }

    /**
     * Add a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link OnPageChangeListener}.
     *
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearOnPageChangeListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        mExternalPageChangeListeners.addOnPageChangeListener(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnPageChangeListener(OnPageChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        mExternalPageChangeListeners.removeOnPageChangeListener(listener);
    }

    /**
     * Remove all listeners that are notified of any changes in scroll state or position.
     */
    public void clearOnPageChangeListeners() {
        mExternalPageChangeListeners.clearOnPageChangeListeners();
    }

    /**
     * Sets a {@link androidx.viewpager.widget.ViewPager.PageTransformer} that will be called for
     * each attached page whenever the scroll position is changed. This allows the application to
     * apply custom property transformations to each page, overriding the default sliding behavior.
     *
     * @param transformer PageTransformer that will modify each page's animation properties
     */
    public void setPageTransformer(@Nullable PageTransformer transformer) {
        // TODO: add support for reverseDrawingOrder: b/112892792
        // TODO: add support for pageLayerType: b/112893074
        mPageTransformerAdapter.setPageTransformer(transformer);
    }

    /**
     * Listener that updates mCurrentItem after swipes. Will of course also update it in all other
     * cases, but we already know about those updates (as we triggered those ourselves).
     */
    private final OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mCurrentItem = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private class SmoothScrollToPosition implements Runnable {
        private final int mPosition;

        SmoothScrollToPosition(int position) {
            mPosition = position;
        }

        @Override
        public void run() {
            mRecyclerView.smoothScrollToPosition(mPosition);
        }
    }

    @Retention(CLASS)
    @IntDef({ScrollState.IDLE, ScrollState.DRAGGING, ScrollState.SETTLING})
    public @interface ScrollState {
        int IDLE = 0;
        int DRAGGING = 1;
        int SETTLING = 2;
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public interface OnPageChangeListener {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position Position index of the first page currently being displayed.
         *                 Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels);

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        void onPageSelected(int position);

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         */
        void onPageScrollStateChanged(@ScrollState int state);
    }

    /**
     * A PageTransformer is invoked whenever a visible/attached page is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the page views using animation properties.
     *
     * <p>As property animation is only supported as of Android 3.0 and forward,
     * setting a PageTransformer on a ViewPager on earlier platform versions will
     * be ignored.</p>
     */
    public interface PageTransformer {

        /**
         * Apply a property transformation to the given page.
         *
         * @param page Apply the transformation to this page
         * @param position Position of page relative to the current front-and-center
         *                 position of the pager. 0 is front and center. 1 is one full
         *                 page position to the right, and -1 is one page position to the left.
         */
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        void transformPage(@NonNull View page, float position);
=======
        void transformPage(@NonNull View page, @FloatRange(from = -1.0, to = 1.0) float position);
    }

    /**
     * Add an {@link ItemDecoration} to this ViewPager2. Item decorations can
     * affect both measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     */
    public void addItemDecoration(@NonNull ItemDecoration decor) {
        mRecyclerView.addItemDecoration(decor);
    }

    /**
     * Add an {@link ItemDecoration} to this ViewPager2. Item decorations can
     * affect both measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     * @param index Position in the decoration chain to insert this decoration at. If this value
     *              is negative the decoration will be added at the end.
     * @throws IndexOutOfBoundsException on indexes larger than {@link #getItemDecorationCount}
     */
    public void addItemDecoration(@NonNull ItemDecoration decor, int index) {
        mRecyclerView.addItemDecoration(decor, index);
    }

    /**
     * Returns an {@link ItemDecoration} previously added to this ViewPager2.
     *
     * @param index The index position of the desired ItemDecoration.
     * @return the ItemDecoration at index position
     * @throws IndexOutOfBoundsException on invalid index
     */
    @NonNull
    public ItemDecoration getItemDecorationAt(int index) {
        return mRecyclerView.getItemDecorationAt(index);
    }

    /**
     * Returns the number of {@link ItemDecoration} currently added to this ViewPager2.
     *
     * @return number of ItemDecorations currently added added to this ViewPager2.
     */
    public int getItemDecorationCount() {
        return mRecyclerView.getItemDecorationCount();
    }

    /**
     * Invalidates all ItemDecorations. If ViewPager2 has item decorations, calling this method
     * will trigger a {@link #requestLayout()} call.
     */
    public void invalidateItemDecorations() {
        mRecyclerView.invalidateItemDecorations();
    }

    /**
     * Removes the {@link ItemDecoration} associated with the supplied index position.
     *
     * @param index The index position of the ItemDecoration to be removed.
     * @throws IndexOutOfBoundsException on invalid index
     */
    public void removeItemDecorationAt(int index) {
        mRecyclerView.removeItemDecorationAt(index);
    }

    /**
     * Remove an {@link ItemDecoration} from this ViewPager2.
     *
     * <p>The given decoration will no longer impact the measurement and drawing of
     * item views.</p>
     *
     * @param decor Decoration to remove
     * @see #addItemDecoration(ItemDecoration)
     */
    public void removeItemDecoration(@NonNull ItemDecoration decor) {
        mRecyclerView.removeItemDecoration(decor);
    }

    private abstract class AccessibilityProvider {
        void onInitialize(@NonNull CompositeOnPageChangeCallback pageChangeEventDispatcher,
                @NonNull RecyclerView recyclerView) {
        }

        boolean handlesGetAccessibilityClassName() {
            return false;
        }

        String onGetAccessibilityClassName() {
            throw new IllegalStateException("Not implemented.");
        }

        void onRestorePendingState() {
        }

        void onAttachAdapter(@Nullable Adapter<?> newAdapter) {
        }

        void onDetachAdapter(@Nullable Adapter<?> oldAdapter) {
        }

        void onSetOrientation() {
        }

        void onSetNewCurrentItem() {
        }

        void onSetUserInputEnabled() {
        }

        void onSetLayoutDirection() {
        }

        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        }

        boolean handlesPerformAccessibilityAction(int action, Bundle arguments) {
            return false;
        }

        boolean onPerformAccessibilityAction(int action, Bundle arguments) {
            throw new IllegalStateException("Not implemented.");
        }

        void onRvInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
        }

        boolean handlesLmPerformAccessibilityAction(int action) {
            return false;
        }

        boolean onLmPerformAccessibilityAction(int action) {
            throw new IllegalStateException("Not implemented.");
        }

        void onLmInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfoCompat info) {
        }

        boolean handlesRvGetAccessibilityClassName() {
            return false;
        }

        CharSequence onRvGetAccessibilityClassName() {
            throw new IllegalStateException("Not implemented.");
        }
    }

    class BasicAccessibilityProvider extends AccessibilityProvider {
        @Override
        public boolean handlesLmPerformAccessibilityAction(int action) {
            return (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
                    || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
                    && !isUserInputEnabled();
        }

        @Override
        public boolean onLmPerformAccessibilityAction(int action) {
            if (!handlesLmPerformAccessibilityAction(action)) {
                throw new IllegalStateException();
            }
            return false;
        }

        @Override
        public void onLmInitializeAccessibilityNodeInfo(
                @NonNull AccessibilityNodeInfoCompat info) {
            if (!isUserInputEnabled()) {
                info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                info.setScrollable(false);
            }
        }

        @Override
        public boolean handlesRvGetAccessibilityClassName() {
            return true;
        }

        @Override
        public CharSequence onRvGetAccessibilityClassName() {
            if (!handlesRvGetAccessibilityClassName()) {
                throw new IllegalStateException();
            }
            return "androidx.viewpager.widget.ViewPager";
        }
    }

    class PageAwareAccessibilityProvider extends AccessibilityProvider {
        private final AccessibilityViewCommand mActionPageForward =
                new AccessibilityViewCommand() {
                    @Override
                    public boolean perform(@NonNull View view,
                            @Nullable CommandArguments arguments) {
                        ViewPager2 viewPager = (ViewPager2) view;
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                        return true;
                    }
                };

        private final AccessibilityViewCommand mActionPageBackward =
                new AccessibilityViewCommand() {
                    @Override
                    public boolean perform(@NonNull View view,
                            @Nullable CommandArguments arguments) {
                        ViewPager2 viewPager = (ViewPager2) view;
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
                        return true;
                    }
                };

        private RecyclerView.AdapterDataObserver mAdapterDataObserver;

        @Override
        public void onInitialize(@NonNull CompositeOnPageChangeCallback pageChangeEventDispatcher,
                @NonNull RecyclerView recyclerView) {
            ViewCompat.setImportantForAccessibility(recyclerView,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);

            mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    updatePageAccessibilityActions();
                }
            };

            if (ViewCompat.getImportantForAccessibility(ViewPager2.this)
                    == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                ViewCompat.setImportantForAccessibility(ViewPager2.this,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }

        @Override
        public boolean handlesGetAccessibilityClassName() {
            return true;
        }

        @Override
        public String onGetAccessibilityClassName() {
            if (!handlesGetAccessibilityClassName()) {
                throw new IllegalStateException();
            }
            return "androidx.viewpager.widget.ViewPager";
        }

        @Override
        public void onRestorePendingState() {
            updatePageAccessibilityActions();
        }

        @Override
        public void onAttachAdapter(@Nullable Adapter<?> newAdapter) {
            updatePageAccessibilityActions();
            if (newAdapter != null) {
                newAdapter.registerAdapterDataObserver(mAdapterDataObserver);
            }
        }

        @Override
        public void onDetachAdapter(@Nullable Adapter<?> oldAdapter) {
            if (oldAdapter != null) {
                oldAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
            }
        }

        @Override
        public void onSetOrientation() {
            updatePageAccessibilityActions();
        }

        @Override
        public void onSetNewCurrentItem() {
            updatePageAccessibilityActions();
        }

        @Override
        public void onSetUserInputEnabled() {
            updatePageAccessibilityActions();
            if (Build.VERSION.SDK_INT < 21) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            }
        }

        @Override
        public void onSetLayoutDirection() {
            updatePageAccessibilityActions();
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            addCollectionInfo(info);
            if (Build.VERSION.SDK_INT >= 16) {
                addScrollActions(info);
            }
        }

        @Override
        public boolean handlesPerformAccessibilityAction(int action, Bundle arguments) {
            return action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
                    || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
        }

        @Override
        public boolean onPerformAccessibilityAction(int action, Bundle arguments) {
            if (!handlesPerformAccessibilityAction(action, arguments)) {
                throw new IllegalStateException();
            }

            int nextItem = (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
                    ? getCurrentItem() - 1
                    : getCurrentItem() + 1;
            setCurrentItem(nextItem, true);
            return true;
        }

        @Override
        public void onRvInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
            event.setSource(ViewPager2.this);
            event.setClassName(ViewPager2.this.getAccessibilityClassName());
        }

        /**
         * Update the ViewPager2's available page accessibility actions. These are updated in
         * response to page, adapter, and orientation changes. Compatible with API >= 21.
         */
        void updatePageAccessibilityActions() {
            ViewPager2 viewPager = ViewPager2.this;

            ViewCompat.removeAccessibilityAction(viewPager, ACTION_PAGE_LEFT.getId());
            ViewCompat.removeAccessibilityAction(viewPager, ACTION_PAGE_RIGHT.getId());
            ViewCompat.removeAccessibilityAction(viewPager, ACTION_PAGE_UP.getId());
            ViewCompat.removeAccessibilityAction(viewPager, ACTION_PAGE_DOWN.getId());

            if (getAdapter() == null) {
                return;
            }

            int itemCount = getAdapter().getItemCount();
            if (itemCount == 0) {
                return;
            }

            if (!isUserInputEnabled()) {
                return;
            }

            if (getOrientation() == ORIENTATION_HORIZONTAL) {
                boolean isLayoutRtl = isLayoutRtl();
                AccessibilityNodeInfoCompat.AccessibilityActionCompat actionPageForward =
                        isLayoutRtl ? ACTION_PAGE_LEFT : ACTION_PAGE_RIGHT;
                AccessibilityNodeInfoCompat.AccessibilityActionCompat actionPageBackward =
                        isLayoutRtl ? ACTION_PAGE_RIGHT : ACTION_PAGE_LEFT;

                if (mCurrentItem < itemCount - 1) {
                    ViewCompat.replaceAccessibilityAction(viewPager, actionPageForward, null,
                            mActionPageForward);
                }
                if (mCurrentItem > 0) {
                    ViewCompat.replaceAccessibilityAction(viewPager, actionPageBackward, null,
                            mActionPageBackward);
                }
            } else {
                if (mCurrentItem < itemCount - 1) {
                    ViewCompat.replaceAccessibilityAction(viewPager, ACTION_PAGE_DOWN, null,
                            mActionPageForward);
                }
                if (mCurrentItem > 0) {
                    ViewCompat.replaceAccessibilityAction(viewPager, ACTION_PAGE_UP, null,
                            mActionPageBackward);
                }
            }
        }

        private void addCollectionInfo(AccessibilityNodeInfo info) {
            int rowCount = 0;
            int colCount = 0;
            if (getAdapter() != null) {
                if (getOrientation() == ORIENTATION_VERTICAL) {
                    rowCount = getAdapter().getItemCount();
                } else {
                    colCount = getAdapter().getItemCount();
                }
            }
            AccessibilityNodeInfoCompat nodeInfoCompat = AccessibilityNodeInfoCompat.wrap(info);
            AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo =
                    AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(rowCount, colCount,
                            /* hierarchical= */false,
                            AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_NONE);
            nodeInfoCompat.setCollectionInfo(collectionInfo);
        }

        private void addScrollActions(AccessibilityNodeInfo info) {
            final Adapter<?> adapter = getAdapter();
            if (adapter == null) {
                return;
            }
            int itemCount = adapter.getItemCount();
            if (itemCount == 0 || !isUserInputEnabled()) {
                return;
            }
            if (mCurrentItem > 0) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
            }
            if (mCurrentItem < itemCount - 1) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
            info.setScrollable(true);
        }
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }
}
