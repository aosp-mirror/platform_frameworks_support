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
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;

/**
 * An {@link Activity} to exercise Proxy Override functionality.
 */
public class ProxyOverrideActivity extends AppCompatActivity {
    private static final String TAG = ProxyOverrideActivity.class.getSimpleName();

    private Proxy mProxy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy_override);
        setTitle(R.string.proxy_override_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        final String proxyUrl = getProxyUrl();
        setProxyOverride(proxyUrl);
    }

    private String getProxyUrl() {
        mProxy = new Proxy();
        mProxy.start();
        return "localhost:" + mProxy.getPort();
    }

    private void setProxyOverride(String proxyUrl) {
        ProxyController proxyController = ProxyController.getInstance();
        ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule(proxyUrl).build();
        proxyController.setProxyOverride(proxyConfig, () -> onProxyOverrideComplete());
    }

    private void onProxyOverrideComplete() {
        WebView webView = findViewById(R.id.proxy_override_webview);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                Log.w(TAG, "Title received: " + title);
            }
        });
        webView.loadUrl("http://www.google.com/");
    }

    @Override
    protected void onDestroy() {
        mProxy.shutdown();
        super.onDestroy();
    }

}
