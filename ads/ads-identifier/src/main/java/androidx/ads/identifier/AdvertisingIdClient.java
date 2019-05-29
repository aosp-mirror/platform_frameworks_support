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

import static androidx.ads.identifier.internal.BlockingServiceConnection.checkNotMainThread;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.util.Log;

import androidx.ads.identifier.internal.BlockingServiceConnection;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Helper library for retrieval of advertising ID and related information such as the limit ad
 * tracking setting.
 */
public class AdvertisingIdClient {

    @VisibleForTesting
    static final String GET_AD_ID_ACTION = "androidx.ads.identifier.provider.GET_AD_ID";

    @VisibleForTesting
    static final String HIGH_PRIORITY_PERMISSION = "androidx.ads.identifier.provider.HIGH_PRIORITY";

    private static final String TAG = "AdvertisingIdClient";

    private static final long SERVICE_TIMEOUT_SECONDS = 10;

    @Nullable
    private BlockingServiceConnection mConnection;

    @Nullable
    private IAdvertisingIdService mService;

    private final Context mContext;

    private ComponentName mComponentName;

    /** Constructs a new {@link AdvertisingIdClient} object. */
    @VisibleForTesting
    AdvertisingIdClient(Context context) {
        Preconditions.checkNotNull(context);
        Context applicationContext = context.getApplicationContext();
        mContext = applicationContext == null ? context : applicationContext;
    }

    private void start()
            throws IOException, IllegalStateException, AdvertisingIdNotAvailableException {
        if (mConnection == null) {
            mComponentName = getProviderComponentName(mContext);
            mConnection = getServiceConnection();
            mService = getAdvertisingIdService(mConnection);
        }
    }

    /** Returns the advertising ID info using {@link AdvertisingIdInfo}. */
    @VisibleForTesting
    AdvertisingIdInfo getInfo() throws IOException, AdvertisingIdNotAvailableException {
        if (mConnection == null) {
            start();
        }
        Preconditions.checkNotNull(mComponentName);
        Preconditions.checkNotNull(mConnection);
        Preconditions.checkNotNull(mService);
        try {
            String id = mService.getId();
            if (id == null || id.trim().isEmpty()) {
                throw new AdvertisingIdNotAvailableException();
            }
            return AdvertisingIdInfo.builder()
                    .setId(normalizeId(id))
                    .setProviderPackageName(mComponentName.getPackageName())
                    .setLimitAdTrackingEnabled(mService.isLimitAdTrackingEnabled())
                    .build();
        } catch (RemoteException e) {
            Log.i(TAG, "Remote exception ", e);
            throw new IOException("Remote exception", e);
        }
    }

    /**
     * Checks the Ad ID format, if it's not in UUID format, normalizes the Ad ID to UUID format.
     * Returned Ad ID will in lower case format using locale {@code Locale.US};
     */
    private static String normalizeId(String id) {
        String lowerCaseId = id.toLowerCase(Locale.US);
        if (isUuidFormat(lowerCaseId)) {
            return lowerCaseId;
        }
        return UUID.nameUUIDFromBytes(id.getBytes(Charset.forName("UTF-8"))).toString();
    }

    /* Validate the input is lowercase and is a valid UUID. */
    private static boolean isUuidFormat(String id) {
        try {
            return id.equals(UUID.fromString(id).toString());
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    /** Closes the connection. */
    @VisibleForTesting
    void finish() {
        checkNotMainThread("Calling this from your main thread can lead to ANR");
        if (mConnection == null) {
            return;
        }
        try {
            mContext.unbindService(mConnection);
        } catch (Throwable e) {
            // This could happen when the thread takes too long to disconnect from the service.
            Log.i(TAG, "AdvertisingIdClient unbindService failed.", e);
        }
        mService = null;
        mConnection = null;
        mComponentName = null;
    }

    /** Closes the connection before the client is finalized. */
    @Override
    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    private static ComponentName getProviderComponentName(Context context)
            throws AdvertisingIdNotAvailableException {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = queryAdIdServices(packageManager);
        ServiceInfo serviceInfo = selectServiceByPriority(resolveInfos, packageManager);
        if (serviceInfo == null) {
            throw new AdvertisingIdNotAvailableException();
        }
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    private static List<ResolveInfo> queryAdIdServices(PackageManager packageManager) {
        Intent intent = new Intent(GET_AD_ID_ACTION);
        return packageManager.queryIntentServices(intent, 0);
    }

    @VisibleForTesting
    static ServiceInfo selectServiceByPriority(
            List<ResolveInfo> resolveInfos, PackageManager packageManager) {
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }
        ServiceInfo selectedServiceInfo = null;
        PackageInfo selectedPackageInfo = null;
        for (ResolveInfo resolveInfo : resolveInfos) {
            PackageInfo packageInfo;
            try {
                packageInfo =
                        packageManager.getPackageInfo(
                                resolveInfo.serviceInfo.packageName,
                                PackageManager.GET_PERMISSIONS);
            } catch (NameNotFoundException ignored) {
                // Ignore this provider if name not found.
                continue;
            }
            if (selectedPackageInfo == null
                    || isCandidatePackageHasHigherPriority(packageInfo, selectedPackageInfo)) {
                selectedServiceInfo = resolveInfo.serviceInfo;
                selectedPackageInfo = packageInfo;
            }
        }
        return selectedServiceInfo;
    }

    private static boolean isCandidatePackageHasHigherPriority(PackageInfo candidate,
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

    /**
     * Retrieves BlockingServiceConnection which must be unbound after use.
     *
     * @throws IOException when unable to bind service successfully.
     */
    @VisibleForTesting
    BlockingServiceConnection getServiceConnection() throws IOException {
        Intent intent = new Intent(GET_AD_ID_ACTION);
        intent.setComponent(mComponentName);

        final BlockingServiceConnection bsc = new BlockingServiceConnection();
        try {
            if (mContext.bindService(intent, bsc, Service.BIND_AUTO_CREATE)) {
                return bsc;
            }
        } catch (Throwable e) {
            throw new IOException(e);
        }

        throw new IOException("Connection failure");
    }

    /**
     * Get the AdvertisingIdService from the blocking queue. This should wait until
     * onServiceConnected event with a {@link #SERVICE_TIMEOUT_SECONDS} second timeout.
     *
     * @throws IOException if connection failed or the timeout period has expired.
     */
    private static IAdvertisingIdService getAdvertisingIdService(BlockingServiceConnection bsc)
            throws IOException {
        try {
            // Block until the bind is complete, or timeout period is over.
            return IAdvertisingIdService.Stub.asInterface(
                    bsc.getServiceWithTimeout(SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    /**
     * Checks whether there is any advertising ID provider installed on the device.
     *
     * <p>This method can be called in the main thread, it does a quick check for the advertising ID
     * providers.
     * <p>Note: Even this method returns true, there still be a possibility that the
     * {@link #getAdvertisingIdInfo(Context)} method throws an exception for some reasons.
     *
     * @param context Current {@link Context} (such as the current {@link android.app.Activity}).
     * @return whether there is Ad ID providers available on the device.
     */
    public static boolean isAdvertisingIdProvidersAvailable(Context context) {
        List<ResolveInfo> resolveInfos = queryAdIdServices(context.getPackageManager());
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    /**
     * Retrieves the user's advertising ID info.
     *
     * <p>This method cannot be called in the main thread as it may block leading to ANRs. An {@code
     * IllegalStateException} will be thrown if this is called on the main thread.
     *
     * @param context Current {@link Context} (such as the current {@link android.app.Activity}).
     * @return {@link AdvertisingIdInfo} with user's advertising ID info.
     * @throws IOException                        signaling connection to Ad ID providers failed.
     * @throws IllegalStateException              indicating this method was called on the main
     *                                            thread.
     * @throws AdvertisingIdNotAvailableException indicating advertising ID is not available, like
     *                                            no advertising ID provider found or provider
     *                                            does not return an ID.
     */
    public static AdvertisingIdInfo getAdvertisingIdInfo(Context context)
            throws IOException, IllegalStateException, AdvertisingIdNotAvailableException {
        checkNotMainThread("Calling this from your main thread can lead to ANR");
        AdvertisingIdClient client = new AdvertisingIdClient(context);
        try {
            return client.getInfo();
        } finally {
            client.finish();
        }
    }
}
