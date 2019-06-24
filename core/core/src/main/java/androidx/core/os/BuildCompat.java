<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
=======
/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.os;

import android.os.Build.VERSION;

/**
 * This class contains additional platform version checking methods for targeting pre-release
 * versions of Android.
 */
public class BuildCompat {
    private BuildCompat() {
    }

    /**
     * Checks if the device is running on the Android N release or newer.
     *
     * @return {@code true} if N APIs are available for use
     * @deprecated Android N is a finalized release and this method is no longer necessary. It will
     *             be removed in a future release of the Support Library. Instead, use
     *             {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.N}.
     */
    @Deprecated
    public static boolean isAtLeastN() {
        return VERSION.SDK_INT >= 24;
    }

    /**
     * Checks if the device is running on the Android N MR1 release or newer.
     *
     * @return {@code true} if N MR1 APIs are available for use
     * @deprecated Android N MR1 is a finalized release and this method is no longer necessary. It
     *             will be removed in a future release of the Support Library. Instead, use
     *             {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1}.
     */
    @Deprecated
    public static boolean isAtLeastNMR1() {
        return VERSION.SDK_INT >= 25;
    }

    /**
     * Checks if the device is running on a pre-release version of Android O or newer.
     * <p>
     * @return {@code true} if O APIs are available for use, {@code false} otherwise
     * @deprecated Android O is a finalized release and this method is no longer necessary. It will
     *             be removed in a future release of the Support Library. Instead use
     *             {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.O}.
     */
    @Deprecated
    public static boolean isAtLeastO() {
        return VERSION.SDK_INT >= 26;
    }

    /**
     * Checks if the device is running on a pre-release version of Android O MR1 or newer.
     * <p>
     * @return {@code true} if O MR1 APIs are available for use, {@code false} otherwise
     * @deprecated Android O MR1 is a finalized release and this method is no longer necessary. It
     *             will be removed in a future release of the Support Library. Instead, use
     *             {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1}.
     */
    @Deprecated
    public static boolean isAtLeastOMR1() {
        return VERSION.SDK_INT >= 27;
    }

    /**
     * Checks if the device is running on a pre-release version of Android P or newer.
     * <p>
     * @return {@code true} if P APIs are available for use, {@code false} otherwise
     * @deprecated Android P is a finalized release and this method is no longer necessary. It
     *             will be removed in a future release of the Support Library. Instead, use
     *             {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.P}.
     */
    @Deprecated
    public static boolean isAtLeastP() {
        return VERSION.SDK_INT >= 28;
    }

    /**
     * Checks if the device is running on a pre-release version of Android Q or newer.
     * <p>
     * @return {@code true} if Q APIs are available for use, {@code false} otherwise
     * @deprecated Android Q is a finalized release and this method is no longer necessary. It
     *             will be removed in a future release of the Support Library. Instead, use
     *             {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q}.
     */
    @Deprecated
    public static boolean isAtLeastQ() {
        return VERSION.SDK_INT >= 29;
    }
}
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)
