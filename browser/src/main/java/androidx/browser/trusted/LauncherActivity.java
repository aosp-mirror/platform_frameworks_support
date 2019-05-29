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

package androidx.browser.trusted;

import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.splashscreens.PwaWrapperSplashScreenStrategy;
import androidx.core.content.ContextCompat;

/**
 * A convenience class to make using Trusted Web Activities easier. You can extend this class for
 * basic modifications to the behaviour.
 *
 * If you just want to wrap a website in a Trusted Web Activity you should:
 * 1) Copy the manifest for the svgomg project.
 * 2) Set up Digital Asset Links [1] for your site and app.
 * 3) Set the DEFAULT_URL metadata in the manifest and the browsable intent filter to point to your
 *    website.
 *
 * You can skip (2) if you just want to try out TWAs but not on your own website, but you must
 * add the {@code --disable-digital-asset-link-verification-for-url=https://svgomg.firebaseapp.com}
 * to Chrome for this to work [2].
 *
 * You may also go beyond this and add notification delegation, which causes push notifications to
 * be shown by your app instead of Chrome. This is detailed in the javadoc for
 * {@link TrustedWebActivityService}.
 *
 * If you just want default behaviour your Trusted Web Activity client app doesn't even need any
 * Java code - you just set everything up in the Android Manifest!
 *
 * This activity also supports showing a splash screen while the Trusted Web Activity provider is
 * warming up and is loading the page in Trusted Web Activity. This is supported in Chrome 75+.
 *
 * Splash screens support in Chrome is based on transferring the splash screen via FileProvider [3].
 * To set up splash screens, you need to:
 * 1) Set up a FileProvider in the Manifest as described in [3]. The file provider paths should be
 * as follows: <paths><files-path path="twa_splash/" name="twa_splash"/></paths>
 * 2) Provide splash-screen related metadata (see descriptions in {@link LauncherActivityMetadata}),
 * including the authority of your FileProvider.
 *
 * Splash screen is first shown here in LauncherActivity, then seamlessly moved onto the browser.
 * Showing splash screen in the app first is optional, but highly recommended, because on slow
 * devices (e.g. Android Go) it can take seconds to boot up a browser.
 *
 * Recommended theme for this Activity is:
 * <pre>{@code
 * <style name="LauncherActivityTheme" parent="Theme.AppCompat.NoActionBar">
 *     <item name="android:windowIsTranslucent">true</item>
 *     <item name="android:windowBackground">@android:color/transparent</item>
 *     <item name="android:statusBarColor">@android:color/transparent</item>
 *     <item name="android:navigationBarColor">@android:color/transparent</item>
 *     <item name="android:backgroundDimEnabled">false</item>
 * </style>
 * }</pre>
 *
 * Note that even with splash screen enabled, it is still recommended to use a transparent theme.
 * That way the Activity can gracefully fall back to being a transparent "trampoline" activity in
 * the following cases:
 * - Splash screens are not supported by the picked browser.
 * - The TWA is already running, and LauncherActivity merely needs to deliver a new Intent to it.
 *
 * [1] https://developers.google.com/digital-asset-links/v1/getting-started
 * [2] https://www.chromium.org/developers/how-tos/run-chromium-with-flags#TOC-Setting-Flags-for-Chrome-on-Android
 * [3] https://developer.android.com/reference/android/support/v4/content/FileProvider
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LauncherActivity extends AppCompatActivity {
    private static final String TAG = "TWALauncherActivity";

    private static final String BROWSER_WAS_LAUNCHED_KEY =
            "android.support.customtabs.trusted.BROWSER_WAS_LAUNCHED_KEY";

    /** We only want to show the update prompt once per instance of this application. */
    private static boolean sChromeVersionChecked;

    private LauncherActivityMetadata mMetadata;

    private boolean mBrowserWasLaunched;

    @Nullable
    private PwaWrapperSplashScreenStrategy mSplashScreenStrategy;

    @Nullable
    private TwaLauncher mTwaLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean(BROWSER_WAS_LAUNCHED_KEY)) {
            // This activity died in the background after launching Trusted Web Activity, then
            // the user closed the Trusted Web Activity and ended up here.
            finish();
            return;
        }

        mMetadata = LauncherActivityMetadata.parse(this);

        if (splashScreenNeeded()) {
            mSplashScreenStrategy = new PwaWrapperSplashScreenStrategy(this,
                    mMetadata.splashImageDrawableId,
                    getColorCompat(mMetadata.splashScreenBackgroundColorId),
                    getSplashImageScaleType(),
                    getSplashImageTransformationMatrix(),
                    mMetadata.splashScreenFadeOutDurationMillis,
                    mMetadata.fileProviderAuthority);
        }

        TrustedWebActivityBuilder twaBuilder =
                new TrustedWebActivityBuilder(this, getLaunchingUrl())
                        .setStatusBarColor(getColorCompat(mMetadata.statusBarColorId));

        mTwaLauncher = new TwaLauncher(this);
        mTwaLauncher.launch(twaBuilder, mSplashScreenStrategy, new Runnable() {
            @Override
            public void run() {
                mBrowserWasLaunched = true;
            }
        });

        if (!sChromeVersionChecked) {
            TrustedWebUtils.promptForChromeUpdateIfNeeded(this, mTwaLauncher.getProviderPackage());
            sChromeVersionChecked = true;
        }
    }

    private boolean splashScreenNeeded() {
        // Splash screen was not requested.
        if (mMetadata.splashImageDrawableId == 0) return false;

        // If this activity isn't task root, then a TWA is already running in this task. This can
        // happen if a VIEW intent (without Intent.FLAG_ACTIVITY_NEW_TASK) is being handled after
        // launching a TWA. In that case we're only passing a new intent into existing TWA, and
        // don't show the splash screen.
        return isTaskRoot();
    }

    /**
     * Override to set a custom scale type for the image displayed on a splash screen.
     * See {@link ImageView.ScaleType}.
     */
    @NonNull
    protected ImageView.ScaleType getSplashImageScaleType() {
        return ImageView.ScaleType.CENTER;
    }

    /**
     * Override to set a transformation matrix for the image displayed on a splash screen.
     * See {@link ImageView#setImageMatrix}.
     * Has any effect only if {@link #getSplashImageScaleType()} returns {@link
     * ImageView.ScaleType#MATRIX}.
     */
    @Nullable
    protected Matrix getSplashImageTransformationMatrix() {
        return null;
    }

    private int getColorCompat(int splashScreenBackgroundColorId) {
        return ContextCompat.getColor(this, splashScreenBackgroundColorId);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mBrowserWasLaunched) {
            finish(); // The user closed the Trusted Web Activity and ended up here.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTwaLauncher != null) {
            mTwaLauncher.destroy();
        }
        if (mSplashScreenStrategy != null) {
            mSplashScreenStrategy.destroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BROWSER_WAS_LAUNCHED_KEY, mBrowserWasLaunched);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (mSplashScreenStrategy != null) {
            mSplashScreenStrategy.onActivityEnterAnimationComplete();
        }
    }

    /**
     * Returns the URL that the Trusted Web Activity should be launched to. By default this
     * implementation checks to see if the Activity was launched with an Intent with data, if so
     * attempt to launch to that URL. If not, read the
     * "android.support.customtabs.trusted.DEFAULT_URL" metadata from the manifest.
     *
     * Override this for special handling (such as ignoring or sanitising data from the Intent).
     */
    protected Uri getLaunchingUrl() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            Log.d(TAG, "Using URL from Intent (" + uri + ").");
            return uri;
        }

        if (mMetadata.defaultUrl != null) {
            Log.d(TAG, "Using URL from Manifest (" + mMetadata.defaultUrl + ").");
            return Uri.parse(mMetadata.defaultUrl);
        }

        return Uri.parse("https://www.example.com/");
    }

}
