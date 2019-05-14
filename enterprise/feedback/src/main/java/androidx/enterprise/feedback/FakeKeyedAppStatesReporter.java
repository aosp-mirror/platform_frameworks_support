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

package androidx.enterprise.feedback;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fake {@link KeyedAppStatesReporter} for testing.
 *
 * Example usage:
 * <pre>
 *   FakeKeyedAppStatesReporter reporter = new FakeKeyedAppStatesReporter();
 *   // inject the reporter to the part of your code it will be used
 *   assertThat(reporter.getKeyedAppStatesByKey().get("myKey").message).isEqualTo("expected");
 * </pre>
 */
public class FakeKeyedAppStatesReporter extends KeyedAppStatesReporter {

    private List<KeyedAppState> mKeyedAppStates = new ArrayList<>();
    private List<KeyedAppState> mOnDeviceKeyedAppStates = new ArrayList<>();
    private List<KeyedAppState> mUploadedKeyedAppStates = new ArrayList<>();
    private Map<String, KeyedAppState> mKeyedAppStatesByKey = new HashMap<>();
    private Map<String, KeyedAppState> mOnDeviceKeyedAppStatesByKey = new HashMap<>();
    private Map<String, KeyedAppState> mUploadedKeyedAppStatesByKey = new HashMap<>();
    private int mNumberOfUploads = 0;

    @Override
    public void setStates(@NonNull Collection<KeyedAppState> states) {
        for (KeyedAppState state : states) {
            mOnDeviceKeyedAppStates.add(state);
            mOnDeviceKeyedAppStatesByKey.put(state.getKey(), state);
            mKeyedAppStates.add(state);
            mKeyedAppStatesByKey.put(state.getKey(), state);
        }
    }

    /**
     * Records the set states and immediately uploads the states to the server.
     *
     * <p>The fake does not enforce any quota on uploading.
     */
    @Override
    public void setStatesImmediate(@NonNull Collection<KeyedAppState> states) {
        setStates(states);
        upload();
    }

    private void upload() {
        for (KeyedAppState state : mOnDeviceKeyedAppStates) {
            mUploadedKeyedAppStates.add(state);
            mUploadedKeyedAppStatesByKey.put(state.getKey(), state);
        }
        mOnDeviceKeyedAppStates.clear();
        mOnDeviceKeyedAppStatesByKey.clear();
        mNumberOfUploads++;
    }

    /**
     * Reset the state of this fake.
     */
    public void reset() {
        mOnDeviceKeyedAppStates.clear();
        mOnDeviceKeyedAppStatesByKey.clear();
        mUploadedKeyedAppStates.clear();
        mUploadedKeyedAppStatesByKey.clear();
        mKeyedAppStates.clear();
        mKeyedAppStatesByKey.clear();
        mNumberOfUploads = 0;
    }

    /**
     * Get a list of all KeyedAppState instances that have been set.
     *
     * <p>This is in the order than they were set, and may continue multiple with the same key, if
     * that key has been set twice.
     */
    public List<KeyedAppState> getKeyedAppStates() {
        return new ArrayList<>(mKeyedAppStates);
    }

    /**
     * Get a map of the latest KeyedAppState set for each key.
     */
    public Map<String, KeyedAppState> getKeyedAppStatesByKey() {
        return new HashMap<>(mKeyedAppStatesByKey);
    }

    /**
     * Get a list of KeyedAppState instances that have been set but not yet uploaded.
     *
     * <p>This is in the order than they were set, and may continue multiple with the same key, if
     * that key has been set twice.
     *
     * <p>Once uploaded (using {@link #setStatesImmediate(Collection)}) instances will no longer be
     * returned by this method.
     */
    public List<KeyedAppState> getOnDeviceKeyedAppStates() {
        return new ArrayList<>(mOnDeviceKeyedAppStates);
    }

    /**
     * Get a map of the latest KeyedAppState set for each key that has not yet uploaded.
     *
     * <p>Once uploaded (using {@link #setStatesImmediate(Collection)}) instances will no longer be
     * returned by this method.
     */
    public Map<String, KeyedAppState> getOnDeviceKeyedAppStatesByKey() {
        return new HashMap<>(mOnDeviceKeyedAppStatesByKey);
    }

    /**
     * Get a list of KeyedAppState instances that have been set and uploaded.
     *
     * <p>This is in the order than they were set, and may continue multiple with the same key, if
     * that key has been set twice.
     *
     * <p>States will be returned by this method if they were set using
     * {@link #setStatesImmediate(Collection)} or if {@link #setStatesImmediate(Collection)} has
     * been called since they were set.
     */
    public List<KeyedAppState> getUploadedKeyedAppStates() {
        return new ArrayList<>(mUploadedKeyedAppStates);
    }

    /**
     * Get a list of the latest KeyedAppState set for each key that has been uploaded.
     *
     * <p>This is in the order than they were set, and may continue multiple with the same key, if
     * that key has been set twice.
     *
     * <p>States will be returned by this method if they were set using
     * {@link #setStatesImmediate(Collection)} or if {@link #setStatesImmediate(Collection)} has
     * been called since they were set.
     */
    public Map<String, KeyedAppState> getUploadedKeyedAppStatesByKey() {
        return new HashMap<>(mUploadedKeyedAppStatesByKey);
    }

    /**
     * Get the number of times {@link #setStatesImmediate(Collection)} has been called.
     */
    public int getNumberOfUploads() {
        return mNumberOfUploads;
    }
}
