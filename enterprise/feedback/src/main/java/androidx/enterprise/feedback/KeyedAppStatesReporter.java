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

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collection;

/**
 * A reporter of keyed app states to enable communication between an app and an EMM (enterprise
 * mobility management).
 */
public abstract class KeyedAppStatesReporter {

    // Package-private constructor to restrict subclasses to the same package
    KeyedAppStatesReporter() {}

    static final String PHONESKY_PACKAGE_NAME = "com.android.vending";

    /** The value of {@link Message#what} to indicate a state update. */
    static final int WHAT_STATE = 1;

    /**
     * The value of {@link Message#what} to indicate a state update with request for immediate
     * upload.
     */
    static final int WHAT_IMMEDIATE_STATE = 2;

    /** The name for the bundle (stored as a parcelable) containing the keyed app states. */
    static final String APP_STATES = "androidx.enterprise.feedback.APP_STATES";

    /**
     * The name for the keyed app state key for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getKey()
     */
    static final String APP_STATE_KEY = "androidx.enterprise.feedback.APP_STATE_KEY";

    /**
     * The name for the severity of the app state.
     *
     * @see KeyedAppState#getSeverity()
     */
    static final String APP_STATE_SEVERITY = "androidx.enterprise.feedback.APP_STATE_SEVERITY";

    /**
     * The name for the optional app state message for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getMessage()
     */
    static final String APP_STATE_MESSAGE = "androidx.enterprise.feedback.APP_STATE_MESSAGE";

    /**
     * The name for the optional app state data for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getData()
     */
    static final String APP_STATE_DATA = "androidx.enterprise.feedback.APP_STATE_DATA";

    /** The intent action for reporting app states. */
    static final String ACTION_APP_STATES = "androidx.enterprise.feedback.action.APP_STATES";

    static boolean canPackageReceiveAppStates(Context context, String packageName) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return packageName.equals(PHONESKY_PACKAGE_NAME)
            || devicePolicyManager.isDeviceOwnerApp(packageName)
            || devicePolicyManager.isProfileOwnerApp(packageName);
    }

    /**
     * Returns an instance of {@link KeyedAppStatesReporter}.
     *
     * <p>Currently this returns the result of calling
     * {@link SingletonKeyedAppStatesReporter#getInstance()}, but this implementation detail is
     * subject to change in future.
     *
     * <p>If you need to customize the initialization parameters of the instance, you should use
     * {@link SingletonKeyedAppStatesReporter#initialize} and
     * {@link SingletonKeyedAppStatesReporter#getInstance}.
     */
    @NonNull
    public static KeyedAppStatesReporter getInstance(Context context) {
        return SingletonKeyedAppStatesReporter.getInstance(context);
    }

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
    public abstract void setStates(@NonNull Collection<KeyedAppState> states);

    /**
     * Performs the same function as {@link #setStates(Collection)}, except it
     * also requests that the states are immediately uploaded to be accessible
     * via server APIs.
     *
     * <p>The receiver is not obligated to meet this immediate upload request.
     * For example, Play and Android Management APIs have daily quotas.
     */
    public abstract void setStatesImmediate(@NonNull Collection<KeyedAppState> states);
}
