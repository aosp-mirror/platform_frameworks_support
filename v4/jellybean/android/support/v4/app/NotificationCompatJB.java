
/*
 * Copyright (C) 2012 The Android Open Source Project
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

class NotificationCompatJB implements NotificationCompat.NotificationCompatImpl {
    public static Notification.Builder createBuilder(NotificationCompat.Builder b) {
        final Notification.Builder builder = NotificationCompatICS.createBuilder(b);

        if (b.mActionIcons != null) {
            final int size = b.mActionIcons.size();
            for (int i = 0; i < size; i++) {
                builder.addAction(b.mActionIcons.get(i), b.mActionTitles.get(i),
                    b.mActionIntents.get(i));
            }
        }

        return builder.setPriority(b.mPriority)
                .setSubText(b.mSubText)
                .setUsesChronometer(b.mUsesChronometer);
    }

    @Override
    public Notification build(NotificationCompat.Builder b) {
        if (b.mStyle != null) {
            NotificationCompat.Style style = b.mStyle;
            b.mStyle = null; // Avoid infinite recursion
            style.setBuilder(b);
            return style.build();
        }
        return createBuilder(b).build();
    }

    public static Notification buildBigPictureStyle(NotificationCompat.BigPictureStyle s) {
        return new Notification.BigPictureStyle(createBuilder(s.mBuilder))
                .bigLargeIcon(s.mBigLargeIcon)
                .bigPicture(s.mBigPicture)
                .setBigContentTitle(s.mBigContentTitle)
                .setSummaryText(s.mSummaryText)
                .build();
    }

    public static Notification buildBigTextStyle(NotificationCompat.BigTextStyle s) {
        return new Notification.BigTextStyle(createBuilder(s.mBuilder))
                .bigText(s.mBigText)
                .setBigContentTitle(s.mBigContentTitle)
                .setSummaryText(s.mSummaryText)
                .build();
    }

    public static Notification buildInboxStyle(NotificationCompat.InboxStyle s) {
        Notification.InboxStyle style = new Notification.InboxStyle(createBuilder(s.mBuilder))
                .setBigContentTitle(s.mBigContentTitle)
                .setSummaryText(s.mSummaryText);

        if (s.mLines != null) {
            for (CharSequence line : s.mLines) {
                style.addLine(line);
            }
        }

        return style.build();
    }
}
