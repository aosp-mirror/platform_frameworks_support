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

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utility class to use new APIs that were added in M (API level 23). These need to exist in a
 * separate class so that the Android runtime (ART) can successfully verify classes without
 * encountering the new APIs.
 */
@DoNotInline
@RequiresApi(23)
public final class ApiHelperForM {
    // Do not instantiate this class.
    private ApiHelperForM() {}

    /**
     * todo.
     */
    public interface VisualStateCallback {
        /**
         * todo.
         */
        void onComplete(long requestId);
    }

    /**
     * todo.
     */
    public static void postVisualStateCallback(@NonNull WebView webview, long requestId,
            @NonNull final VisualStateCallback callback) {
        webview.postVisualStateCallback(requestId,
                    new android.webkit.WebView.VisualStateCallback() {
                        @Override
                        public void onComplete(long l) {
                            callback.onComplete(l);
                        }
                    });
    }
}
