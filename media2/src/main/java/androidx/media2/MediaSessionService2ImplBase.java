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

    private static final String NOTIFICATION_CHANNEL_ID = "default_channel_id";
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private MediaSession2 mSession;
    private MediaSessionService2 mInstance;
    private NotificationManager mNotificationManager;

    private int mNotificationId;
    private String mNotificationChannelName;

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
        final NotificationManager notificationManager = (NotificationManager)
                mInstance.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager = notificationManager;

        mNotificationId = mInstance.getClass().hashCode();
        mNotificationChannelName = mInstance.getResources().getString(
                R.string.default_notification_channel_name);

        mPlayAction = createNotificationAction(R.drawable.ic_play,
                R.string.play, ACTION_PLAY);
        mPauseAction = createNotificationAction(R.drawable.ic_pause,
                R.string.pause, ACTION_PAUSE);
        mSkipToPrevAction = createNotificationAction(R.drawable.ic_skip_to_previous,
                R.string.skip_to_previous_item, ACTION_SKIP_TO_PREVIOUS);
        mSkipToNextAction = createNotificationAction(R.drawable.ic_skip_to_next,
                R.string.skip_to_next_item, ACTION_SKIP_TO_NEXT);

        session.getCallback().setOnHandleForegroundServiceListener(
                new MediaSession2.SessionCallback.OnHandleForegroundServiceListener() {
                    @Override
                    public void onHandleForegroundService(int state) {
                        MediaNotification mediaNotification = service.onUpdateNotification();
                        if (mediaNotification == null) {
                            // The service implementation doesn't want to use the automatic
                            // start/stopForeground feature.
                            return;
                        }

                        if (state == MediaPlayerConnector.PLAYER_STATE_IDLE
                                || state == MediaPlayerConnector.PLAYER_STATE_ERROR) {
                            service.stopForeground(true /* removeNotification */);
                            return;
                        }

                        int id = mediaNotification.getNotificationId();
                        Notification notification = mediaNotification.getNotification();

                        if (state == MediaPlayerConnector.PLAYER_STATE_PAUSED) {
                            // Calling stopForeground(true) is a workaround for pre-L devices
                            // which prevents the media notification from being undismissable.
                            boolean shouldRemoveNotification =
                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
                            service.stopForeground(shouldRemoveNotification);
                            notificationManager.notify(id, notification);
                        } else {
                            service.startForeground(mediaNotification.getNotificationId(),
                                    mediaNotification.getNotification());
                        }
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
                CharSequence title = metadata.getText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE);
                if (title == null) {
                    title = metadata.getText(MediaMetadata2.METADATA_KEY_TITLE);
                }
                builder.setContentTitle(title)
                        .setContentText(metadata.getText(MediaMetadata2.METADATA_KEY_ARTIST))
                        .setLargeIcon(metadata.getBitmap(MediaMetadata2.METADATA_KEY_ALBUM_ART));
            }
        }

        MediaStyle mediaStyle = new MediaStyle()
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mInstance, ACTION_STOP))
                .setMediaSession(mSession.getSessionCompat().getSessionToken())
                .setShowActionsInCompactView(1 /* Show play/pause button only in compact view */);

        Notification notification = builder
                .setContentIntent(mSession.getImpl().getSessionActivity())
                .setDeleteIntent(createPendingIntent(ACTION_STOP))
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_music_note)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .build();

        return new MediaNotification(mNotificationId, notification);
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
        // TODO: This is not the service name, but mbr component name?
        return MediaButtonReceiver.buildMediaButtonPendingIntent(mInstance, action);
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
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                mNotificationChannelName, NotificationManager.IMPORTANCE_LOW);
        mNotificationManager.createNotificationChannel(channel);
    }
}
