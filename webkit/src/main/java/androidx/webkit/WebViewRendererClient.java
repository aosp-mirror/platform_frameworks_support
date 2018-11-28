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

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.RenderProcessGoneDetailImpl;
import androidx.webkit.internal.WebViewRendererImpl;

import org.chromium.support_lib_boundary.WebViewRendererClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

import java.lang.reflect.InvocationHandler;

/**
 */
public class WebViewRendererClient implements WebViewRendererClientBoundaryInterface {
    private static final String[] sSupportedFeatures = new String[] {
            Features.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE + Features.DEV_SUFFIX,
    };

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

    public void onRendererUnresponsive(@NonNull WebView view, @Nullable WebViewRenderer renderer) {}

    public void onRendererResponsive(@NonNull WebView view, @Nullable WebViewRenderer renderer) {}

    public boolean onRenderProcessGone(
            @NonNull WebView view, @NonNull RenderProcessGoneDetail detail) {
        return false;
    }

    /**
     * Applications are not meant to override this.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onRendererUnresponsive(
            @NonNull WebView view, @Nullable /* WebViewRenderer */ InvocationHandler renderer) {
        onRendererUnresponsive(view, WebViewRendererImpl.forInvocationHandler(renderer));
    }

    /**
     * Applications are not meant to override this.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onRendererResponsive(
            @NonNull WebView view, @Nullable /* WebViewRenderer */ InvocationHandler renderer) {
        onRendererResponsive(view, WebViewRendererImpl.forInvocationHandler(renderer));
    }

    /**
     * Applications are not meant to override this.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final boolean onRenderProcessGone(@NonNull WebView view,
            @NonNull /* RenderProcessGoneDetail */ InvocationHandler detail) {
        return onRenderProcessGone(view, RenderProcessGoneDetailImpl.forInvocationHandler(detail));
    }
}
