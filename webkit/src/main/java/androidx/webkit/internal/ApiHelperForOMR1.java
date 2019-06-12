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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Utility class to use new APIs that were added in OMR1 (API level 27). These need to exist in a
 * separate class so that the Android runtime (ART) can successfully verify classes without
 * encountering the new APIs.
 */
@DoNotInline
@RequiresApi(27)
public final class ApiHelperForOMR1 {
    // Do not instantiate this class.
    private ApiHelperForOMR1() {}

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
    public static void startSafeBrowsing(@NonNull Context context,
            @Nullable ValueCallback<Boolean> callback) {
        WebView.startSafeBrowsing(context, callback);
    }

    /**
     * todo.
     */
    public static void setSafeBrowsingWhitelist(@NonNull List<String> hosts,
            @Nullable ValueCallback<Boolean> callback) {
        WebView.setSafeBrowsingWhitelist(hosts, callback);
    }

    /**
     * todo.
     */
    @NonNull
    public static Uri getSafeBrowsingPrivacyPolicyUrl() {
        return WebView.getSafeBrowsingPrivacyPolicyUrl();
    }
}
