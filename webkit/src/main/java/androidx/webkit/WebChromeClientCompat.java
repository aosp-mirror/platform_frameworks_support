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

package androidx.webkit;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.WebMessagePortImpl;

import org.chromium.support_lib_boundary.WebChromeClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

import java.lang.reflect.InvocationHandler;

/**
 * Compatibility version of {@link android.webkit.WebChromeClient}.
 */
public class WebChromeClientCompat
        extends WebChromeClient implements WebChromeClientBoundaryInterface {
    private static final String[] sSupportedFeatures = new String[] {
            Features.ON_POST_MESSAGE,
    };

    /**
     * Invoked by chromium (for WebView APKs 73+) for the {@code chrome.runtime.postMessage()}
     * JavaScript call.
     * Applications are not meant to override this, and should instead override the non-final {@link
     * #onPostMessage(WebView, String, String, WebMessagePortCompat[])} method.
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void onPostMessage(@NonNull WebView view, @NonNull String message,
            @NonNull String sourceOrigin, @NonNull InvocationHandler[] handlers) {
        WebMessagePortCompat[] ports = new WebMessagePortCompat[handlers.length];
        for (int i = 0; i < ports.length; i++) {
            ports[i] = new WebMessagePortImpl(handlers[i]);
        }
        onPostMessage(view, message, sourceOrigin, ports);
    }

    /**
     * Notify the host application that JavaScript {@code chrome.runtime.postMessage()} was called.
     *
     * @param view         The {@link android.webkit.WebView} for which the JavaScript API is
     *                     called.
     * @param message      The message passed from JavaScript.
     * @param sourceOrigin On which origin this API was called.
     * @param ports        {@link androidx.webkit.WebMessagePortCompat} ports that entangled with
     *                     JavaScript ports.
     */
    public void onPostMessage(@NonNull WebView view, @NonNull String message,
            @NonNull String sourceOrigin, @NonNull WebMessagePortCompat[] ports) {}

    /**
     * Returns the list of features this client supports. This feature list should always be a
     * subset of the Features declared in WebViewFeature.
     *
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final String[] getSupportedFeatures() {
        return sSupportedFeatures;
    }
}
