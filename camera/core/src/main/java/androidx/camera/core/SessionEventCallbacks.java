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

package androidx.camera.core;


import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Different implementations of {@link SessionEventCallback}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SessionEventCallbacks {
    private SessionEventCallbacks() {
    }

    /** Returns a session event callback which does nothing. */
    public static SessionEventCallback createNoOpCallback() {
        return new NoOpSessionEventCallback();
    }

    /** Returns a session event callback which calls a list of other callbacks. */
    public static SessionEventCallback createComboCallback(List<SessionEventCallback> callbacks) {
        return new ComboSessionEventCallback(callbacks);
    }

    /** Returns a session event callback which calls a list of other callbacks. */
    public static SessionEventCallback createComboCallback(SessionEventCallback... callbacks) {
        return createComboCallback(Arrays.asList(callbacks));
    }

    static final class NoOpSessionEventCallback extends SessionEventCallback {

    }

    /**
     * A SessionEventCallback which contains a list of SessionEventCallback and will
     * propagate received callback to the list.
     */
    public static final class ComboSessionEventCallback extends SessionEventCallback {
        private final List<SessionEventCallback> mCallbacks = new ArrayList<>();

        ComboSessionEventCallback(List<SessionEventCallback> callbacks) {
            for (SessionEventCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                if (!(callback instanceof NoOpSessionEventCallback)) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public List<CaptureStage> onPresetSession() {
            List<CaptureStage> ret = new LinkedList<>();
            for (SessionEventCallback callback : mCallbacks) {
                Collection<CaptureStage> presetCaptureStage = callback.onPresetSession();
                if (presetCaptureStage != null) {
                    ret.addAll(presetCaptureStage);
                }
            }
            return ret;
        }

        @Override
        public List<CaptureStage> onEnableSession() {
            List<CaptureStage> ret = new LinkedList<>();
            for (SessionEventCallback callback : mCallbacks) {
                Collection<CaptureStage> enableCaptureStage = callback.onEnableSession();
                if (enableCaptureStage != null) {
                    ret.addAll(enableCaptureStage);
                }
            }
            return ret;
        }

        @Override
        public List<CaptureStage> onRepeating() {
            List<CaptureStage> ret = new LinkedList<>();
            for (SessionEventCallback callback : mCallbacks) {
                Collection<CaptureStage> repeatingCaptureStage = callback.onRepeating();
                if (repeatingCaptureStage != null) {
                    ret.addAll(repeatingCaptureStage);
                }
            }
            return ret;
        }

        @Override
        public List<CaptureStage> onDisableSession() {
            List<CaptureStage> ret = new LinkedList<>();
            for (SessionEventCallback callback : mCallbacks) {
                Collection<CaptureStage> disableCaptureStage = callback.onDisableSession();
                if (disableCaptureStage != null) {
                    ret.addAll(disableCaptureStage);
                }
            }
            return ret;
        }

        @NonNull
        public List<SessionEventCallback> getCallbacks() {
            return mCallbacks;
        }
    }
}
