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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} to exercise Proxy Override functionality.
 */
public class ProxyOverrideActivity extends AppCompatActivity {
    private Proxy mProxy;
    private String mProxyUrl;
    private WebView mWebView;
    private TextView mRequestCountTextView;
    private EditText mEditText;
    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy_override);
        setTitle(R.string.proxy_override_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        // Initialize proxy server
        mProxy = new Proxy(0, () -> mRequestCountTextView.setText(getResources().getString(
                    R.string.proxy_override_requests_served, mProxy.getRequestCount()))
        );
        mProxy.start();
        mProxyUrl = "localhost:" + mProxy.getPort();

        // Initialize views
        mRequestCountTextView = findViewById(R.id.proxy_override_textview);
        mRequestCountTextView.setText(getResources().getString(
                R.string.proxy_override_requests_served, 0));
        mWebView = findViewById(R.id.proxy_override_webview);
        mWebView.setWebViewClient(new WebViewClient());
        mInputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        mEditText = findViewById(R.id.proxy_override_edittext);
        mEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                String url = mEditText.getText().toString();
                if (!url.isEmpty()) {
                    if (!url.startsWith("http")) url = "http://" + url;
                    mWebView.loadUrl(url);
                    mEditText.setText("");
                }
                mInputMethodManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                mWebView.requestFocus();
                return true;
            }
            return false;
        });

        // Check for proxy override feature
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            // If feature is not supported, just show a warning in the webview
            mRequestCountTextView.setVisibility(View.GONE);
            mEditText.setVisibility(View.GONE);
            mWebView.loadData("<html><body>Proxy override not available</body></html>",
                    "text/html", "utf-8");
            return;
        }

        // Set proxy override
        setProxyOverride();
    }

    private void setProxyOverride() {
        ProxyController proxyController = ProxyController.getInstance();
        ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule(mProxyUrl).build();
        proxyController.setProxyOverride(proxyConfig, () -> onProxyOverrideComplete());
    }

    private void onProxyOverrideComplete() {
        // Your code goes here, after the proxy override callback was executed
        mWebView.loadUrl("http://www.google.com");
    }

    @Override
    protected void onDestroy() {
        mProxy.shutdown();
        super.onDestroy();
    }
}
