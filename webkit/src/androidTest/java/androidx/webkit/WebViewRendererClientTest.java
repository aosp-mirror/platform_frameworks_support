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

import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewRendererClientTest {
    private static final long TEST_TIMEOUT_MS = 20000L;

    private static class JSBlocker {
        private CountDownLatch mLatch;
        JSBlocker(int requiredReleaseCount) {
            mLatch = new CountDownLatch(requiredReleaseCount);
        }

        JSBlocker() {
            this(1);
        }

        public void releaseBlock() {
            mLatch.countDown();
        }

        @JavascriptInterface
        public void block() throws Exception {
            mLatch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    private <T> ListenableFuture<T> onMainThread(final Callable<T> callable) {
        final ResolvableFuture<T> future = ResolvableFuture.create();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(callable.call());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
    }

    private void setWebViewRendererClientOnMainThread(
            final WebView webView, final WebViewRendererClient webViewRendererClient) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewCompat.setWebViewRendererClient(webView, webViewRendererClient);
            }
        });
    }

    private ListenableFuture<WebViewRendererClient> getWebViewRendererClientOnMainThread(
            final WebView webView) {
        return onMainThread(new Callable<WebViewRendererClient>() {
            @Override
            public WebViewRendererClient call() {
                return WebViewCompat.getWebViewRendererClient(webView);
            }
        });
    }

    private void loadUrlSync(final WebView view, final String url) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        latch.countDown();
                    }
                });
                view.loadUrl(url);
            }
        });

        latch.await();
    }

    @Test
    public void testWebViewRendererClient() throws Throwable {
        AssumptionUtils.checkFeature(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE);
        final WebView webView = WebViewOnUiThread.createWebView();
        final JSBlocker blocker = new JSBlocker();
        final CountDownLatch latch = new CountDownLatch(1);

        final WebViewRendererClient webViewRendererClient = new WebViewRendererClient() {
            @Override
            public void onRendererUnresponsive(WebView view, WebViewRenderer renderer) {
                blocker.releaseBlock();
            }

            @Override
            public void onRendererResponsive(WebView view, WebViewRenderer renderer) {
                latch.countDown();
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                return false;
            }
        };

        setWebViewRendererClientOnMainThread(webView, webViewRendererClient);
        loadUrlSync(webView, "about:blank");

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(blocker, "blocker");
                webView.evaluateJavascript("blocker.block();", null);
                webView.dispatchKeyEvent(
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            }
        });
        latch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        WebViewOnUiThread.destroy(webView);
    }

    @Test
    public void testSetWebViewRendererClient() throws Throwable {
        AssumptionUtils.checkFeature(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE);
        final WebView webView = WebViewOnUiThread.createWebView();

        final WebViewRendererClient webViewRendererClient = new WebViewRendererClient() {
            @Override
            public void onRendererUnresponsive(WebView view, WebViewRenderer renderer) {}

            @Override
            public void onRendererResponsive(WebView view, WebViewRenderer renderer) {}

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                return false;
            }
        };

        Assert.assertNull("Initially the renderer client should be null",
                getWebViewRendererClientOnMainThread(webView).get());

        setWebViewRendererClientOnMainThread(webView, webViewRendererClient);

        Assert.assertSame(
                "After the renderer client is set, getting it should return the same object",
                webViewRendererClient, getWebViewRendererClientOnMainThread(webView).get());

        WebViewOnUiThread.destroy(webView);
    }
}
