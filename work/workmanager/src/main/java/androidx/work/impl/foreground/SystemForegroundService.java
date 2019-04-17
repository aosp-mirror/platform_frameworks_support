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

package androidx.work.impl.foreground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.work.Logger;
import androidx.work.R;

/**
 * Service invoked by the {@link SystemForegroundScheduler} to process
 * {@link androidx.work.WorkRequest}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundService extends LifecycleService implements
        SystemForegroundDispatcher.SystemForegroundCallbacks {

    private static final String TAG = Logger.tagWithPrefix("SystemForegroundService");

    private int mNotificationId;
    private NotificationManager mNotificationManager;
    private SystemForegroundDispatcher mDispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationId = getResources().getInteger(R.integer.notification_id);
        mDispatcher = new SystemForegroundDispatcher(getApplicationContext());
        mDispatcher.setCallback(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            updateNotification();
            mDispatcher.onStartCommand(intent);
        }
        // If the service were to crash, we want all unacknowledged Intents to get redelivered.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.onDestroy();
    }

    @Override
    public void onCompleted() {
        Logger.get().debug(TAG, "All commands completed.");
        // No need to pass in startId; stopSelf() translates to stopSelf(-1) which is a hard stop
        // of all startCommands. This is the behavior we want.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        stopSelf();
    }

    void updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
            // Create channel if necessary
            String channelId = getString(R.string.channel_id);
            String title = getString(R.string.notification_title);
            String content = getString(R.string.notification_content);

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentText(title)
                    .setTicker(title)
                    .setContentText(content)
                    .setSmallIcon(R.drawable.ic_work_notification)
                    .setOngoing(true)
                    .build();

            startForeground(mNotificationId, notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        String id = getString(R.string.channel_id);
        String name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = getResources().getInteger(R.integer.channel_importance);
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        mNotificationManager.createNotificationChannel(channel);
    }
}
