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

package com.example.androidx.webkit;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} to exercise Force Dark functionality.
 * It shows WebViews side by side with different dark mode settings.
 */
public class ForceDarkActivity extends AppCompatActivity {
    private static final int STRETCH_THIS_DIMENSION = 0;
    private static final int MARGIN_DP = 5;
    private static final String DESCRIPTION =
            "<html><body><h1>Force Dark Mode is %s </h1></body></html>";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_force_dark);
        setTitle(R.string.force_dark_activity_title);
        LinearLayout layout = findViewById(R.id.activity_medium_interstitial);
        layout.setOrientation(LinearLayout.VERTICAL);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = STRETCH_THIS_DIMENSION;
        int weight = 1; // equal weights for each WebView
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.setMargins(MARGIN_DP, MARGIN_DP, MARGIN_DP, MARGIN_DP);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            layout.addView(setupWebView(params, WebSettingsCompat.FORCE_DARK_ON));
            layout.addView(setupWebView(params, WebSettingsCompat.FORCE_DARK_OFF));
            layout.addView(setupWebView(params, WebSettingsCompat.FORCE_DARK_AUTO));
        } else {
            WebkitHelpers.showMessageInActivity(ForceDarkActivity.this,
                    R.string.webkit_api_not_available);
        }
    }

    private WebView setupWebView(LinearLayout.LayoutParams params, int forceDarkMode) {
        WebView webView = new WebView(this);
        webView.setLayoutParams(params);
        webView.setWebViewClient(new WebViewClient());
        switch (forceDarkMode) {
            case WebSettingsCompat.FORCE_DARK_ON:
                webView.loadData(String.format(DESCRIPTION, "ON"),
                        "text/html; charset=utf-8", "utf-8");
                break;
            case WebSettingsCompat.FORCE_DARK_OFF:
                webView.loadData(String.format(DESCRIPTION, "OFF"),
                        "text/html; charset=utf-8", "utf-8");
                break;
            case WebSettingsCompat.FORCE_DARK_AUTO:
                webView.loadData(String.format(DESCRIPTION, "AUTO"),
                        "text/html; charset=utf-8", "utf-8");
                break;
            default:
                throw new UnsupportedOperationException("Unknown force dark mode");
        }

        WebSettingsCompat.setForceDark(webView.getSettings(), forceDarkMode);
        return webView;
    }
}
