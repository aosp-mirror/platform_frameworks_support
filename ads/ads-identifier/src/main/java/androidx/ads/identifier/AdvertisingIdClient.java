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
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Helper library for retrieval of advertising ID and related information such as the limit ad
 * tracking setting.
 */
public class AdvertisingIdClient {

    private static final String TAG = "AdIdClientCompat";

    private static final String ADVERTISING_ID_SERVICE = "androidx.ads.identifier.provider.START";
    private static final String HIGH_PRIORITY_PERMISSION =
            "androidx.ads.identifier.provider.HIGH_PRIORITY";
    private static final long SERVICE_TIMEOUT_SECONDS = 10;

    @GuardedBy("this")
    @Nullable
    private BlockingServiceConnection mConnection;

    @GuardedBy("this")
    @Nullable
    private IAdvertisingIdService mService;

    @GuardedBy("this")
    private final Context mContext;

    @GuardedBy("this")
    private ComponentName mComponentName;

    /** Constructs a new {@link AdvertisingIdClient} object. */
    private AdvertisingIdClient(Context context) {
        Preconditions.checkNotNull(context);
        Context applicationContext = context.getApplicationContext();
        mContext = applicationContext == null ? context : applicationContext;
    }

    private void start()
            throws IOException, IllegalStateException, AdvertisingIdServicesNotAvailableException {
        synchronized (this) {
            if (mConnection == null) {
                mComponentName = getProviderComponentName(mContext);
                mConnection = getServiceConnection(mContext, mComponentName);
                mService = getAdvertisingIdService(mConnection);
            }
        }
    }

    /** Returns the advertising ID info using {@link AdvertisingIdInfo}. */
    private AdvertisingIdInfo getInfo()
            throws IOException, AdvertisingIdServicesNotAvailableException {
        AdvertisingIdInfo info;
        synchronized (this) {
            if (mConnection == null) {
                start();
            }
            Preconditions.checkNotNull(mComponentName);
            Preconditions.checkNotNull(mConnection);
            Preconditions.checkNotNull(mService);
            try {
                String id = mService.getId();
                if (id == null || id.trim().isEmpty()) {
                    throw new AdvertisingIdServicesNotAvailableException();
                }
                info =
                        AdvertisingIdInfo.builder()
                                .setId(normalizeId(id))
                                .setProviderPackageName(mComponentName.getPackageName())
                                .setLimitAdTrackingEnabled(mService.isLimitAdTrackingEnabled())
                                .build();
            } catch (RemoteException e) {
                Log.i(TAG, "Remote exception ", e);
                throw new IOException("Remote exception");
            }
        }
        return info;
    }

    private static String normalizeId(String id) {
        String lowerCaseId = id.toLowerCase();
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
    private void finish() {
        checkNotMainThread("Calling this from your main thread can lead to deadlock");
        synchronized (this) {
            if (mConnection == null) {
                return;
            }
            try {
                mContext.unbindService(mConnection);
            } catch (Throwable e) {
                // This could happen when the thread takes too long to discount from the service.
                Log.i(TAG, "AdvertisingIdClient unbindService failed.", e);
            }
            mService = null;
            mConnection = null;
            mComponentName = null;
        }
    }

    /** Closes the connection before the client is finalized. */
    @Override
    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    private static ComponentName getProviderComponentName(Context context)
            throws AdvertisingIdServicesNotAvailableException {
        Intent advertisingIdIntent = new Intent(ADVERTISING_ID_SERVICE);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(advertisingIdIntent, 0);
        ServiceInfo serviceInfo = selectServiceByPriority(resolveInfos, packageManager);
        if (serviceInfo == null) {
            throw new AdvertisingIdServicesNotAvailableException();
        }
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    private static ServiceInfo selectServiceByPriority(
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
                    || comparePackageInfo(packageInfo, selectedPackageInfo) > 0) {
                selectedServiceInfo = resolveInfo.serviceInfo;
                selectedPackageInfo = packageInfo;
            }
        }
        return selectedServiceInfo;
    }

    // Comparator to compare all the Ad ID provider, greater means higher priority.
    private static int comparePackageInfo(PackageInfo packageInfo1, PackageInfo packageInfo2) {
        int flagSystem1 = packageInfo1.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM;
        int flagSystem2 = packageInfo2.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM;
        if (flagSystem1 != flagSystem2) {
            return flagSystem1 - flagSystem2;
        }
        // if (flagSystem1 == ApplicationInfo.FLAG_SYSTEM) {
        boolean package1RequestHighPriority =
                isRequestHighPriority(packageInfo1.requestedPermissions);
        boolean package2RequestHighPriority =
                isRequestHighPriority(packageInfo2.requestedPermissions);
        if (package1RequestHighPriority != package2RequestHighPriority) {
            return package1RequestHighPriority ? 1 : -1;
        }
        // }
        if (packageInfo1.firstInstallTime != packageInfo2.firstInstallTime) {
            return -Long.signum(packageInfo1.firstInstallTime - packageInfo2.firstInstallTime);
        }
        return packageInfo1.packageName.compareTo(packageInfo2.packageName);
    }

    private static boolean isRequestHighPriority(String[] array) {
        if (array == null) {
            return false;
        }
        for (String permission : array) {
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
    private static BlockingServiceConnection getServiceConnection(
            Context context, ComponentName providerComponentName) throws IOException {
        Intent advertisingIdIntent = new Intent(ADVERTISING_ID_SERVICE);
        advertisingIdIntent.setComponent(providerComponentName);

        final BlockingServiceConnection bsc = new BlockingServiceConnection();
        try {
            if (context.bindService(advertisingIdIntent, bsc, Service.BIND_AUTO_CREATE)) {
                return bsc;
            }
        } catch (Throwable e) {
            throw new IOException(e);
        }

        throw new IOException("Connection failure");
    }

    /**
     * Get the AdvertisingIdService from the blocking queue. This should wait until
     * onServiceConnected event with a 10 second timeout.
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
     * Retrieves the user's advertising ID info.
     *
     * <p>This method cannot be called in the main thread as it may block leading to ANRs. An {@code
     * IllegalStateException} will be thrown if this is called on the main thread.
     *
     * @param context Current {@link Context} (such as the current {@link android.app.Activity}).
     * @return {@link AdvertisingIdInfo} with user's advertising ID info.
     * @throws IOException           signaling connection to Ad ID providers failed.
     * @throws IllegalStateException indicating this method was called on the main thread.
     */
    public static AdvertisingIdInfo getAdvertisingIdInfo(Context context)
            throws IOException, IllegalStateException, AdvertisingIdServicesNotAvailableException {
        checkNotMainThread("Calling this from your main thread can lead to deadlock");
        final AdvertisingIdClient client = new AdvertisingIdClient(context);
        try {
            return client.getInfo();
        } finally {
            client.finish();
        }
    }
}
