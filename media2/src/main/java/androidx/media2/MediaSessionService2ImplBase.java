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

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;
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
    // TODO: Change this dummy res id into real string resource id.
    private static final int DUMMY_RES_ID_FOR_TITLE = R.string.status_bar_notification_info_overflow;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private MediaSession2 mSession;
    private MediaSessionService2 mInstance;
    private ComponentName mServiceName;
    private NotificationManager mNotificationManager;

    private NotificationCompat.Action mPlayAction;
    private NotificationCompat.Action mPauseAction;
    private NotificationCompat.Action mSkipToPrevAction;
    private NotificationCompat.Action mSkipToNextAction;

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

        mInstance = service;
        mServiceName = new ComponentName(mInstance.getPackageName(),
                mInstance.getClass().getCanonicalName());
        mNotificationManager = (NotificationManager)
                mInstance.getSystemService(Context.NOTIFICATION_SERVICE);

        mPlayAction = createNotificationAction(R.drawable.ic_play,
                DUMMY_RES_ID_FOR_TITLE, ACTION_PLAY);
        mPauseAction = createNotificationAction(R.drawable.ic_pause,
                DUMMY_RES_ID_FOR_TITLE, ACTION_PAUSE);
        mSkipToPrevAction = createNotificationAction(R.drawable.ic_skip_to_previous,
                DUMMY_RES_ID_FOR_TITLE, ACTION_SKIP_TO_PREVIOUS);
        mSkipToNextAction = createNotificationAction(R.drawable.ic_skip_to_next,
                DUMMY_RES_ID_FOR_TITLE, ACTION_SKIP_TO_NEXT);

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
                mInstance, NOTIFICATION_CHANNEL_ID);

        // TODO: Filter actions when SessionPlayer#getSupportedActions() is introduced.
        builder.addAction(mSkipToPrevAction);
        if (mSession.getPlayerState() == MediaPlayerConnector.PLAYER_STATE_PLAYING) {
            builder.addAction(mPauseAction);
        } else {
            builder.addAction(mPlayAction);
        }
        builder.addAction(mSkipToNextAction);

        // Set metadata info in the notification.
        if (mSession.getCurrentMediaItem() != null) {
            MediaMetadata2 metadata = mSession.getCurrentMediaItem().getMetadata();
            if (metadata != null) {
                builder.setContentTitle(metadata.getText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE))
                        .setContentText(metadata.getText(
                                MediaMetadata2.METADATA_KEY_DISPLAY_SUBTITLE))
                        .setLargeIcon(metadata.getBitmap(MediaMetadata2.METADATA_KEY_ALBUM_ART));
            }
        }

        MediaStyle mediaStyle = new MediaStyle()
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mInstance, mServiceName, ACTION_STOP))
                .setMediaSession(mSession.getSessionCompat().getSessionToken())
                .setShowActionsInCompactView(1 /* Show play/pause button only in compact view */)
                .setShowCancelButton(true /* For pre-lollipop */);

        Notification notification = builder
                .setContentIntent(mSession.getImpl().getSessionActivity())
                .setDeleteIntent(createPendingIntent(ACTION_STOP))
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_music_note)
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

    private NotificationCompat.Action createNotificationAction(int iconResId, int titleResId,
            @PlaybackStateCompat.Actions long action) {
        CharSequence title = mInstance.getResources().getText(titleResId);
        return new NotificationCompat.Action(iconResId, title, createPendingIntent(action));
    }

    private PendingIntent createPendingIntent(@PlaybackStateCompat.Actions long action) {
        return MediaButtonReceiver.buildMediaButtonPendingIntent(mInstance, mServiceName, action);
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
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "Now Playing", NotificationManager.IMPORTANCE_LOW);
        mNotificationManager.createNotificationChannel(channel);
    }
}
