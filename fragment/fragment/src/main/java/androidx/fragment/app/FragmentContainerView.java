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

package androidx.fragment.app;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * FragmentContainerView is a customized Layout designed specifically for Fragments. It extends
 * {@link FrameLayout}, so it can reliably handle Fragment Transactions, and it also has additional
 * features to coordinate with fragment behavior.
 *
 * <p>Layout animations and transitions are disabled for FragmentContainerView. Animations should be
 * done through {@link FragmentTransaction#setCustomAnimations(int, int, int, int)}. If
 * animateLayoutChanges is set to <code>true</code> or
 * {@link #setLayoutTransition(LayoutTransition)} is called directly an
 * {@link UnsupportedOperationException} will be thrown.
 *
 * <p>Fragments using exit animations are drawn before all others for FragmentContainerView. This
 * ensures that exiting Fragments do not appear on top of the view. When using this layout, a
 * Fragment with an enter animation is popped using {@link FragmentManager#popBackStack()}, the
 * reverse animation will not appear.
 *
 * <p>FragmentContainerView automatically adds a child view that is present to assist with
 * animations. If attempting to remove this view, depending on the method one of three things will
 * happen:
 *
 * 1. Appropriate assumptions can be made and the remove command will be ignored.
 * 2. If all views are being removed, the view will be removed and automatically re-added.
 * 3. There is no way to predict the desired behavior, and an {@link IllegalArgumentException} will
 *      be thrown.
 */
public final class FragmentContainerView extends FrameLayout {

    @Nullable
    protected ArrayList<View> mDisappearingFragmentChildren;

    private ArrayList<View> mTransitioningFragmentViews;

    /**
     * Placeholder view for helping correct animations.
     */
    private View mHoldView;

    public FragmentContainerView(@NonNull Context context) {
        this(context, null);
    }

    public FragmentContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FragmentContainerView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Make the holder view the first child
        mHoldView = new View(context);
        addView(mHoldView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * When called, this method throws a {@link UnsupportedOperationException}. This can be called
     * either explicitly, or implicitly by setting animateLayoutChanges to <code>true</code>.
     *
     * <p>View animations and transitions are disabled for FragmentContainerView. Use
     * {@link FragmentTransaction#setCustomAnimations(int, int, int, int)} and
     * {@link FragmentTransaction#setTransition(int)}.
     *
     * @param transition The LayoutTransition object that will animated changes in layout. A value
     * of <code>null</code> means no transition will run on layout changes.
     * @attr ref android.R.styleable#ViewGroup_animateLayoutChanges
     */
    @Override
    public void setLayoutTransition(@Nullable LayoutTransition transition) {
        throw new UnsupportedOperationException(
                "FragmentContainerView does not support Layout Transitions or "
                        + "animateLayoutChanges=\"true\".");
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (mDisappearingFragmentChildren != null) {
            for (int i = 0; i < mDisappearingFragmentChildren.size(); i++) {
                super.drawChild(canvas, mDisappearingFragmentChildren.get(i), getDrawingTime());
            }
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
        if (mDisappearingFragmentChildren != null && mDisappearingFragmentChildren.size() > 0) {
            // If the child is disappearing, we have already drawn it so skip.
            if (mDisappearingFragmentChildren.contains(child)) {
                return false;
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void startViewTransition(@NonNull View view) {
        if (view.getParent() == this) {
            if (mTransitioningFragmentViews == null) {
                mTransitioningFragmentViews = new ArrayList<>();
            }
            mTransitioningFragmentViews.add(view);
        }
        super.startViewTransition(view);
    }

    @Override
    public void endViewTransition(@NonNull View view) {
        if (mTransitioningFragmentViews != null) {
            mTransitioningFragmentViews.remove(view);
            if (mDisappearingFragmentChildren != null) {
                mDisappearingFragmentChildren.remove(view);
            }
        }
        super.endViewTransition(view);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the index passed in is 0, this method call will be ignored.
     */
    @Override
    public void removeViewAt(int index) {
        if (index != 0) {
            View view = getChildAt(index);
            addDisappearingFragmentView(view);
            super.removeViewAt(index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the view passed in is the first child view, this method call will be ignored.
     */
    @Override
    public void removeViewInLayout(@NonNull View view) {
        if (view != mHoldView) {
            addDisappearingFragmentView(view);
            super.removeViewInLayout(view);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the view passed in is the first child view, this method call will be ignored.
     */
    @Override
    public void removeView(@NonNull View view) {
        if (view != mHoldView) {
            addDisappearingFragmentView(view);
            super.removeView(view);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the start index passed in is 0, this method call will throw an
     * {@link IllegalArgumentException}.
     */
    @Override
    public void removeViews(int start, int count) {
        if (start == 0) {
            throw new IllegalArgumentException("Cannot remove the first child view in a "
                    + "FragmentContainerView. Start index must be greater than 0.");
        }
        for (int i = start; i < start + count; i++) {
            final View view = getChildAt(i);
            addDisappearingFragmentView(view);
        }
        super.removeViews(start, count);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the start index passed in is 0, this method call will throw an
     * {@link IllegalArgumentException}.
     */
    @Override
    public void removeViewsInLayout(int start, int count) {
        if (start == 0) {
            throw new IllegalArgumentException("Cannot remove the first child view in a "
                    + "FragmentContainerView. Start index must be greater than 0.");
        }
        for (int i = start; i < start + count; i++) {
            final View view = getChildAt(i);
            addDisappearingFragmentView(view);
        }
        super.removeViewsInLayout(start, count);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Once all the views are removed, FragmentContainerView automatically adds a view.
     */
    @Override
    public void removeAllViewsInLayout() {
        for (int i = getChildCount() - 1; i > 0; i--) {
            final View view = getChildAt(i);
            addDisappearingFragmentView(view);
        }
        super.removeAllViewsInLayout();
        addView(mHoldView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the view passed in is the first child view, this method call will be ignored.
     */
    @Override
    protected void removeDetachedView(@NonNull View child, boolean animate) {
        if (child != mHoldView) {
            if (animate) {
                addDisappearingFragmentView(child);
            }
            super.removeDetachedView(child, animate);
        }
    }

    /**
     * This method adds a {@link View} to the list of disappearing views only if it meets the
     * proper conditions to be considered a disappearing view.
     *
     * @param v {@link View} that might be added to list of disappearing views
     */
    private void addDisappearingFragmentView(@NonNull View v) {
        if (v.getAnimation() != null || (mTransitioningFragmentViews != null
                && mTransitioningFragmentViews.contains(v))) {
            if (mDisappearingFragmentChildren == null) {
                mDisappearingFragmentChildren = new ArrayList<>();
            }
            mDisappearingFragmentChildren.add(v);
        }
    }



}
