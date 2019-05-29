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

package androidx.browser.trusted;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.splashscreens.SplashScreenStrategy;

/**
 * Encapsulates the steps necessary to launch a Trusted Web Activity, such as establishing a
 * connection with {@link CustomTabsService}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TwaLauncher {
    private static final String TAG = "TwaLauncher";

    private static final int DEFAULT_SESSION_ID = 96375;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    final String mProviderPackage;

    @TwaProviderPicker.LaunchMode
    private final int mLaunchMode;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final int mSessionId;

    @Nullable
    private TwaCustomTabsServiceConnection mServiceConnection;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    CustomTabsSession mSession;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    Runnable mOnSessionCreatedRunnable;

    private boolean mDestroyed;

    /**
     * Creates an instance that will automatically choose the browser to launch a TWA in.
     * If no browser supports TWA, will launch a usual Custom Tab (see {@link TwaProviderPicker}.
     */
    public TwaLauncher(Context context) {
       this(context, null);
    }

    /**
     * Same as above, but also allows to specify a browser to launch. If specified, it is assumed to
     * support TWAs.
     */
    public TwaLauncher(Context context, @Nullable String providerPackage) {
        this(context, providerPackage, DEFAULT_SESSION_ID);
    }

    /**
     * Same as above, but also accepts a session id. This allows to launch multiple TWAs in the same
     * task.
     */
    public TwaLauncher(Context context, @Nullable String providerPackage, int sessionId) {
        mContext = context;
        mSessionId = sessionId;
        if (providerPackage == null) {
            TwaProviderPicker.Action action =
                    TwaProviderPicker.pickProvider(context.getPackageManager());
            mProviderPackage = action.provider;
            mLaunchMode = action.launchMode;
        } else {
            mProviderPackage = providerPackage;
            mLaunchMode = TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY;
        }
    }

    /**
     * Opens the specified url in a TWA.
     * When TWA is already running in the current task, the url will be opened in existing TWA,
     * if the same instance TwaLauncher is used. If another instance of TwaLauncher is used,
     * the TWA will be reused only if the session ids match (see constructors).
     *
     * @param url Url to open.
     */
    public void launch(Uri url) {
        launch(new TrustedWebActivityBuilder(mContext, url), null, null);
    }

    /**
     * Similar to {@link #launch(Uri)}, but allows more customization.
     *
     * @param twaBuilder {@link TrustedWebActivityBuilder} containing the url to open, along with
     * optional parameters: status bar color, additional trusted origins, etc.
     * @param splashScreenStrategy {@link SplashScreenStrategy} to use for showing splash screens,
     * null if splash screen not needed.
     * @param completionCallback Callback triggered when the url has been opened.
     */
    public void launch(TrustedWebActivityBuilder twaBuilder,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (mDestroyed) {
            throw new IllegalStateException("TwaLauncher already destroyed");
        }

        if (mLaunchMode == TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY) {
            launchTwa(twaBuilder, splashScreenStrategy, completionCallback);
        } else {
            launchCct(twaBuilder, completionCallback);
        }
    }

    private void launchCct(TrustedWebActivityBuilder twaBuilder,
            @Nullable Runnable completionCallback) {
        // CustomTabsIntent will fall back to launching the Browser if there are no Custom Tabs
        // providers installed.
        CustomTabsIntent.Builder customTabBuilder = new CustomTabsIntent.Builder();
        Integer statusBarColor = twaBuilder.getStatusBarColor();
        if (statusBarColor != null) {
            customTabBuilder.setToolbarColor(statusBarColor);
        }
        CustomTabsIntent intent = customTabBuilder.build();

        if (mProviderPackage != null) {
            intent.intent.setPackage(mProviderPackage);
        }
        intent.launchUrl(mContext, twaBuilder.getUrl());
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void launchTwa(final TrustedWebActivityBuilder twaBuilder,
            @Nullable final SplashScreenStrategy splashScreenStrategy,
            @Nullable final Runnable completionCallback) {
        Integer statusBarColor = twaBuilder.getStatusBarColor();
        if (splashScreenStrategy != null) {
            splashScreenStrategy.onTwaLaunchInitiated(mProviderPackage, statusBarColor);
        }

        Runnable onSessionCreatedRunnable = new Runnable() {
            @Override
            public void run() {
                TwaLauncher.this.launchWhenSessionEstablished(twaBuilder, splashScreenStrategy,
                        completionCallback);
            }
        };
        if (mSession != null) {
            onSessionCreatedRunnable.run();
            return;
        }

        mOnSessionCreatedRunnable = onSessionCreatedRunnable;
        if (mServiceConnection == null) {
            mServiceConnection = new TwaCustomTabsServiceConnection();
        }
        CustomTabsClient.bindCustomTabsService(mContext, mProviderPackage,
                mServiceConnection);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void launchWhenSessionEstablished(final TrustedWebActivityBuilder twaBuilder,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable final Runnable completionCallback) {
        if (mSession == null) {
            throw new IllegalStateException("mSession is null in launchWhenSessionEstablished");
        }

        if (splashScreenStrategy != null) {
            splashScreenStrategy.configureTwaBuilder(twaBuilder, mSession,
                    new Runnable() {
                        @Override
                        public void run() {
                            TwaLauncher.this.launchWhenSplashScreenReady(twaBuilder,
                                    completionCallback);
                        }
                    });
        } else {
            launchWhenSplashScreenReady(twaBuilder, completionCallback);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void launchWhenSplashScreenReady(TrustedWebActivityBuilder builder,
            @Nullable Runnable completionCallback) {
        Log.d(TAG, "Launching Trusted Web Activity.");
        builder.launchActivity(mSession);
        // Remember who we connect to as the package that is allowed to delegate notifications
        // to us.
        TrustedWebActivityService.setVerifiedProvider(mContext, mProviderPackage);
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    /**
     * Performs clean-up.
     */
    public void destroy() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mDestroyed = true;
    }

    /**
     * Returns package name of the browser this TwaLauncher is launching.
     */
    @Nullable
    public String getProviderPackage() {
        return mProviderPackage;
    }

    class TwaCustomTabsServiceConnection extends CustomTabsServiceConnection {
        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            if (TrustedWebUtils.warmupIsRequired(mContext, mProviderPackage)) {
                client.warmup(0);
            }
            mSession = client.newSession(null, mSessionId);
            if (mOnSessionCreatedRunnable != null) {
                mOnSessionCreatedRunnable.run();
                mOnSessionCreatedRunnable = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSession = null;
        }
    }
}
