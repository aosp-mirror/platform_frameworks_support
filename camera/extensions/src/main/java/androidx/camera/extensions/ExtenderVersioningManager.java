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

package androidx.camera.extensions;

import android.util.Log;

import androidx.camera.extensions.impl.ExtenderVersioning;
import androidx.camera.extensions.impl.ExtenderVersioningImpl;

/**
 * Provides interfaces to check the extension version.
 */
class ExtenderVersioningManager {
    private static final String TAG = "ExtenderVersioning";

    private static volatile ExtenderVersioningManager sExtenderVersioningManager;

    static ExtenderVersioningManager getInstance() {
        if (sExtenderVersioningManager != null) {
            return sExtenderVersioningManager;
        }
        synchronized (ExtenderVersioningManager.class) {
            if (sExtenderVersioningManager == null) {
                sExtenderVersioningManager = new ExtenderVersioningManager();
                sExtenderVersioningManager.init();
            }
        }

        return sExtenderVersioningManager;
    }

    private ExtenderVersioning mImpl;
    private Version mRuntimeVersion;

    private ExtenderVersioningManager() {
    }

    void init() {
        mImpl = create();

        String vendorVersion = mImpl.checkApiVersion(VersionName.CURRENT.toVersionString());
        Version vendorVersionObj = Version.parse(vendorVersion);
        Version temp = null;
        if (vendorVersionObj != null) {
            for (VersionName versionName : VersionName.values()) {
                Version v = versionName.getVersion();
                if (v.isSameMajorMinor(vendorVersionObj)) {
                    // Check if it have newer patch in the supported version set.
                    if (temp == null || temp.compareTo(v) < 0) {
                        temp = v;
                    }
                }
            }
        }

        mRuntimeVersion = temp;

        Log.d(TAG, "Selected runtime: " + mRuntimeVersion);
    }

    boolean isExtensionVersionSupported() {
        return mRuntimeVersion != null;
    }

    Version getRuntimeVersion() {
        return mRuntimeVersion;
    }

    /**
     * Create a new instance of the ExtenderVersioning.
     */
    ExtenderVersioning create() {
        try {
            return new ExtenderVersioningImpl();
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "No versioning extender found. Falling back to default.");
            return new DefaultExtenderVersioning();
        }
    }

    /** Empty implementation of versioning extender which does nothing. */
    private static class DefaultExtenderVersioning implements ExtenderVersioning {
        DefaultExtenderVersioning() {
        }

        @Override
        public String checkApiVersion(String version) {
            return null;
        }
    }
}
