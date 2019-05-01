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

    static boolean canPackageReceiveAppStates(Context context, String packageName) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return packageName.equals(PHONESKY_PACKAGE_NAME)
            || devicePolicyManager.isDeviceOwnerApp(packageName)
            || devicePolicyManager.isProfileOwnerApp(packageName);
    }
}
