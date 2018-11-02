/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.coordinatorlayout.testutils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.test.espresso.IdlingResource;

public class CoordinatorLayoutUtils {

    public static class DependentBehavior extends CoordinatorLayout.Behavior<View> {
        private final View mDependency;

        public DependentBehavior(View dependency) {
            mDependency = dependency;
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
            return mDependency != null && dependency == mDependency;
        }
    }

    public static class TranslateBehavior extends CoordinatorLayout.Behavior<View> {
        public static final int TRANSLATION_MULTIPLIER = 7;

        private final int mDependencyId;

        public TranslateBehavior(@IdRes final int dependencyId) {
            super();

            mDependencyId = dependencyId;
        }

        @Override
        public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull View child,
                                       @NonNull View dependency) {
            return dependency.getId() == mDependencyId
                    || super.layoutDependsOn(parent, child, dependency);
        }

        @Override
        public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent,
                                              @NonNull View child, @NonNull View dependency) {
            child.setTranslationY(-dependency.getTranslationY() * TRANSLATION_MULTIPLIER);
            return true;
        }
    }

    public static class AnimationIdlingResource extends AnimatorListenerAdapter
            implements IdlingResource {

        private boolean mIsIdle = true;
        private ResourceCallback mCallback;

        @Override
        public String getName() {
            return "AnimationIdlingResource";
        }

        @Override
        public boolean isIdleNow() {
            return mIsIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            setIdle(false);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            setIdle(true);
        }

        private void setIdle(boolean idle) {
            boolean wasIdle = mIsIdle;
            mIsIdle = idle;
            if (mIsIdle && !wasIdle && mCallback != null) {
                mCallback.onTransitionToIdle();
            }
        }
    }

}
