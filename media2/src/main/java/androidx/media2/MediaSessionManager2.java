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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides support for interacting with media sessions that applications have published to express
 * their ongoing media playback state.
 *
 * @see MediaSessionCompat
 * @see MediaSession2
 * @see MediaSessionService2
 * @see MediaLibraryService2
 * @see MediaControllerCompat
 * @see MediaController2
 * @see MediaBrowser2
 */
public final class MediaSessionManager2 {
    static final String TAG = "MediaSessionManager2";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Object sLock = new Object();
    private static volatile MediaSessionManager sSessionManager1;
    private static volatile MediaSessionManager2 sSessionManager2;

    private final Object mLock = new Object();
    private final Context mContext;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private final ArraySet<SessionToken2> mSessionServiceTokens = new ArraySet<>();

    /**
     * Gets an instance of the media session manager associated with the context.
     *
     * @return The MediaSessionManager instance for this context.
     */
    public static @NonNull MediaSessionManager2 getSessionManager2(@NonNull Context context) {
        sSessionManager1 = MediaSessionManager.getSessionManager(context);
        MediaSessionManager2 manager2 = sSessionManager2;
        if (manager2 == null) {
            synchronized (sLock) {
                manager2 = sSessionManager2;
                if (manager2 == null) {
                    sSessionManager2 = new MediaSessionManager2(context.getApplicationContext());
                    manager2 = sSessionManager2;
                }
            }
        }
        return manager2;
    }

    private MediaSessionManager2(Context context) {
        mContext = context;
        mHandler = new Handler();
        registerPackageBroadcastReceivers();
        buildMediaSessionService2List();
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
        return sSessionManager1.isTrustedForMediaControl(userInfo);
    }

    /**
     * Get {@link List} of {@link SessionToken2} for {@link MediaSessionService2} regardless of
     * their activeness. This list represents media apps that support background playback.
     *
     * @return list of tokens
     */
    public List<SessionToken2> getSessionServiceTokens() {
        return new ArrayList<>(mSessionServiceTokens);
    }

    @SuppressWarnings("FallThrough")
    private void registerPackageBroadcastReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGES_SUSPENDED);
        filter.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);

        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Check if the package is replacing (i.e. reinstalling)
                final boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

                if (DEBUG) {
                    Log.d(TAG, "Received change in packages, intent=" + intent);
                }
                switch (intent.getAction()) {
                    case Intent.ACTION_PACKAGE_ADDED:
                    case Intent.ACTION_PACKAGE_REMOVED:
                    case Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE:
                    case Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE:
                        if (isReplacing) {
                            // Ignore if the package(s) are replacing. In that case, followings will
                            // happen in order.
                            //    1. ACTION_PACKAGE_REMOVED with isReplacing=true
                            //    2. ACTION_PACKAGE_ADDED with isReplacing=true
                            //    3. ACTION_PACKAGE_REPLACED
                            //    (Note that ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE and
                            //     ACTION_EXTERNAL_APPLICATIONS_AVAILABLE will be also called with
                            //     isReplacing=true for both ASEC hosted packages and packages in
                            //     external storage)
                            // Since we only want to update session service list once, ignore
                            // actions above when replacing.
                            // Replacing will be handled only once with the ACTION_PACKAGE_REPLACED.
                            break;
                        }
                        // fall-through
                    case Intent.ACTION_PACKAGE_CHANGED:
                    case Intent.ACTION_PACKAGES_SUSPENDED:
                    case Intent.ACTION_PACKAGES_UNSUSPENDED:
                    case Intent.ACTION_PACKAGE_REPLACED:
                        buildMediaSessionService2List();
                }
            }
        }, filter, null, mHandler);
    }

    void buildMediaSessionService2List() {
        if (DEBUG) {
            Log.d(TAG, "buildMediaSessionService2List");
        }
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
        synchronized (mLock) {
            // List to keep the session services that need be removed because they don't exist
            // in the 'services' above.
            boolean sessionTokensUpdated = false;

            ArraySet<SessionToken2> sessionTokensToRemove = new ArraySet<>(mSessionServiceTokens);
            for (ResolveInfo service : services) {
                if (service == null || service.serviceInfo == null) {
                    continue;
                }
                ServiceInfo serviceInfo = service.serviceInfo;
                SessionToken2 token = new SessionToken2(mContext,
                        new ComponentName(serviceInfo.packageName, serviceInfo.name));
                // If the token already exists, keep it in the mSessions by removing from
                // sessionTokensToRemove.
                if (!sessionTokensToRemove.remove(token)) {
                    // New session service is found.
                    sessionTokensUpdated |= mSessionServiceTokens.add(token);
                }
            }
            for (SessionToken2 token : sessionTokensToRemove) {
                sessionTokensUpdated |= mSessionServiceTokens.remove(token);
            }

            if (sessionTokensUpdated) {
                // TODO: Introduce session token listener.
            }
            if (DEBUG) {
                Log.d(TAG, "Found " + mSessionServiceTokens.size() + " session services");
                for (SessionToken2 token : mSessionServiceTokens) {
                    Log.d(TAG, "   " + token);
                }
            }
        }
    }
}
