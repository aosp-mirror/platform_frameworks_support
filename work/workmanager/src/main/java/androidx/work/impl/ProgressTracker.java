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

package androidx.work.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is the source of truth for progress information for all running
 * {@link androidx.work.ListenableWorker}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProgressTracker {

    private static final String TAG = Logger.tagWithPrefix("ProgressTracker");

    private final Map<String, MutableLiveData<Object>> mProgressMap;
    private final Object mLock;

    public ProgressTracker() {
        mProgressMap = new HashMap<>();
        mLock = new Object();
    }

    /**
     * Starts to track progress information for a given WorkSpec. This is an idempotent operation.
     *
     * @param workSpecId The {@link String} workSpecId
     * @return The instance of {@link MutableLiveData} being used to track progress information
     */
    @NonNull
    public MutableLiveData<Object> startTracking(@NonNull String workSpecId) {
        synchronized (mLock) {
            MutableLiveData<Object> progress = mProgressMap.get(workSpecId);
            if (progress != null) {
                return progress;
            } else {
                Logger.get().debug(TAG,
                        String.format("Tracking progress for WorkSpec %s", workSpecId));
                progress = new MutableLiveData<>();
                mProgressMap.put(workSpecId, progress);
                return progress;
            }
        }
    }

    /**
     * Stops tracking progress for a given workSpecId. This is typically done after the
     * {@link androidx.work.Worker} has transitioned from {@code RUNNING} state to another state.
     */
    public void stopTracking(@NonNull String workSpecId) {
        synchronized (mLock) {
            Logger.get().debug(TAG,
                    String.format("Stopping tracking of progress for %s", workSpecId));
            mProgressMap.remove(workSpecId);
        }
    }
}
