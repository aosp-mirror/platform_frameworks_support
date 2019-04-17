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

package androidx.appcompat.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
final class AutoNightModeManagers {

    private AutoNightModeManagers() {
    }

    private static AutoNightModeManagerFactory sFactory = null;

    /**
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(LIBRARY)
    static void setFactory(@Nullable AutoNightModeManagerFactory factory) {
        sFactory = factory;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    static AutoNightModeManager createManagerForMode(
            @NonNull AppCompatDelegateImpl delegate,
            @AppCompatDelegate.NightMode int mode) {
        if (sFactory != null) {
            return sFactory.createManagerForMode(delegate, mode);
        }
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY:
                return new AutoBatteryNightModeManager(delegate);
            case AppCompatDelegate.MODE_NIGHT_AUTO_TIME:
                return new AutoTimeNightModeManager(delegate);
        }
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    interface AutoNightModeManagerFactory {
        AutoNightModeManager createManagerForMode(@NonNull AppCompatDelegateImpl delegate,
                @AppCompatDelegate.NightMode int mode);
    }
}
