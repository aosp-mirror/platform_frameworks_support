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

package androidx.browser.trusted.splashscreens;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.browser.customtabs.TrustedWebUtils.SplashScreenParamKey;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.TrustedWebActivityBuilder;
import androidx.browser.trusted.Utils;

/**
 * Implementation of {@link SplashScreenStrategy} suitable for apps that are PWA wrappers (i.e.
 * apps having no other UI outside of a TWA they launch).
 *
 * Shows splash screen in the client app before TWA is launched, then seamlessly transfers it into
 * the browser, which keeps it visible until the web page is loaded. The browser must support
 * {@link TrustedWebUtils.SplashScreenVersion#V1}.
 *
 * To use this you need to set up a FileProvider in AndroidManifest with the following paths:
 * <paths><files-path path="twa_splash/" name="twa_splash"/></paths>.
 *
 * **NB**: This class requires {@link #onActivityEnterAnimationComplete} to be called from
 * {@link Activity#onEnterAnimationComplete()}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PwaWrapperSplashScreenStrategy implements SplashScreenStrategy {

    private static final String TAG = "SplashScreenStrategy";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Activity mActivity;
    @DrawableRes
    private final int mDrawableId;
    @ColorInt
    private final int mBackgroundColor;
    private final ImageView.ScaleType mScaleType;
    @Nullable
    private final Matrix mTransformationMatrix;
    private final String mFileProviderAuthority;
    private final int mFadeOutDurationMillis;

    @Nullable
    private Bitmap mSplashImage;

    @Nullable
    private SplashImageTransferTask mSplashImageTransferTask;

    @Nullable
    private String mProviderPackage;

    private boolean mProviderSupportsSplashScreens;

    // Defaulting to true for pre-L because enter animations were introduced in L.
    private boolean mEnterAnimationComplete = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;

    @Nullable
    private Runnable mOnEnterAnimationCompleteRunnable;

    /**
     * @param activity {@link Activity} on top of which a TWA is going to be launched.
     * @param drawableId Resource id of the Drawable of an image (e.g. logo) displayed in the
     * splash screen.
     * @param backgroundColor Background color of the splash screen.
     * @param scaleType see {@link SplashScreenParamKey#SCALE_TYPE}
     * @param transformationMatrix see {@link SplashScreenParamKey#IMAGE_TRANSFORMATION_MATRIX}.
     * @param fadeOutDurationMillis see {@link SplashScreenParamKey#FADE_OUT_DURATION_MS}.
     * @param fileProviderAuthority Authority of a FileProvider used for transferring the splash
     * image to the browser.
     */
    public PwaWrapperSplashScreenStrategy(
            Activity activity,
            @DrawableRes int drawableId,
            @ColorInt int backgroundColor,
            ImageView.ScaleType scaleType,
            @Nullable Matrix transformationMatrix,
            int fadeOutDurationMillis,
            String fileProviderAuthority) {
        mDrawableId = drawableId;
        mBackgroundColor = backgroundColor;
        mScaleType = scaleType;
        mTransformationMatrix = transformationMatrix;
        mActivity = activity;
        mFileProviderAuthority = fileProviderAuthority;
        mFadeOutDurationMillis = fadeOutDurationMillis;
    }

    @Override
    public void onTwaLaunchInitiated(String providerPackage, @Nullable Integer statusBarColor) {
        mProviderPackage = providerPackage;
        mProviderSupportsSplashScreens = TrustedWebUtils.splashScreensAreSupported(mActivity,
                providerPackage, TrustedWebUtils.SplashScreenVersion.V1);

        if (!mProviderSupportsSplashScreens) {
            Log.w(TAG, "Provider " + providerPackage + " doesn't support splash screens");
            return;
        }

        showSplashScreen();
        if (mSplashImage != null) {
            customizeStatusAndNavBarDuringSplashScreen(statusBarColor);
        }
    }

    /**
     * Splash screen is shown both before the Trusted Web Activity is launched - in this activity,
     * and for some time after that - in browser, on top of web page being loaded.
     * This method shows the splash screen in the LauncherActivity.
     */
    private void showSplashScreen() {
        mSplashImage = Utils.convertDrawableToBitmap(mActivity, mDrawableId);
        if (mSplashImage == null) {
            Log.w(TAG, "Failed to retrieve splash image from provided drawable id");
            return;
        }
        ImageView view = new ImageView(mActivity);
        view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        view.setImageBitmap(mSplashImage);
        view.setBackgroundColor(mBackgroundColor);

        view.setScaleType(mScaleType);
        if (mScaleType == ImageView.ScaleType.MATRIX) {
            view.setImageMatrix(mTransformationMatrix);
        }

        mActivity.setContentView(view);
    }

    /**
     * Sets the colors of status and navigation bar to match the ones seen after the splash screen
     * is transferred to the browser.
     */
    private void customizeStatusAndNavBarDuringSplashScreen(@Nullable Integer statusBarColor) {
        // Custom tabs may in future support customizing status bar icon color and nav bar color.
        // For now, we apply the colors Chrome uses.
        Utils.setWhiteNavigationBar(mActivity);

        if (statusBarColor == null) return;

        Utils.setStatusBarColor(mActivity, statusBarColor);

        if (Utils.shouldUseDarkStatusBarIcons(statusBarColor)) {
            Utils.setDarkStatusBarIcons(mActivity);
        }
    }

    @Override
    public void configureTwaBuilder(final TrustedWebActivityBuilder builder,
            CustomTabsSession session,
            final Runnable onReadyCallback) {
        if (!mProviderSupportsSplashScreens || mSplashImage == null) {
            onReadyCallback.run();
            return;
        }
        if (TextUtils.isEmpty(mFileProviderAuthority)) {
            Log.w(TAG, "FileProvider authority not specified, can't transfer splash image.");
            onReadyCallback.run();
            return;
        }
        mSplashImageTransferTask = new SplashImageTransferTask(mActivity,
                mSplashImage, mFileProviderAuthority, session,
                mProviderPackage);

        mSplashImageTransferTask.execute(new SplashImageTransferTask.Callback() {
            @Override
            public void onFinished(boolean success) {
                onSplashImageTransferred(builder, success, onReadyCallback);
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onSplashImageTransferred(TrustedWebActivityBuilder builder, boolean success,
            final Runnable onReadyCallback) {
        if (!success) {
            Log.w(TAG, "Failed to transfer splash image.");
            onReadyCallback.run();
            return;
        }
        builder.setSplashScreenParams(makeSplashScreenParamsBundle());

        runWhenEnterAnimationComplete(new Runnable() {
            @Override
            public void run() {
                onReadyCallback.run();
                // Avoid window animations during transition.
                mActivity.overridePendingTransition(0, 0);
            }
        });
    }

    private void runWhenEnterAnimationComplete(Runnable runnable) {
        if (mEnterAnimationComplete) {
            runnable.run();
        } else {
            mOnEnterAnimationCompleteRunnable = runnable;
        }
    }

    @NonNull
    private Bundle makeSplashScreenParamsBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(SplashScreenParamKey.VERSION, TrustedWebUtils.SplashScreenVersion.V1);
        bundle.putInt(SplashScreenParamKey.FADE_OUT_DURATION_MS, mFadeOutDurationMillis);
        bundle.putInt(SplashScreenParamKey.BACKGROUND_COLOR, mBackgroundColor);
        bundle.putInt(SplashScreenParamKey.SCALE_TYPE, mScaleType.ordinal());
        if (mTransformationMatrix != null) {
            float[] values = new float[9];
            mTransformationMatrix.getValues(values);
            bundle.putFloatArray(SplashScreenParamKey.IMAGE_TRANSFORMATION_MATRIX,
                    values);
        }
        return bundle;
    }

    /**
     * To be called from {@link Activity#onEnterAnimationComplete}.
     */
    public void onActivityEnterAnimationComplete() {
        mEnterAnimationComplete = true;
        if (mOnEnterAnimationCompleteRunnable != null) {
            mOnEnterAnimationCompleteRunnable.run();
            mOnEnterAnimationCompleteRunnable = null;
        }
    }

    /**
     * Performs clean-up.
     */
    public void destroy() {
        if (mSplashImageTransferTask != null) {
            mSplashImageTransferTask.cancel();
        }
    }
}
