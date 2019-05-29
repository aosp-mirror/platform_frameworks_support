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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityBuilder;

/**
 * Defines behavior of the splash screen shown when launching a TWA.
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface SplashScreenStrategy {

    /**
     * Called immediately in the beginning of TWA launching process (before establishing
     * connection with CustomTabsService). Can be used to display splash screen on the client app's
     * side before the browser is launched.
     * @param providerPackage Package name of the browser being launched. Implementations should
     * check whether this browser supports splash screens.
     * @param statusBarColor Status bar color of TWA. Implementations that show splash screen in
     * client app should set this status bar color. Null if client did not specify a status bar
     * color.
     */
    void onTwaLaunchInitiated(String providerPackage, @Nullable Integer statusBarColor);

    /**
     * Called when TWA is ready to be launched.
     * @param builder {@link TrustedWebActivityBuilder} to be supplied with splash screen related
     * parameters.
     * @param session {@link CustomTabsSession} with which the TWA will launch.
     * @param onReadyCallback Callback to be triggered when splash screen preparation is finished.
     * TWA is launched immediately upon triggering this callback.
     */
    void configureTwaBuilder(TrustedWebActivityBuilder builder, CustomTabsSession session,
            Runnable onReadyCallback);

}
