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

import static android.app.Service.START_STICKY;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.GuardedBy;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaSessionService2.MediaNotification;
import androidx.media2.MediaSessionService2.MediaSessionService2Impl;

/**
 * Implementation of {@link MediaSessionService2}.
 */
class MediaSessionService2ImplBase implements MediaSessionService2Impl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaSession2 mSession;
    private MediaNotificationHandler mNotificationHandler;

    MediaSessionService2ImplBase() {
    }

    @Override
    public void onCreate(final MediaSessionService2 service) {
        SessionToken2 token = new SessionToken2(service,
                new ComponentName(service, service.getClass().getName()));
        if (token.getType() != getSessionType()) {
            throw new RuntimeException("Expected session type " + getSessionType()
                    + " but was " + token.getType());
        }
        MediaSession2 session = service.onCreateSession(token.getId());
        synchronized (mLock) {
            mSession = session;
            if (mSession == null || !token.getId().equals(mSession.getToken().getId())
                    || mSession.getToken().getType() != getSessionType()) {
                mSession = null;
                throw new RuntimeException("Expected session with id " + token.getId()
                        + " and type " + token.getType() + ", but got " + mSession);
            }
        }

        final MediaNotificationHandler notificationHandler = new MediaNotificationHandler(service);
        mNotificationHandler = notificationHandler;

        session.getCallback().setOnHandleForegroundServiceListener(
                new MediaSession2.SessionCallback.OnHandleForegroundServiceListener() {
                    @Override
                    public void onHandleForegroundService(int state) {
                        notificationHandler.onHandleForegroundService(state);
                    }
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        final MediaSession2 session = getSession();
        if (session == null) {
            Log.w(TAG, "Session hasn't created");
            return null;
        }
        switch (intent.getAction()) {
            case MediaSessionService2.SERVICE_INTERFACE:
                return session.getSessionBinder();
            case MediaBrowserServiceCompat.SERVICE_INTERFACE:
                return session.getLegacyBrowerServiceBinder();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        switch (intent.getAction()) {
            case Intent.ACTION_MEDIA_BUTTON: {
                final MediaSession2 session = getSession();
                if (session == null) {
                    Log.w(TAG, "Session hasn't created");
                    return START_STICKY;
                }
                KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ke != null) {
                    session.getSessionCompat().getController().dispatchMediaButtonEvent(ke);
                }
                break;
            }
        }
        return START_STICKY;
    }

    @Override
    public MediaNotification onUpdateNotification() {
        return mNotificationHandler.onUpdateNotification();
    }

    @Override
    public MediaSession2 getSession() {
        synchronized (mLock) {
            return mSession;
        }
    }

    @Override
    public int getSessionType() {
        return SessionToken2.TYPE_SESSION_SERVICE;
    }
}
