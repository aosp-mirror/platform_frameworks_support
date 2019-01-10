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

/**
 * An {@link Activity} to show a more useful usecase: performing ajax calls to load files from
 * local app assets and resources in a safer way using WebViewAssetLoader.
 */
public class WebViewAssetLoaderAjaxActivity extends AppCompatActivity {

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
        setTitle(R.string.webview_asset_loader_ajax_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mAssetLoader = new WebViewAssetLoader(this.getApplicationContext());
        mWebView = new WebView(this);
        mWebView.setWebViewClient(new MyWebViewClient());
        setContentView(mWebView);

        WebSettings webViewSettings = mWebView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        // Setting this off for security. Off by default for SDK versions >= 16.
        webViewSettings.setAllowFileAccessFromFileURLs(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        // Keeping these off is less critical but still a good idea, especially
        // if your app is not using file:// or content:// URLs.
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        // Host android resources ... under http(s)://appassets.androidplatform.net/res/...
        mAssetLoader.hostResources();
        // Host the app assets under http(s)://appassets.androidplatform.net/assets/...
        mAssetLoader.hostAssets();
        Uri.Builder path = mAssetLoader.getAssetsHttpsPrefix().buildUpon()
                                                .appendPath("www/ajax_requests.html");

        mWebView.loadUrl(path.toString());
    }
}
