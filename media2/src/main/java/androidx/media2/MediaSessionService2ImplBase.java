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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;
import androidx.media.MediaBrowserServiceCompat;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media2.MediaSessionService2.MediaNotification;
import androidx.media2.MediaSessionService2.MediaSessionService2Impl;

/**
 * Implementation of {@link MediaSessionService2}.
 */
class MediaSessionService2ImplBase implements MediaSessionService2Impl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    // TODO: Should we make this ID unique per app? If so, how can we do that?
    private static final String NOTIFICATION_CHANNEL_ID = "DemoChannelId";
    private static final int NOTIFICATION_ID = 1001;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaSession2 mSession;
    private NotificationManager mNotificationManager;

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

        mNotificationManager = (NotificationManager)
                mSession.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        session.getCallback().setOnHandleForegroundServiceListener(
                new MediaSession2.SessionCallback.OnHandleForegroundServiceListener() {
                    @Override
                    public void onHandleForegroundService(int state) {
                        if (state == MediaPlayerConnector.PLAYER_STATE_IDLE
                                || state == MediaPlayerConnector.PLAYER_STATE_ERROR) {
                            service.stopForeground(false /* removeNotification */);
                            return;
                        }

                        // state is PLAYER_STATE_PLAYING or PLAYER_STATE_PAUSE.
                        MediaNotification mediaNotification = service.onUpdateNotification();
                        if (mediaNotification == null) {
                            return;
                        }

                        int notificationId = mediaNotification.getNotificationId();
                        Notification notification = mediaNotification.getNotification();

                        mNotificationManager.notify(notificationId, notification);
                        service.startForeground(notificationId, notification);
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
    public MediaNotification onUpdateNotification() {
        if (shouldCreateNotificationChannel()) {
            createNotificationChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mSession.getContext(), NOTIFICATION_CHANNEL_ID);


        MediaStyle mediaStyle = new MediaStyle()
//                .setCancelButtonIntent(stopPendingIntent)
                .setMediaSession(mSession.getSessionCompat().getSessionToken())
//                .setShowActionsInCompactView(0, 1, 2 /* What action should we show? */)
                .setShowCancelButton(true);

        MediaMetadata2 metadata;
        if (mSession.getCurrentMediaItem() != null) {
            metadata = mSession.getCurrentMediaItem().getMetadata();
        }

        Notification notification = builder
//                .setContentIntent(controller.sessionActivity)
//                .setContentText(description.subtitle)
//                .setContentTitle(description.title)
//                .setDeleteIntent(stopPendingIntent)
//                .setLargeIcon(description.iconBitmap)
//                .setOnlyAlertOnce(true)
//                .setSmallIcon(R.drawable.music_note_icon?)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        return new MediaNotification(NOTIFICATION_ID, notification);
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

    private boolean shouldCreateNotificationChannel() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !notificationChannelExists();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private boolean notificationChannelExists() {
        return mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        // TODO: What Importance should we use?
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "Now Playing", NotificationManager.IMPORTANCE_LOW);
        mNotificationManager.createNotificationChannel(channel);
    }
}
