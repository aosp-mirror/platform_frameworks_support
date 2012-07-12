package android.support.v4.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

class ActivityCompatJB {
    public static void startActivity(Context context, Intent intent, Bundle options) {
        context.startActivity(intent, options);
    }
}
