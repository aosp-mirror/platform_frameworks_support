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

import java.util.HashSet;
import java.util.Set;

/**
 * Provides interfaces to check the extension version.
 */
class ExtenderVersioningManager {
    private static final String TAG = "ExtenderVersioning";

    private final Version mCurrentVersion = Version.create(1, 0, 0, "alpha02");

    private final Set<Version> mSupportedVersionSet = new HashSet<>();
    private final ExtenderVersioning mImpl;
    private final Version mDeviceSupportedVersion;

    private static volatile ExtenderVersioningManager sExtenderVersioningManager;

    static ExtenderVersioningManager getInstance() {
        if (sExtenderVersioningManager != null) {
            return sExtenderVersioningManager;
        }
        synchronized (ExtenderVersioningManager.class) {
            if (sExtenderVersioningManager == null) {
                sExtenderVersioningManager = new ExtenderVersioningManager();
            }
        }

        return sExtenderVersioningManager;
    }

    private ExtenderVersioningManager() {
        mSupportedVersionSet.add(mCurrentVersion);
        mImpl = create();

        String vendorVersionString = mImpl.checkApiVersion(mCurrentVersion.toVersionString());
        Version vendorVersionObj = Version.parse(vendorVersionString);
        Version temp = null;
        for (Version v : mSupportedVersionSet) {
            if (v.getMajor() == vendorVersionObj.getMajor()
                    && v.getMinor() == vendorVersionObj.getMinor()) {
                if (temp == null || temp.compareTo(v) < 0) {
                    temp = v;
                }
            }
        }
        mDeviceSupportedVersion = temp;
    }

    boolean isExtensionVersionSupported() {
        return mDeviceSupportedVersion != null;
    }

    Version getDeviceSupportedVersion() {
        return mDeviceSupportedVersion;
    }

    /**
     * Create a new instance of the ExtenderVersioning.
     */
    private ExtenderVersioning create() {
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
