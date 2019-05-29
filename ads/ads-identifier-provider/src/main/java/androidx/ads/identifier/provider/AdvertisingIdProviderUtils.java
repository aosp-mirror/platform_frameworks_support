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

package androidx.ads.identifier.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * The utils class for the advertising ID providers.
 */
public class AdvertisingIdProviderUtils {

    @VisibleForTesting
    static final String GET_AD_ID_ACTION = "androidx.ads.identifier.provider.GET_AD_ID";

    @VisibleForTesting
    static final String OPEN_SETTINGS_ACTION = "androidx.ads.identifier.provider.OPEN_SETTINGS";

    private static Callable<AdvertisingIdProviderInterface> sProviderCallable = null;

    /**
     * Registers the {@link Callable} to create the Ads ID provider.
     *
     * <p>This {@link Callable} will be called within the library's built-in Ads ID Service's
     * {@link android.app.Service#onCreate} method.
     * <p>Provider could call this method to register the implementation in
     * {@link android.app.Application#onCreate}, which before {@link android.app.Service#onCreate}
     * has been called.
     */
    public static void registerProviderCallable(
            Callable<AdvertisingIdProviderInterface> providerCallable) {
        sProviderCallable = Preconditions.checkNotNull(providerCallable);
    }

    /**
     * Gets the {@link Callable} to create the Ads ID provider.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public static Callable<AdvertisingIdProviderInterface> getProviderCallable() {
        return sProviderCallable;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    public static void clearProviderCallable() {
        sProviderCallable = null;
    }

    /**
     * Retrieves a list of all Ad ID providers' information on this device, which is also based on
     * the AndroidX Ads ID provider library , including self.
     */
    public static List<AdvertisingIdProviderInfo> getAllAdIdProviders(Context context) {
        Intent serviceIntent = new Intent(GET_AD_ID_ACTION);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(serviceIntent, 0);
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> activityMap = getActivityMap(packageManager);

        List<AdvertisingIdProviderInfo> providerInfos = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.serviceInfo.packageName;

            AdvertisingIdProviderInfo.Builder builder =
                    AdvertisingIdProviderInfo.builder().setPackageName(packageName);
            String activityName = activityMap.get(packageName);
            if (activityName != null) {
                builder.setSettingsIntent(
                        new Intent(OPEN_SETTINGS_ACTION)
                                .setClassName(packageName, activityName));
            }
            providerInfos.add(builder.build());
        }
        return providerInfos;
    }

    private static Map<String, String> getActivityMap(PackageManager packageManager) {
        Intent settingsIntent = new Intent(OPEN_SETTINGS_ACTION);
        List<ResolveInfo> settingsResolveInfos =
                packageManager.queryIntentActivities(settingsIntent, 0);
        if (settingsResolveInfos == null || settingsResolveInfos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> activityMap = new HashMap<>();
        for (ResolveInfo settingsResolveInfo : settingsResolveInfos) {
            activityMap.put(
                    settingsResolveInfo.activityInfo.packageName,
                    settingsResolveInfo.activityInfo.name);
        }
        return activityMap;
    }
}
