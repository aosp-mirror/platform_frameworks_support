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

package com.example.androidx.webkit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebChromeClientCompat;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;

/**
 * An {@link Activity} to exercise Javascript Java interaction functionality.
 */
public class JsJavaInteractionActivity extends AppCompatActivity {
    private static class MyCallback extends WebMessagePortCompat.WebMessageCallbackCompat {
        @Override
        public void onMessage(WebMessagePortCompat port, WebMessageCompat message) {
            Log.w("chromium", "message = " + message.getData());
            WebMessageCompat reply = new WebMessageCompat("pong");
            port.postMessage(reply);
        }
    }

    private static class MyWebChromeClient extends WebChromeClientCompat {
        @Override
        public void onPostMessage(
                WebView view, String message, String sourceOrigin, WebMessagePortCompat[] ports) {
            Log.w("chromium", "message = " + message);
            Log.w("chromium", "sourceOrigin = " + sourceOrigin);
            for (WebMessagePortCompat port : ports) {
                port.setWebMessageCallback(new MyCallback());
            }
        }
    }

    private static class MyWebViewClient extends WebViewClientCompat {
        @Override
        public void onPageCommitVisible(WebView view, String url) {}
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_js_java_interaction);
        setTitle(R.string.js_java_interaction_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        WebView webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        WebViewCompat.setDocumentStartJavascript(webView, "const hello = 1;", false);

        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.loadUrl("https://cs.chromium.org");
    }
}
