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

package androidx.browser.trusted;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Utilities used by helper classes that are setting up and launching Trusted Web Activities.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Utils {

    /** Sets status bar color. */
    public static void setStatusBarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        activity.getWindow().setStatusBarColor(color);
    }

    /** Darkens the color of status bar icons. */
    public static void setDarkStatusBarIcons(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        addSystemUiVisibilityFlag(activity, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    /** Whitens the navigation bar. */
    public static void setWhiteNavigationBar(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        activity.getWindow().setNavigationBarColor(Color.WHITE);

        // Make the button icons dark
        addSystemUiVisibilityFlag(activity, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private static void addSystemUiVisibilityFlag(Activity activity, int flag) {
        View root = activity.getWindow().getDecorView().getRootView();
        int visibility = root.getSystemUiVisibility();
        visibility |= flag;
        root.setSystemUiVisibility(visibility);
    }

    /**
     * Determines whether to use dark status bar icons by comparing the contrast ratio of the color
     * relative to white (https://www.w3.org/TR/WCAG20/#contrast-ratiodef) to a threshold.
     * This criterion matches the one used by Chrome:
     * https://chromium.googlesource.com/chromium/src/+/90ac05ba6cb9ab5d5df75f0cef62c950be3716c3/chrome/android/java/src/org/chromium/chrome/browser/util/ColorUtils.java#215
     */
    public static boolean shouldUseDarkStatusBarIcons(int statusBarColor) {
        float luminance = 0.2126f * luminanceOfColorComponent(Color.red(statusBarColor))
                + 0.7152f * luminanceOfColorComponent(Color.green(statusBarColor))
                + 0.0722f * luminanceOfColorComponent(Color.blue(statusBarColor));
        float contrast = Math.abs((1.05f) / (luminance + 0.05f));
        return contrast < 3;
    }

    private static float luminanceOfColorComponent(float c) {
        c /= 255f;
        return (c < 0.03928f) ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    /**
     * Converts drawable located at given resource id into a Bitmap.
     */
    @Nullable
    public static Bitmap convertDrawableToBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) {
            return null;
        }
        drawable = DrawableCompat.wrap(drawable);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
