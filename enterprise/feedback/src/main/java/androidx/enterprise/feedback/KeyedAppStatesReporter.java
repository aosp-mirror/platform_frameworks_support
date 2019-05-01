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

import java.util.Collection;

/**
 * A reporter of keyed app states to enable communication between an app and an EMM (enterprise
 * mobility management).
 */
public interface KeyedAppStatesReporter {
    /**
     * Set app states to be sent to an EMM (enterprise mobility management). The EMM can then
     * display this information to the management organization.
     *
     * <p>Do not send personally-identifiable information with this method.
     *
     * <p>Each provided keyed app state will replace any previously set keyed app states with the
     * same key for this package name.
     *
     * <p>If multiple keyed app states are set with the same key, only one will be received by the
     * EMM. Which will be received is not defined.
     *
     * <p>This information is sent immediately to all device owner and profile owner apps on the
     * device. It is also sent immediately to the app with package name com.android.vending if it
     * exists, which is the Play Store on GMS devices.
     *
     * <p>EMMs can access these states either directly in a custom DPC (device policy manager), via
     * Android Management APIs, or via Play EMM APIs.
     *
     * @see #setStatesImmediate(Collection)
     */
    void setStates(@NonNull Collection<KeyedAppState> states);

    /**
     * Performs the same function as {@link #setStates(Collection)}, except it
     * also requests that the states are immediately uploaded to be accessible
     * via server APIs.
     *
     * <p>The receiver is not obligated to meet this immediate upload request.
     * For example, Play and Android Management APIs have daily quotas.
     */
    void setStatesImmediate(@NonNull Collection<KeyedAppState> states);
}
