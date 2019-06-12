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

package androidx.webkit.internal;

import android.content.pm.PackageInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utility class to use new APIs that were added in O (API level 26). These need to exist in a
 * separate class so that the Android runtime (ART) can successfully verify classes without
 * encountering the new APIs.
 */
@DoNotInline
@RequiresApi(26)
public final class ApiHelperForO {
    // Do not instantiate this class.
    private ApiHelperForO() {}

    /**
     * todo.
     */
    @Nullable
    public static PackageInfo getCurrentWebViewPackage() {
        return WebView.getCurrentWebViewPackage();
    }

    /**
     * todo.
     */
    public static void setSafeBrowsingEnabled(@NonNull WebSettings settings, boolean enabled) {
        settings.setSafeBrowsingEnabled(enabled);
    }

    /**
     * todo.
     */
    public static boolean getSafeBrowsingEnabled(@NonNull WebSettings settings) {
        return settings.getSafeBrowsingEnabled();
    }

    /**
     * todo.
     */
    public static @NonNull WebViewClient getWebViewClient(@NonNull WebView webview) {
        return webview.getWebViewClient();
    }

    /**
     * todo.
     */
    public static @Nullable WebChromeClient getWebChromeClient(@NonNull WebView webview) {
        return webview.getWebChromeClient();
    }
}
