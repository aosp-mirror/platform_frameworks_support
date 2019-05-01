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

/**
 * Utility methods for use by {@link KeyedAppStateReporter} implementations.
 */
final class KeyedAppStatesReporterUtil {
    private KeyedAppStatesReporterUtil() {

    }

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
}
