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

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} to demonstrate medium ("Quiet") interstitials. WebView displays a
 * grey error page with a small bit of description when it's "medium" sized (large enough to show
 * text, but small enough that it's likely not the predominant part of the UI), when loading
 * malicious resources.
 * <p>
 * Medium interstitials are triggered when the WebView is either taller or wider than an otherwise
 * "small" WebView. This {@link android.app.Activity} can show either case ("tall" and "short",
 * respectively), based on the boolean extra {@link #LAYOUT_HORIZONTAL}.
 */
public class MediumInterstitialActivity extends AppCompatActivity {

    public static final String LAYOUT_HORIZONTAL = "layoutHorizontal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Decide whether to show the WebViews stacked on top of each other ("short") or
        // side-by-side ("tall").
        Intent intent = getIntent();
        boolean isHorizontal = intent.getBooleanExtra(LAYOUT_HORIZONTAL, true);

        if (isHorizontal) {
            setContentView(R.layout.activity_medium_short_interstitial);
            setTitle(R.string.medium_short_interstitial_activity_title);
        } else {
            setContentView(R.layout.activity_medium_tall_interstitial);
            setTitle(R.string.medium_tall_interstitial_activity_title);
        }
        WebkitHelpers.appendWebViewVersionToTitle(this);

        findWebViewAndLoadUrl(R.id.medium_webview_1, SafeBrowsingHelpers.MALWARE_URL);
        findWebViewAndLoadUrl(R.id.medium_webview_2, SafeBrowsingHelpers.PHISHING_URL);
        findWebViewAndLoadUrl(R.id.medium_webview_3, SafeBrowsingHelpers.UNWANTED_SOFTWARE_URL);
        findWebViewAndLoadUrl(R.id.medium_webview_4, SafeBrowsingHelpers.BILLING_URL);
        // Consider adding more threat types, if we support more.
    }

    private void findWebViewAndLoadUrl(@IdRes int webViewResId, String url) {
        WebView webView = findViewById(webViewResId);
        // A medium interstitial may have links on it; allow this WebView to handle opening those.
        webView.setWebViewClient(new WebViewClient());
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webview.getSettings(), true);
        }
        webView.loadUrl(url);
    }
}
