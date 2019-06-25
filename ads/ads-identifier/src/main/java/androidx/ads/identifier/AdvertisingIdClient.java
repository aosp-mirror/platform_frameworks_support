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

import static androidx.ads.identifier.AdvertisingIdUtils.getAdIdProviders;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Pair;

import androidx.ads.identifier.internal.HoldingConnectionClient;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Helper library for retrieval of advertising ID and related information such as the limit
 * advertising tracking setting.
 */
public class AdvertisingIdClient {

    private static final Object sLock = new Object();

    @SuppressLint("StaticFieldLeak") // Application Context only.
    @GuardedBy("sLock")
    @Nullable
    private static HoldingConnectionClient sHoldingConnectionClient;

    private AdvertisingIdClient() {
    }

    @WorkerThread
    private static Pair<IAdvertisingIdService, Intent> getServiceWithIntent(Context context)
            throws IOException, AdvertisingIdNotAvailableException, TimeoutException,
            InterruptedException {
        synchronized (sLock) {
            if (sHoldingConnectionClient == null) {
                sHoldingConnectionClient = new HoldingConnectionClient(context);
            }
            return sHoldingConnectionClient.getServiceWithIntent();
        }
    }

    private static AdvertisingIdInfo getAdvertisingIdInfo(
            Pair<IAdvertisingIdService, Intent> serviceWithIntent)
            throws IOException, AdvertisingIdNotAvailableException {
        IAdvertisingIdService service = serviceWithIntent.first;
        ComponentName componentName = serviceWithIntent.second.getComponent();

        try {
            String id = service.getId();
            if (id == null || id.trim().isEmpty()) {
                throw new AdvertisingIdNotAvailableException(
                        "Advertising ID provider does not returns an advertising ID.");
            }
            return AdvertisingIdInfo.builder()
                    .setId(normalizeId(id))
                    .setProviderPackageName(componentName.getPackageName())
                    .setLimitAdTrackingEnabled(service.isLimitAdTrackingEnabled())
                    .build();
        } catch (RemoteException e) {
            throw new IOException("Remote exception", e);
        } catch (RuntimeException e) {
            throw new AdvertisingIdNotAvailableException(
                    "Advertising ID provider throws a exception.", e);
        }
    }

    /**
     * Checks the Advertising ID format, if it's not in UUID format, normalizes the Advertising
     * ID to UUID format.
     *
     * @return advertising ID will in lower case format using locale {@code Locale.US};
     */
    @VisibleForTesting
    static String normalizeId(String id) {
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

    /**
     * Checks whether there is any advertising ID provider installed on the device.
     *
     * <p>This method can be called in the main thread, it does a quick check for the advertising ID
     * providers.
     * <p>Note: Even this method returns true, there still be a possibility that the
     * {@link #getAdvertisingIdInfo(Context)} method throws an exception for some reasons.
     *
     * @param context Current {@link Context} (such as the current {@link android.app.Activity}).
     * @return whether there is Advertising ID providers available on the device.
     */
    public static boolean isAdvertisingIdProviderAvailable(@NonNull Context context) {
        List<ResolveInfo> resolveInfos = getAdIdProviders(context.getPackageManager());
        return !resolveInfos.isEmpty();
    }

    /** Check that the caller is NOT calling us on the main (UI) thread. */
    private static void checkNotMainThread() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalStateException("Calling this from your main thread can lead to ANR.");
        }
    }

    @VisibleForTesting
    static void clearHoldingConnectionClient() {
        synchronized (sLock) {
            sHoldingConnectionClient = null;
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
     * @throws IOException                        signaling connection to Advertising ID providers
     *                                            failed.
     * @throws AdvertisingIdNotAvailableException indicating advertising ID is not available, like
     *                                            no advertising ID provider found or provider does
     *                                            not return an advertising ID.
     * @throws TimeoutException                   indicating connection timeout period has expired.
     * @throws InterruptedException               indicating the current thread has been
     *                                            interrupted.
     */
    @WorkerThread
    @NonNull
    public static AdvertisingIdInfo getAdvertisingIdInfo(@NonNull Context context)
            throws IOException, AdvertisingIdNotAvailableException, TimeoutException,
            InterruptedException {
        Preconditions.checkNotNull(context);
        checkNotMainThread();

        try {
            return getAdvertisingIdInfo(getServiceWithIntent(context.getApplicationContext()));
        } finally {
            synchronized (sLock) {
                if (sHoldingConnectionClient != null) {
                    sHoldingConnectionClient.scheduleAutoDisconnect();
                }
            }
        }
    }
}
