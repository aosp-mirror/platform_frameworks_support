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

package androidx.mediarouter.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * Volume slider with showing, hiding, and applying alpha supports to the thumb.
 */
class MediaRouteVolumeSlider extends AppCompatSeekBar {
    private static final String TAG = "MediaRouteVolumeSlider";
    private final float mDisabledAlpha;

    private boolean mHideThumb;
    private Drawable mThumb;
    private int mProgressAndThumbColor;
    private int mBackgroundColor;

    public MediaRouteVolumeSlider(Context context) {
        this(context, null);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.seekBarStyle);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(context);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int alpha = isEnabled() ? 0xFF : (int) (0xFF * mDisabledAlpha);

        LayerDrawable ld = (LayerDrawable) getProgressDrawable().mutate();
        Drawable progressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        Drawable backgroundDrawable = ld.findDrawableByLayerId(android.R.id.background);

        // The thumb drawable is a collection of drawables and its current drawables are changed per
        // state. Apply the color filter and alpha on every state change.
        setDrawableColor(mThumb, mProgressAndThumbColor, alpha);
        setDrawableColor(progressDrawable, mProgressAndThumbColor, alpha);
        setDrawableColor(backgroundDrawable, mBackgroundColor, alpha);
    }

    private void setDrawableColor(Drawable drawable, int color, int alpha) {
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        drawable.setAlpha(alpha);
    }

    @Override
    public void setThumb(Drawable thumb) {
        mThumb = thumb;
        super.setThumb(mHideThumb ? null : mThumb);
    }

    /**
     * Sets whether to show or hide thumb.
     */
    public void setHideThumb(boolean hideThumb) {
        if (mHideThumb == hideThumb) {
            return;
        }
        mHideThumb = hideThumb;
        super.setThumb(mHideThumb ? null : mThumb);
    }

    /**
     * Sets color of thumb and progressbar.
     */
    public void setColor(int color) {
        setProgressAndThumbColor(color);
        setBackgroundColor(color);
    }

    /**
     * Sets theme color of the volume slider which fills both progress part of the prgressbar and
     * thumb. The change takes effect next time drawable state is changed.
     * <p>
     * The color cannot be translucent, otherwise the underlying progress bar will be seen through
     * the thumb.
     * </p>
     */
    public void setProgressAndThumbColor(int progressAndThumbColor) {
        if (mProgressAndThumbColor == progressAndThumbColor) {
            return;
        }
        if (Color.alpha(progressAndThumbColor) != 0xFF) {
            Log.e(TAG, "Volume slider progress color cannot be translucent: #"
                    + Integer.toHexString(progressAndThumbColor));
        }
        mProgressAndThumbColor = progressAndThumbColor;
    }

    /**
     * Sets background color of the volume slider. The change takes effect next time drawable state
     * is changed.
     * <p>
     * The color cannot be translucent, otherwise the underlying background view will be seen
     * through the progressbar.
     * </p>
     */
    public void setBackgroundColor(int backgroundColor) {
        if (mBackgroundColor == backgroundColor) {
            return;
        }
        if (Color.alpha(backgroundColor) != 0xFF) {
            Log.e(TAG, "Volume slider background color cannot be translucent: #"
                    + Integer.toHexString(backgroundColor));
        }
        mBackgroundColor = backgroundColor;
    }
}
