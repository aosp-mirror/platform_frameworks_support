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
