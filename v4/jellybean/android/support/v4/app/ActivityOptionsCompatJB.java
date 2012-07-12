package android.support.v4.app;

import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

class ActivityOptionsCompatJB extends ActivityOptionsCompat {
    public static ActivityOptionsCompat makeCustomAnimation(Context context, int enterResId, int exitResId) {
        return new ActivityOptionsCompatJB(ActivityOptions.makeCustomAnimation(context, enterResId, exitResId));
    }

    public static ActivityOptionsCompat makeScaleUpAnimation(View source, int startX, int startY, int startWidth, int startHeight) {
        return new ActivityOptionsCompatJB(ActivityOptions.makeScaleUpAnimation(source, startX, startY, startWidth, startHeight));
    }

    public static ActivityOptionsCompat makeThumbnailScaleUpAnimation(View source, Bitmap thumbnail, int startX, int startY) {
        return new ActivityOptionsCompatJB(ActivityOptions.makeThumbnailScaleUpAnimation(source, thumbnail, startX, startY));
    }

    private final ActivityOptions mActivityOptions;

    private ActivityOptionsCompatJB(ActivityOptions activityOptions) {
        mActivityOptions = activityOptions;
    }

    @Override
    public Bundle toBundle() {
        return mActivityOptions.toBundle();
    }

    @Override
    public void update(ActivityOptionsCompat otherOptions) {
        if (otherOptions != null && otherOptions instanceof ActivityOptionsCompatJB) {
            ActivityOptions realOther = ((ActivityOptionsCompatJB) otherOptions).mActivityOptions;
            mActivityOptions.update(realOther);
        }
    }
}
