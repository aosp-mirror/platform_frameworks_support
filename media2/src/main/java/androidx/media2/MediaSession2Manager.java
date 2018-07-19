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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides support for interacting with media sessions that applications have published
 * in order to express their ongoing media playback state.
 *
 * @see MediaSessionCompat
 * @see MediaSession2
 * @see MediaSessionService2
 * @see MediaLibraryService2
 * @see MediaControllerCompat
 * @see MediaController2
 * @see MediaBrowser2
 */
@RequiresApi(28)
public final class MediaSession2Manager {
    static final String TAG = "MediaSession2Manager";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static MediaSessionManager sSessionManager;
    @GuardedBy("sLock")
    private static MediaSession2Manager sSession2Manager;

    private final Context mContext;

    /**
     * Gets an instance of MediaSession2Manager associated with the context.
     *
     * @return the MediaSession2Manager instance for this context.
     */
    public static @NonNull MediaSession2Manager getSession2Manager(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        synchronized (sLock) {
            sSessionManager = MediaSessionManager.getSessionManager(context);
            if (sSession2Manager == null) {
                sSession2Manager = new MediaSession2Manager(context.getApplicationContext());
            }
            return sSession2Manager;
        }
    }

    private MediaSession2Manager(Context context) {
        mContext = context;
    }

    /**
     * Checks whether the remote user is a trusted app.
     * <p>
     * An app is trusted if the app holds the android.Manifest.permission.MEDIA_CONTENT_CONTROL
     * permission or has an enabled notification listener.
     *
     * @param userInfo The remote user info from either
     *            {@link MediaSessionCompat#getCurrentControllerInfo()} and
     *            {@link MediaBrowserServiceCompat#getCurrentBrowserInfo()}.
     * @return {@code true} if the remote user is trusted and its package name matches with the UID.
     *            {@code false} otherwise.
     */
    public boolean isTrustedForMediaControl(@NonNull MediaSessionManager.RemoteUserInfo userInfo) {
        MediaSessionManager sessionManager;
        synchronized (sLock) {
            sessionManager = sSessionManager;
        }
        return sessionManager.isTrustedForMediaControl(userInfo);
    }

    /**
     * Gets {@link List} of {@link SessionToken2} for {@link MediaSessionService2} regardless of
     * their activeness. This list represents media apps that support background playback.
     *
     * @return list of tokens
     */
    public @NonNull List<SessionToken2> getSessionServiceTokens() {
        ArraySet<SessionToken2> sessionServiceTokens = new ArraySet<>();
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> services = new ArrayList<>();
        // If multiple actions are declared for a service, browser gets higher priority.
        List<ResolveInfo> libraryServices = pm.queryIntentServices(
                new Intent(MediaLibraryService2.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
        if (libraryServices != null) {
            services.addAll(libraryServices);
        }
        List<ResolveInfo> sessionServices = pm.queryIntentServices(
                new Intent(MediaSessionService2.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
        if (sessionServices != null) {
            services.addAll(sessionServices);
        }
        List<ResolveInfo> browserServices = pm.queryIntentServices(
                new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        if (browserServices != null) {
            services.addAll(browserServices);
        }

        for (ResolveInfo service : services) {
            if (service == null || service.serviceInfo == null) {
                continue;
            }
            ServiceInfo serviceInfo = service.serviceInfo;
            SessionToken2 token = new SessionToken2(mContext,
                    new ComponentName(serviceInfo.packageName, serviceInfo.name));
            sessionServiceTokens.add(token);
        }
        if (DEBUG) {
            Log.d(TAG, "Found " + sessionServiceTokens.size() + " session services");
            for (SessionToken2 token : sessionServiceTokens) {
                Log.d(TAG, "   " + token);
            }
        }
        return new ArrayList<>(sessionServiceTokens);
    }
}
