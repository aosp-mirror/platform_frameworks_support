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

package androidx.core.view;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings.Secure;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

/**
 * Helper for accessing features in {@link LocationManager}.
 */
public class LocationManagerCompat {

    private static final String LOCATION_MODE = "location_mode";

    public static final int LOCATION_MODE_OFF = 0;
    public static final int LOCATION_MODE_SENSORS_ONLY = 1;
    public static final int LOCATION_MODE_BATTERY_SAVING = 2;
    public static final int LOCATION_MODE_HIGH_ACCURACY = 3;
    public static final int LOCATION_MODE_ON = 3;

    /** Location mode constants. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        LOCATION_MODE_OFF,
        LOCATION_MODE_SENSORS_ONLY,
        LOCATION_MODE_BATTERY_SAVING,
        LOCATION_MODE_HIGH_ACCURACY,
        LOCATION_MODE_ON
    })
    public @interface LocationMode {}

    @Nullable private static Field sContextField;
    private static boolean sContextFieldFetched = false;

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
            return isLocationEnabled(locationManager) ? LOCATION_MODE_ON
                  : LOCATION_MODE_HIGH_ACCURACY;
        } else if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            // need to retrieve internal context from location manager
            if (!sContextFieldFetched) {
                try {
                    sContextField = LocationManager.class.getDeclaredField("mContext");
                    sContextField.setAccessible(true);
                } catch (NoSuchFieldException | SecurityException e) {
                    // Couldn't find the field. Abort!
                }
                sContextFieldFetched = true;
            }

            if (sContextField != null) {
                try {
                    Context context = (Context) sContextField.get(locationManager);
                    return Secure.getInt(
                        context.getContentResolver(), LOCATION_MODE, LOCATION_MODE_OFF);
                } catch (IllegalAccessException | ClassCastException e) {
                    // Field get failed. Oh well...
                }
            }

            // We failed, return OFF
            return LOCATION_MODE_OFF;
        } else {
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
