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

import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Collection;

/**
 * A reporter of keyed app states to enable communication between an app and an EMM (enterprise
 * mobility management).
 */
public interface KeyedAppStatesReporter {

    /** The value of {@link Message#what} to indicate a state update. */
    int WHAT_STATE = 1;

    /**
     * The value of {@link Message#what} to indicate a state update with request for immediate
     * upload.
     */
    int WHAT_IMMEDIATE_STATE = 2;

    /** The name for the bundle (stored as a parcelable) containing the keyed app states. */
    String APP_STATES = "androidx.enterprise.feedback.APP_STATES";

    /**
     * The name for the keyed app state key for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getKey()
     */
    String APP_STATE_KEY = "androidx.enterprise.feedback.APP_STATE_KEY";

    /**
     * The name for the severity of the app state.
     *
     * @see KeyedAppState#getSeverity()
     */
    String APP_STATE_SEVERITY = "androidx.enterprise.feedback.APP_STATE_SEVERITY";

    /**
     * The name for the optional app state message for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getMessage()
     */
    String APP_STATE_MESSAGE = "androidx.enterprise.feedback.APP_STATE_MESSAGE";

    /**
     * The name for the optional app state data for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getData()
     */
    String APP_STATE_DATA = "androidx.enterprise.feedback.APP_STATE_DATA";

    /** The intent action for reporting app states. */
    String ACTION_APP_STATES = "androidx.enterprise.feedback.action.APP_STATES";

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
