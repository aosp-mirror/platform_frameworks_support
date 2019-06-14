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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

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
 * @hide -- remove once z ordering is implemented.
 */
@RestrictTo(LIBRARY)
public final class FragmentContainerView extends FrameLayout {

    private OnApplyWindowInsetsListener mApplyWindowInsetsListener;

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
        setupForInsets();
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

    /**
     * Setup ApplyWindowInsetsListener to dispatch fresh {@link WindowInsets} to each child view
     * when activated, and set the View UI flags appropriately.
     */
    private void setupForInsets() {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }

        if (mApplyWindowInsetsListener == null) {
            mApplyWindowInsetsListener = (v, insets) -> {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    // Give child views fresh insets.
                    child.dispatchApplyWindowInsets(new WindowInsets(insets));
                }
                return insets;
            };
        }
        // First apply the insets listener
        setOnApplyWindowInsetsListener(mApplyWindowInsetsListener);

        // Now set the sys ui flags to enable us to lay out in the window insets
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
