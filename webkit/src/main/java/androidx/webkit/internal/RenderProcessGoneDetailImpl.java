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

package androidx.webkit.internal;

import androidx.annotation.NonNull;
import androidx.webkit.RenderProcessGoneDetail;
import androidx.webkit.WebViewCompat;

import org.chromium.support_lib_boundary.RenderProcessGoneDetailBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;

/**
 * Implementation of {@link WebViewRenderer}.
 * This class uses the WebView APK to implement
 * {@link WebViewRenderer} functionality.
 */
public class RenderProcessGoneDetailImpl extends RenderProcessGoneDetail {
    private RenderProcessGoneDetailBoundaryInterface mBoundaryInterface;

    public RenderProcessGoneDetailImpl(RenderProcessGoneDetailBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Get a support library WebViewRenderer object that is 1:1 with the webview object.
     */
    public static @NonNull RenderProcessGoneDetail forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        return new RenderProcessGoneDetailImpl(BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                RenderProcessGoneDetailBoundaryInterface.class, invocationHandler));
    }

    @Override
    public boolean didCrash() {
        return mBoundaryInterface.didCrash();
    }

    @Override
    public @WebViewCompat.RendererPriority int rendererPriorityAtExit() {
        return mBoundaryInterface.rendererPriorityAtExit();
    }
}
