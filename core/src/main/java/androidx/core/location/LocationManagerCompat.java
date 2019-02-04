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

package androidx.core.location;

import android.annotation.SuppressLint;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for accessing features in {@link LocationManager}.
 */
public final class LocationManagerCompat {

    private static final String LOCATION_MODE = "location_mode";

    public static final int LOCATION_MODE_OFF = 0;
    public static final int LOCATION_MODE_SENSORS_ONLY = 1;
    public static final int LOCATION_MODE_BATTERY_SAVING = 2;
    public static final int LOCATION_MODE_HIGH_ACCURACY = 3;
    public static final int LOCATION_MODE_ON = 3;

    /** Location mode constants. */
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("UniqueConstants")  // because HIGH_ACCURACY and ON are aliases
    @IntDef({
        LOCATION_MODE_OFF,
        LOCATION_MODE_SENSORS_ONLY,
        LOCATION_MODE_BATTERY_SAVING,
        LOCATION_MODE_HIGH_ACCURACY,
        LOCATION_MODE_ON
    })
    public @interface LocationMode {}

    /**
     * Returns the current enabled/disabled state of location.
     *
     * @return true if location is enabled and false if location is disabled.
     */
    public static boolean isLocationEnabled(@NonNull LocationManager locationManager) {
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return getLocationMode(locationManager) != LOCATION_MODE_OFF;
        }
    }

    /**
     * Returns the current mode of location.
     *
     * @return one of the {@link LocationMode} constants.
     */
    @LocationMode
    public static int getLocationMode(@NonNull LocationManager locationManager) {
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            return isLocationEnabled(locationManager) ? LOCATION_MODE_ON : LOCATION_MODE_OFF;
        } else {
            // NOTE: for KitKat and above, it's preferable to use the proper API at the time to get
            // the location mode, Secure.getInt(context, LOCATION_MODE, LOCATION_MODE_OFF). however,
            // this requires a context we don't have directly (we could either ask the client to
            // pass one in, or use reflection to get it from the location manager), and since KitKat
            // and above remained backwards compatible, we can fallback to pre-kitkat behavior.

            boolean nlpEnabled = locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER);
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (nlpEnabled && gpsEnabled) {
                return LOCATION_MODE_HIGH_ACCURACY;
            } else if (nlpEnabled) {
                return LOCATION_MODE_BATTERY_SAVING;
            } else if (gpsEnabled) {
                return LOCATION_MODE_SENSORS_ONLY;
            } else {
                return LOCATION_MODE_OFF;
            }
        }
    }

    private LocationManagerCompat() {}
}
