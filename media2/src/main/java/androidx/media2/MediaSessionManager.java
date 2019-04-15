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
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import androidx.media.MediaBrowserServiceCompat;
import androidx.versionedparcelable.ParcelImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides support for interacting with media sessions that applications have published
 * in order to express their ongoing media playback state.
 *
 * @see MediaSessionCompat
 * @see MediaSession
 * @see MediaSessionService
 * @see MediaLibraryService
 * @see MediaControllerCompat
 * @see MediaController
 * @see MediaBrowser
 */
@RequiresApi(28)
public final class MediaSessionManager {
    static final String TAG = "MediaSessionManager";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    static final String ACTION_SESSION_TOKENS_CHANGED =
            "androidx.media2.action.SESSION_TOKENS_CHANGED";
    static final String ACTION_SESSION_TOKENS_CHANGED_LISTENER_ADDED =
            "androidx.media2.action.SESSION_TOKENS_CHANGED_LISTENER_ADDED";

    static final String EXTRA_PID = "androidx.media2.extras.PID";
    static final String EXTRA_PACKAGE_NAME = "androidx.media2.extras.PACKAGE_NAME";
    static final String EXTRA_SESSION_TOKENS = "androidx.media2.extras.SESSION_TOKENS";

    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static MediaSessionManager sInstance;

    final Context mContext;
    final SessionTokensBroadcastReceiver mSessionTokensBroadcastReceiver =
            new SessionTokensBroadcastReceiver();
    final Object mLock = new Object();
    @GuardedBy("mLock")
    final Map<OnSessionTokensChangedListener, Handler> mListeners = new ArrayMap<>();

    /**
     * Session tokens from this process.
     */
    @GuardedBy("mLock")
    final List<SessionToken> mLocalSessionTokens = new ArrayList<>();

    /**
     * Session tokens from other processes.
     * Maps the remote process's pid to the process's session tokens.
     */
    @GuardedBy("mLock")
    final Map<Integer, List<SessionToken>> mRemoteSessionTokens = new ArrayMap<>();

    /**
     * Gets an instance of MediaSessionManager associated with the context.
     *
     * @return the MediaSessionManager instance for this context.
     */
    public static @NonNull MediaSessionManager getInstance(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new MediaSessionManager(context.getApplicationContext());
            }
            return sInstance;
        }
    }

    private MediaSessionManager(Context context) {
        mContext = context;
    }

    /**
     * Gets {@link Set} of {@link SessionToken} for {@link MediaSessionService} regardless of
     * their activeness. This list represents media apps that support background playback.
     *
     * @return set of tokens
     */
    public @NonNull Set<SessionToken> getSessionServiceTokens() {
        ArraySet<SessionToken> sessionServiceTokens = new ArraySet<>();
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> services = new ArrayList<>();
        // If multiple actions are declared for a service, browser gets higher priority.
        List<ResolveInfo> libraryServices = pm.queryIntentServices(
                new Intent(MediaLibraryService.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
        if (libraryServices != null) {
            services.addAll(libraryServices);
        }
        List<ResolveInfo> sessionServices = pm.queryIntentServices(
                new Intent(MediaSessionService.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
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
            SessionToken token = new SessionToken(mContext,
                    new ComponentName(serviceInfo.packageName, serviceInfo.name));
            sessionServiceTokens.add(token);
        }
        if (DEBUG) {
            Log.d(TAG, "Found " + sessionServiceTokens.size() + " session services");
            for (SessionToken token : sessionServiceTokens) {
                Log.d(TAG, "   " + token);
            }
        }
        return sessionServiceTokens;
    }

    /**
     * TODO: Add appropriate Javadoc
     *
     * @param listener The listener to add
     * @param handler The handler to call listener on.
     */
    public void addOnSessionTokensChangedListener(
            @NonNull OnSessionTokensChangedListener listener, @NonNull Handler handler) {
        if (listener == null) {
            throw new IllegalArgumentException("listener shouldn't be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler shouldn't be null");
        }
        synchronized (mLock) {
            mListeners.put(listener, handler);
            if (mListeners.size() == 1) {
                // No listeners have existed in this process.
                // Register a broadcast receiver for make the listener work.
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_SESSION_TOKENS_CHANGED);
                filter.addAction(ACTION_SESSION_TOKENS_CHANGED_LISTENER_ADDED);
                mContext.registerReceiver(mSessionTokensBroadcastReceiver, filter);

                // TODO: Send broadcast ACTION_SESSION_TOKENS_CHANGED_LISTENER_ADDED here.
            }
        }
    }

    /**
     * Removes the {@link OnSessionTokensChangedListener} to stop receiving session token updates.
     *
     * @param listener The listener to remove.
     */
    public void removeOnSessionTokensChangedListener(
            @NonNull OnSessionTokensChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener shouldn't be null");
        }
        synchronized (mLock) {
            mListeners.remove(listener);
            if (mListeners.isEmpty()) {
                // All listeners are removed. No need to listen to the session changes from
                // remote processes.
                mContext.unregisterReceiver(mSessionTokensBroadcastReceiver);
                mRemoteSessionTokens.clear();
            }
        }
    }

    /**
     * Adds a session token of newly created {@link MediaSession}.
     * Should be only called by the constructor of the {@link MediaSessionImplBase}.
     *
     * @param token the token to add
     */
    void addLocalSessionToken(@NonNull SessionToken token) {
        if (token == null || !(token.getType() == SessionToken.TYPE_SESSION)) {
            if (DEBUG) {
                Log.d(TAG, "addLocalSessionToken: Ignoring invalid token=" + token);
            }
            return;
        }
        synchronized (mLock) {
            mLocalSessionTokens.add(token);
            broadcastLocalSessionTokensChangedLocked(null);
        }
    }

    /**
     * Removes a session token of {@link MediaSession} which is being closed.
     * Should be only called by the {@link MediaSessionImplBase#close()}.
     *
     * @param token the token to remove
     */
    void removeLocalSessionToken(@NonNull SessionToken token) {
        if (token == null || !(token.getType() == SessionToken.TYPE_SESSION)) {
            if (DEBUG) {
                Log.d(TAG, "removeLocalSessionToken: Ignoring invalid token=" + token);
            }
            return;
        }
        synchronized (mLock) {
            mLocalSessionTokens.remove(token);
            broadcastLocalSessionTokensChangedLocked(null);
        }
    }

    /**
     * Broadcasts intents with extras including the list of session tokens in this process.
     * When {@code receiverPackageName} is specified, this intent is sent only to the package.
     *
     * @param receiverPackageName A package name to send the broadcast to, or {@code null} for
     *                            sending regardless of the package name.
     */
    @GuardedBy("mLock")
    private void broadcastLocalSessionTokensChangedLocked(@Nullable String receiverPackageName) {
        Intent intent = new Intent(ACTION_SESSION_TOKENS_CHANGED);
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_PID, Process.myPid());
        ArrayList<ParcelImpl> tokens = new ArrayList<>();
        for (int i = 0; i < mLocalSessionTokens.size(); i++) {
            tokens.add(MediaUtils.toParcelable(mLocalSessionTokens.get(i)));
        }
        extras.putParcelableArrayList(EXTRA_SESSION_TOKENS, tokens);
        intent.putExtras(intent);

        if (receiverPackageName != null) {
            // Send broadcast to receivers only in this package.
            intent.setPackage(receiverPackageName);
        }
        mContext.sendBroadcast(intent);
    }

    /**
     * Called when the other process's session tokens are changed.
     * This method updates the list of whole tokens, and calls
     * {@link OnSessionTokensChangedListener#onSessionTokensChanged(List)} with the list.
     *
     * @param extras intent extras sent from the process whose tokens are changed.
     */
    void handleSessionTokensChangedAction(@NonNull Bundle extras) {
        final List<ParcelImpl> tokenParcelImplsSent =
                extras.getParcelableArrayList(EXTRA_SESSION_TOKENS);
        if (tokenParcelImplsSent == null) {
            if (DEBUG) {
                Log.d(TAG, "handleSessionTokensChangedAction: Ignoring null token list.");
            }
            return;
        }
        final List<SessionToken> tokensSent = new ArrayList<>();
        for (ParcelImpl parcelImpl : tokenParcelImplsSent) {
            SessionToken token = MediaUtils.fromParcelable(parcelImpl);
            if (token != null) {
                tokensSent.add(token);
            }
        }

        final int pid = extras.getInt(EXTRA_PID);
        synchronized (mLock) {
            // Update the whole token list.
            mRemoteSessionTokens.put(pid, tokensSent);
            final List<SessionToken> wholeTokens = new ArrayList<>();
            for (List<SessionToken> tokensPerProcess : mRemoteSessionTokens.values()) {
                wholeTokens.addAll(tokensPerProcess);
            }

            // Notify the local listeners that the tokens are changed.
            for (final OnSessionTokensChangedListener listener : mListeners.keySet()) {
                Handler handler = mListeners.get(listener);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSessionTokensChanged(wholeTokens);
                    }
                });
            }
        }
    }

    /**
     * Called when an other process added the first listener for listening changes in
     * session tokens.
     *
     * @param extras intent extras sent from the process which added the listener.
     */
    void handleListenerAddedAction(@NonNull Bundle extras) {
        // Reply to the app with the list of current SessionTokens in this process.
        String listenerPackageName = extras.getString(EXTRA_PACKAGE_NAME);
        if (TextUtils.isEmpty(listenerPackageName)) {
            if (DEBUG) {
                Log.d(TAG, "handleListenerAddedAction: Ignoring empty package name.");
            }
            return;
        }
        synchronized (mLock) {
            broadcastLocalSessionTokensChangedLocked(listenerPackageName);
        }
    }

    /**
     * TODO: Add appropriate Javadoc
     */
    public interface OnSessionTokensChangedListener {
        /**
         * TODO: Add appropriate Javadoc
         *
         * @param tokens list of {@link SessionToken}
         */
        void onSessionTokensChanged(@NonNull List<SessionToken> tokens);
    }

    class SessionTokensBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!checkIntent(intent)) {
                return;
            }

            Bundle extras = intent.getExtras();
            switch (intent.getAction()) {
                case ACTION_SESSION_TOKENS_CHANGED:
                    handleSessionTokensChangedAction(extras);
                    break;
                case ACTION_SESSION_TOKENS_CHANGED_LISTENER_ADDED:
                    handleListenerAddedAction(extras);
                    break;
                default:
                    // Do nothing.
                    break;
            }
        }

        /**
         * Returns whether the intent is as expected.
         * The intent should contain proper action and non-null extras.
         * The extras should contain the sender's pid which is different from this process.
         */
        private boolean checkIntent(@Nullable Intent intent) {
            if (intent == null) {
                if (DEBUG) {
                    Log.d(TAG, "onReceive: Ignoring null intent.");
                }
                return false;
            }

            String action = intent.getAction();
            if (!TextUtils.equals(ACTION_SESSION_TOKENS_CHANGED, action)
                    && !TextUtils.equals(ACTION_SESSION_TOKENS_CHANGED_LISTENER_ADDED, action)) {
                if (DEBUG) {
                    Log.d(TAG, "onReceive: Ignoring the intent with invalid action=" + action);
                }
                return false;
            }

            Bundle extras = intent.getExtras();
            if (extras == null || !extras.containsKey(EXTRA_PID)) {
                if (DEBUG) {
                    Log.d(TAG, "onReceive: Ignoring the intent with invalid extras. action="
                            + intent.getAction());
                }
                return false;
            }

            if (extras.getInt(EXTRA_PID) == Process.myPid()) {
                if (DEBUG) {
                    Log.d(TAG, "onReceive: Ignoring the intent from the own process. action="
                            + intent.getAction() + ", pid=" + extras.getInt(EXTRA_PID));
                }
                return false;
            }

            return true;
        }
    }
}
