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

import static androidx.browser.customtabs.CustomTabsService.RELATION_HANDLE_ALL_URLS;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;

/**
 * A convenience class for routing "Manage Space" clicks in the settings of apps hosting Trusted Web
 * Activities into a web browser's site settings.
 *
 * To use this activity you need to:
 * 1) Add it to the manifest. You might want to set a transparent theme to avoid seeing a white
 * background while the activity is launched.
 * 2) Set the url for which site-setting are to be shown in the MANAGE_SPACE_URL metadata.
 * Alternatively, override {@link #getUrlForManagingSpace()}.
 * The provided url must belong to the origin associated with your app via the Digital Asset Links.
 * 3) Specify this activity in manageSpaceActivity attribute of the application in the manifest [1].
 *
 * Alternatively, you can create your own activity for managing space and use
 * {@link TrustedWebUtils#launchBrowserSiteSettings} directly, after establishing a connection to
 * CustomTabsService. It is required to also do {@link CustomTabsClient#warmup} and
 * {@link CustomTabsSession#validateRelationship} prior to calling
 * {@link TrustedWebUtils#launchBrowserSiteSettings}.
 *
 * [1] https://developer.android.com/guide/topics/manifest/application-element#space
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ManageDataLauncherActivity extends AppCompatActivity {
    private static final String TAG = "ManageDataLauncher";

    private static final String METADATA_MANAGE_SPACE_URL =
            "android.support.customtabs.trusted.MANAGE_SPACE_URL";

    /**
     * @return url of the page for which site-settings will be launched.
     * By default uses the url provided in metadata.
     * If null is returned, site-settings won't be launched.
     */
    @Nullable
    protected Uri getUrlForManagingSpace() {
        try {
            ActivityInfo info = getPackageManager()
                    .getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);

            if (info.metaData != null && info.metaData.containsKey(METADATA_MANAGE_SPACE_URL)) {
                Uri uri = Uri.parse(info.metaData.getString(METADATA_MANAGE_SPACE_URL));
                Log.d(TAG, "Using clean-up URL from Manifest (" + uri + ").");
                return uri;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen.
            onError(new RuntimeException(e));
        }

        return null;
    }

    /**
     * Override to customize loading view, or return null if not needed.
     */
    @Nullable
    protected View createLoadingView() {
        return new ProgressBar(this);
    }

    /**
     * Override to implement custom error handling.
     */
    protected void onError(RuntimeException e) {
        throw e;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String chromePackage = CustomTabsClient.getPackageName(this,
                TrustedWebUtils.SUPPORTED_CHROME_PACKAGES, false);
        if (chromePackage == null) {
            onError(new RuntimeException("No valid build of Chrome found"));
            finish();
            return;
        }

        View loadingView = createLoadingView();
        if (loadingView != null) {
            setContentView(loadingView);
        }

        CustomTabsClient.bindCustomTabsService(this, chromePackage, mServiceConnection);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mServiceConnection);
        finish();
    }

    private final CustomTabsServiceConnection mServiceConnection =
            new CustomTabsServiceConnection() {

        CustomTabsSession mSession;

        private CustomTabsCallback mCustomTabsCallback = new CustomTabsCallback() {
            @Override
            public void onRelationshipValidationResult(int relation, Uri requestedOrigin,
                    boolean validated, Bundle extras) {
                if (!validated) {
                    onError(new RuntimeException("Failed to validate origin " + requestedOrigin));
                    finish();
                    return;
                }

                try {
                    TrustedWebUtils.launchBrowserSiteSettings(ManageDataLauncherActivity.this,
                            mSession, requestedOrigin);
                } catch (RuntimeException e) {
                    onError(e);
                    finish();
                }
            }
        };

        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            Uri uri = getUrlForManagingSpace();
            if (uri == null) {
                finish();
                return;
            }

            mSession = client.newSession(mCustomTabsCallback);
            if (mSession == null) {
                onError(new RuntimeException("Failed to create CustomTabsSession"));
                finish();
                return;
            }

            // Warm up is needed for origin verification.
            client.warmup(0);
            mSession.validateRelationship(RELATION_HANDLE_ALL_URLS, uri, null);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    };
}
