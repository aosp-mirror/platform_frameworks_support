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

package android.support.transition;

import android.annotation.TargetApi;
import android.support.annotation.RequiresApi;

/**
 * Utility class for creating a default transition that automatically fades,
 * moves, and resizes views during a scene change.
 *
 * <p>An AutoTransition can be described in a resource file by using the
 * tag <code>autoTransition</code>, along with the other standard
 * attributes of {@link android.R.styleable#Transition}.</p>
 */
@RequiresApi(14)
@TargetApi(14)
class AutoTransitionPort extends TransitionSetPort {

    /**
     * Constructs an AutoTransition object, which is a TransitionSet which
     * first fades out disappearing targets, then moves and resizes existing
     * targets, and finally fades in appearing targets.
     */
    public AutoTransitionPort() {
        setOrdering(ORDERING_SEQUENTIAL);
        addTransition(new FadePort(FadePort.OUT)).
                addTransition(new ChangeBoundsPort()).
                addTransition(new FadePort(FadePort.IN));
    }
}
