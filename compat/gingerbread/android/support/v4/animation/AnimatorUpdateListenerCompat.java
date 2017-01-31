/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.animation;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;

/**
 * Implementors of this interface can add themselves as update listeners
 * to a <code>ValueAnimator</code> instance to receive callbacks on every animation
 * frame, after the current frame's values have been calculated for that
 * <code>ValueAnimator</code>.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface AnimatorUpdateListenerCompat {

    /**
     * <p>Notifies the occurrence of another frame of the animation.</p>
     *
     * @param animation The animation which was repeated.
     */
    void onAnimationUpdate(ValueAnimatorCompat animation);

}