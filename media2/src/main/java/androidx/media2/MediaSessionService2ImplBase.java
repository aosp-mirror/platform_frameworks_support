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

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaSessionService2.MediaNotification;
import androidx.media2.MediaSessionService2.MediaSessionService2Impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link MediaSessionService2}.
 */
class MediaSessionService2ImplBase implements MediaSessionService2Impl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private Map<String, MediaSession2> mSessions = new ArrayMap<>();

    MediaSessionService2ImplBase() {
    }

    @Override
    public IBinder onBind(final MediaSessionService2 service, Intent intent) {
        String action = intent.getAction();
        if (!TextUtils.equals(MediaSessionService2.SERVICE_INTERFACE, action)
                && !TextUtils.equals(MediaBrowserServiceCompat.SERVICE_INTERFACE, action)) {
            return null;
        }
        final MediaSession2 session = service.onGetSession();
        addSession(session);
        // TODO: Check whether the session is registered in multiple sessions.
        session.getCallback().setOnHandleForegroundServiceCallback(
                new MediaSession2.SessionCallback.OnHandleForegroundServiceCallback() {
                    @Override
                    public void onPlayerStateChanged(int state) {
                        if (state == MediaPlayerConnector.PLAYER_STATE_IDLE
                                || state == MediaPlayerConnector.PLAYER_STATE_ERROR) {
                            service.stopForeground(false /* removeNotification */);
                            return;
                        }
                        // state is PLAYER_STATE_PLAYING or PLAYER_STATE_PAUSE.
                        MediaNotification mediaNotification = service.onUpdateNotification(session);
                        if (mediaNotification == null) {
                            return;
                        }

                        int notificationId = mediaNotification.getNotificationId();
                        Notification notification = mediaNotification.getNotification();

                        NotificationManagerCompat manager = NotificationManagerCompat.from(service);
                        manager.notify(notificationId, notification);
                        service.startForeground(notificationId, notification);
                    }

                    @Override
                    public void onSessionClosed() {
                        removeSession(session);
                    }
                });

        switch (action) {
            case MediaSessionService2.SERVICE_INTERFACE:
                return session.getSessionBinder();
            case MediaBrowserServiceCompat.SERVICE_INTERFACE:
                return session.getLegacyBrowerServiceBinder();
        }
        return null;
    }

    @Override
    public void addSession(MediaSession2 session) {
        if (session == null) {
            throw new IllegalArgumentException("session shouldn't be null");
        }
        synchronized (mLock) {
            MediaSession2 old = mSessions.get(session.getId());
            if (old != null && old != session) {
                // TODO(b/112114183): Also check the uniqueness before sessions're returned by
                //                    onGetSession
                throw new IllegalArgumentException("Session ID should be unique.");
            }
            mSessions.put(session.getId(), session);
        }
    }

    @Override
    public void removeSession(MediaSession2 session) {
        if (session == null) {
            throw new IllegalArgumentException("session shouldn't be null");
        }
        synchronized (mLock) {
            mSessions.remove(session.getId());
        }
    }

    @Override
    public MediaNotification onUpdateNotification(MediaSession2 session) {
        // May supply default implementation later
        return null;
    }

    @Override
    public List<MediaSession2> getSessions() {
        List<MediaSession2> list = new ArrayList<>();
        synchronized (mLock) {
            list.addAll(mSessions.values());
        }
        return list;
    }
}
