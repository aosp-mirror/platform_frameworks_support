/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import java.util.ArrayList;

class NotificationCompatJellybeanMr1 extends NotificationCompatJellybean {
    public NotificationCompatJellybeanMr1(Context context, Notification n,
            CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
            RemoteViews tickerView, int number,
            PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
            int mProgressMax, int mProgress, boolean mProgressIndeterminate,
            boolean useChronometer, int priority, CharSequence subText, boolean showWhen) {
        super(context, n, contentTitle, contentText, contentInfo, tickerView, number,
            contentIntent, fullScreenIntent, largeIcon, mProgressMax, mProgress,
            mProgressIndeterminate, useChronometer, priority, subText);
        b.setShowWhen(showWhen);
    }
}
