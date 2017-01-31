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
 * limitations under the License
 */

package android.support.v17.preference;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v17.leanback.transition.FadeAndShortSlide;
import android.transition.Transition;
import android.view.Gravity;

/**
 * @hide
 */
@RequiresApi(21)
@TargetApi(21)
@RestrictTo(LIBRARY_GROUP)
public class LeanbackPreferenceFragmentTransitionHelperApi21 {

    public static void addTransitions(Fragment f) {
        final Transition transitionStartEdge = new FadeAndShortSlide(Gravity.START);
        final Transition transitionEndEdge = new FadeAndShortSlide(Gravity.END);

        f.setEnterTransition(transitionEndEdge);
        f.setExitTransition(transitionStartEdge);
        f.setReenterTransition(transitionStartEdge);
        f.setReturnTransition(transitionEndEdge);
    }


}
