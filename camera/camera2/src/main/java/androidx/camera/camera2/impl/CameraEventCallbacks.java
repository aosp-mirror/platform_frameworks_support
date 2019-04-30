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

package androidx.camera.camera2.impl;


import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.AppendableValue;
import androidx.camera.core.CaptureConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Different implementations of {@link CameraEventCallback}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CameraEventCallbacks extends AppendableValue<CameraEventCallback> {

    public CameraEventCallbacks(CameraEventCallback ... callbacks) {
        appendAll(Arrays.asList(callbacks));
    }

    /** Returns a camera event callback which calls a list of other callbacks. */
    public CameraEventCallback createComboCallback() {
        return new ComboCameraEventCallback(getAllItems());
    }

    /** Returns a camera event callback which does nothing. */
    public static CameraEventCallbacks createEmptyCallback() {
        return new CameraEventCallbacks();
    }

    /**
     * A CameraEventCallback which contains a list of CameraEventCallback and will
     * propagate received callback to the list.
     */
    public static final class ComboCameraEventCallback extends CameraEventCallback {
        private final List<CameraEventCallback> mCallbacks = new ArrayList<>();

        ComboCameraEventCallback(List<CameraEventCallback> callbacks) {
            for (CameraEventCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                mCallbacks.add(callback);
            }
        }

        @Override
        public List<CaptureConfig> onPresetSession() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                Collection<CaptureConfig> presetCaptureStage = callback.onPresetSession();
                if (presetCaptureStage != null) {
                    ret.addAll(presetCaptureStage);
                }
            }
            return ret;
        }

        @Override
        public List<CaptureConfig> onEnableSession() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                Collection<CaptureConfig> enableCaptureStage = callback.onEnableSession();
                if (enableCaptureStage != null) {
                    ret.addAll(enableCaptureStage);
                }
            }
            return ret;
        }

        @Override
        public List<CaptureConfig> onRepeating() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                Collection<CaptureConfig> repeatingCaptureStage = callback.onRepeating();
                if (repeatingCaptureStage != null) {
                    ret.addAll(repeatingCaptureStage);
                }
            }
            return ret;
        }

        @Override
        public List<CaptureConfig> onDisableSession() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                Collection<CaptureConfig> disableCaptureStage = callback.onDisableSession();
                if (disableCaptureStage != null) {
                    ret.addAll(disableCaptureStage);
                }
            }
            return ret;
        }

        @NonNull
        public List<CameraEventCallback> getCallbacks() {
            return mCallbacks;
        }
    }
}
