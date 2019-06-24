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
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

/**
 * A {@link ListenableWorker} which is scheduled via a Foreground {@link android.app.Service}.
 */
public abstract class ForegroundWorker extends ListenableWorker {
    public ForegroundWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    /**
     * @return the {@link Notification} to be shown to the user when the Foreground
     * {@link android.app.Service} runs the {@link ForegroundWorker}.
     *
     * This {@link Notification} must have a priority of at least {@code NotificationCompat
     * #PRIORITY_LOW}.
     */
    @NonNull
    public abstract Notification buildNotification();

    /**
     * @return a {@link Notification} id. This {@code id} must *not* be {@code 0}.
     * For more information look https://developer.android.com/guide/components/services#Foreground
     */
    public abstract int getNotificationId();
}
