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

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.concurrent.futures.SettableFuture;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewRendererCompatTest {
    private <T> ListenableFuture<T> onMainThread(final Callable<T> callable)  {
        final SettableFuture<T> future = SettableFuture.create();
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

    private ListenableFuture<Boolean> terminateRendererOnUiThread(
            final WebViewRendererCompat renderer) {
        return onMainThread(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return renderer.terminate();
            }
        });
    }

    ListenableFuture<Boolean> destroyWebViewOnUiThread(final WebView webView) {
        return onMainThread(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                webView.destroy();
                return true;
            }
        });
    }

    ListenableFuture<WebViewRendererCompat> getRendererOnUiThread(final WebView webView) {
        return onMainThread(new Callable<WebViewRendererCompat>() {
            @Override
            public WebViewRendererCompat call() {
                return WebViewCompat.getWebViewRenderer(webView);
            }
        });
    }

    private ListenableFuture<WebViewRendererCompat> startAndGetRenderer(
            final WebView webView) throws Throwable {
        final SettableFuture<WebViewRendererCompat> future = SettableFuture.create();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        WebViewRendererCompat result = WebViewCompat.getWebViewRenderer(webView);
                        future.set(result);
                    }
                });
                webView.loadUrl("about:blank");
            }
        });

        return future;
    }

    ListenableFuture<Boolean> catchRendererTermination(final WebView webView) {
        final SettableFuture<Boolean> future = SettableFuture.create();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean onRenderProcessGone(
                            WebView view,
                            RenderProcessGoneDetail detail) {
                        view.destroy();
                        future.set(true);
                        return true;
                    }
                });
            }
        });

        return future;
    }

    @Test
    public void testGetWebViewRendererPreO() throws Throwable {
        AssumptionUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER);
        Assume.assumeTrue(
                "Skipping single-process test on O+ device",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O);

        // It should not be possible to get a renderer pre-O
        WebView webView = WebViewOnUiThread.createWebView();
        Assert.assertNull(startAndGetRenderer(webView));

        destroyWebViewOnUiThread(webView).get();
    }

    @Test
    @SuppressLint("NewApi")
    public void testGetWebViewRenderer() throws Throwable {
        AssumptionUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER);
        Assume.assumeTrue(
                "Skipping multi-process test on pre-O device",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);

        // It should be possible to get a renderer post-O
        final WebView webView = WebViewOnUiThread.createWebView();

        // Before doing anything that starts a renderer, it should still be possible to obtain a
        // renderer handle, but not to terminate it.
        final WebViewRendererCompat preStartRenderer = getRendererOnUiThread(webView).get();
        Assert.assertFalse(terminateRendererOnUiThread(preStartRenderer).get());

        // After doing something that causes a renderer to be started, getWebViewRenderer should
        // return the same renderer object.
        final WebViewRendererCompat renderer = startAndGetRenderer(webView).get();
        Assert.assertSame(renderer, preStartRenderer);

        // The same renderer object should be returned if getWebViewRenderer is called a second time
        // after a (same origin) navigation.
        Assert.assertSame(renderer, startAndGetRenderer(webView).get());

        // We should be able to terminate the renderer, and it should cause onRenderProcessGone to
        // be called.
        ListenableFuture<Boolean> terminationFuture = catchRendererTermination(webView);
        Assert.assertTrue(terminateRendererOnUiThread(renderer).get());
        // The renderer should actually terminate.
        Assert.assertTrue(terminationFuture.get());


        // Calling terminate a second time should return false
        Assert.assertFalse(terminateRendererOnUiThread(renderer).get());

        // After we create a new webview, the new renderer object should be different to the old
        // one.
        final WebView webView2 = WebViewOnUiThread.createWebView();
        Assert.assertNotSame(renderer, startAndGetRenderer(webView2).get());

        // webView is implicitly destroyed by the WebViewClient installed by
        // catchRendererTermination
        destroyWebViewOnUiThread(webView2).get();
    }
}
