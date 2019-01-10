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
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewAssetLoader.AssetHostingDetails;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * An {@link Activity} to exercise Safe Browsing functionality.
 */
public class WebViewAssetLoaderActivity extends AppCompatActivity {

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request);
        }
    }

    private WebViewAssetLoader mAssetLoader;
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_webview_asset_loader);
        setTitle(R.string.webview_asset_loader_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mAssetLoader = new WebViewAssetLoader(this.getApplicationContext());
        mWebView = new WebView(this);
        mWebView.setWebViewClient(new MyWebViewClient());
        setContentView(mWebView);

        WebSettings webViewSettings = mWebView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        // Setting this off for security. Off by default for SDK versions < 16.
        webViewSettings.setAllowFileAccessFromFileURLs(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        // Keeping these off is less critical but still a good idea, especially
        // if your app is not using file:// or content:// URLs.
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        // Host file:///android_resources/... under http://androidplatform.net/res/...
        AssetHostingDetails webResHostingDetails = mAssetLoader.hostResources(
                                                    "androidplatform.net", "/res", true, true);
        // Host file:///android_assets/www/... under http://androidplatform.net/assets/www/...
        AssetHostingDetails webAssetsHostingDetails =
                    mAssetLoader.hostAssets("androidplatform.net", "www/", "/assets/www/",
                                        true, true);
        Uri.Builder path = webAssetsHostingDetails.getHttpsPrefix()
                                                .buildUpon()
                                                .appendPath("ajax_requests.html");

        mWebView.loadUrl(path.toString());
    }
}
