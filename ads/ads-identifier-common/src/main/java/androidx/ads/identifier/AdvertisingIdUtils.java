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

package androidx.ads.identifier;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.List;

/**
 * The internal utils class for the advertising ID.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AdvertisingIdUtils {

    public static final String GET_AD_ID_ACTION = "androidx.ads.identifier.provider.GET_AD_ID";

    @VisibleForTesting
    static final String HIGH_PRIORITY_PERMISSION = "androidx.ads.identifier.provider.HIGH_PRIORITY";

    AdvertisingIdUtils() {}

    /** Retrieves a list of all Ad ID providers' services on this device. */
    @NonNull
    public static List<ResolveInfo> getAdIdProviders(PackageManager packageManager) {
        Intent intent = new Intent(GET_AD_ID_ACTION);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intent, 0);
        return resolveInfos != null ? resolveInfos : Collections.emptyList();
    }

    /**
     * Selects one service with the highest priority from the input {@code resolveInfos}.
     *
     * @return null if the input {@code resolveInfos} is null or empty.
     */
    @Nullable
    public static ServiceInfo selectServiceByPriority(
            List<ResolveInfo> resolveInfos, PackageManager packageManager) {
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }
        ServiceInfo selectedServiceInfo = null;
        PackageInfo selectedPackageInfo = null;
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            PackageInfo packageInfo;
            try {
                packageInfo =
                        packageManager.getPackageInfo(
                                serviceInfo.packageName, PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Ignore this provider if name not found.
                continue;
            }
            if (selectedPackageInfo == null
                    || hasHigherPriority(packageInfo, selectedPackageInfo)) {
                selectedServiceInfo = serviceInfo;
                selectedPackageInfo = packageInfo;
            }
        }
        return selectedServiceInfo;
    }

    private static boolean hasHigherPriority(PackageInfo candidate,
            PackageInfo currentHighest) {
        boolean isCandidateSystemPackage = isSystemPackage(candidate);
        boolean isCurrentHighestSystemPackage = isSystemPackage(currentHighest);
        if (isCandidateSystemPackage != isCurrentHighestSystemPackage) {
            return isCandidateSystemPackage;
        }
        if (isCandidateSystemPackage) {
            boolean candidateRequestHighPriority = isRequestHighPriority(candidate);
            boolean currentHighestRequestHighPriority = isRequestHighPriority(currentHighest);
            if (candidateRequestHighPriority != currentHighestRequestHighPriority) {
                return candidateRequestHighPriority;
            }
        }
        if (candidate.firstInstallTime != currentHighest.firstInstallTime) {
            return candidate.firstInstallTime < currentHighest.firstInstallTime;
        }
        return candidate.packageName.compareTo(currentHighest.packageName) < 0;
    }

    private static boolean isSystemPackage(PackageInfo packageInfo) {
        return packageInfo.applicationInfo != null
                && (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                == ApplicationInfo.FLAG_SYSTEM;
    }

    private static boolean isRequestHighPriority(PackageInfo packageInfo) {
        if (packageInfo.requestedPermissions == null) {
            return false;
        }
        for (String permission : packageInfo.requestedPermissions) {
            if (HIGH_PRIORITY_PERMISSION.equals(permission)) {
                return true;
            }
        }
        return false;
    }
}
