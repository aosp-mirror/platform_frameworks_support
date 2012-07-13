package android.support.v4.app;

import android.app.Notification;

class NotificationCompatICS implements NotificationCompat.NotificationCompatImpl {
    static Notification.Builder createBuilder(NotificationCompat.Builder b) {
        Notification.Builder builder = NotificationCompatHoneycomb.createBuilder(b);
        if (b.mProgressSet) {
            builder.setProgress(b.mProgressMax, b.mProgress, b.mProgressIndeterminate);
        }
        return builder;
    }

    @Override
    public Notification build(NotificationCompat.Builder builder) {
        return createBuilder(builder).getNotification();
    }
}
