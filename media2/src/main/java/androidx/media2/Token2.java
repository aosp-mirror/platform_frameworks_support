/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Used for creating {@link MediaController2} to specify where to connect.
 * <p>
 * Currently it's either {@link SessionToken2} or {@link SessionServiceToken2}.
 *
 * @see SessionToken2
 * @see SessionServiceToken2
 */
// This is the base class to represent session and session service in one class.
// Base class helps controller apps to keep target of dispatching media key events in uniform way.
// Note that previously MediaSession.Token was for session and ComponentName was for service, and
// telling about media key events .
// see: android.media.session.MediaSessionManager.Callback#onAddressedPlayerChanged() (Android O+)
public abstract class Token2 {
    // From the return value of android.os.Process.getUidForName(String) when error
    static final int UID_UNKNOWN = -1;

    /**
     * @return uid
     */
    public abstract int getUid();

    /**
     * @return package name
     */
    public abstract @NonNull String getPackageName();

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public abstract boolean isLegacy();


    /**
     * Create a {@link Bundle} from this token to share it across processes.
     *
     * @return Bundle
     */
    public abstract @NonNull Bundle toBundle();

    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @param bundle
     * @return Token2 object
     */
    public static Token2 fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        Token2 token = SessionToken2.fromBundle(bundle);
        if (token != null) {
            return token;
        }
        return SessionServiceToken2.fromBundle(bundle);
    }

    static int getUid(PackageManager manager, String packageName) {
        try {
            return manager.getApplicationInfo(packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Cannot find package " + packageName);
        }
    }

    interface Token2Impl {
        int getUid();
        @NonNull String getPackageName();
        @NonNull Bundle toBundle();
        boolean isLegacy();
    }
}
