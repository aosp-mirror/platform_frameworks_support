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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.InstrumentationRegistry;

import java.util.concurrent.Callable;

/**
 * A wrapper around a WebView instance, to run View methods on the UI thread. This also includes
 * static helper methods related to the UI thread.
 *
 * This should remain functionally equivalent to android.webkit.cts.WebViewOnUiThread.
 * Modifications to this class should be reflected in that class as necessary. See
 * http://go/modifying-webview-cts.
 */
class WebViewOnUiThread {
    /**
     * The maximum time, in milliseconds (10 seconds) to wait for a load
     * to be triggered.
     */
    private static final long LOAD_TIMEOUT = 10000;

    /**
     * Set to true after onPageFinished is called.
     */
    private boolean mLoaded;

    /**
     * The progress, in percentage, of the page load. Valid values are between
     * 0 and 100.
     */
    private int mProgress;

    /**
     * The WebView that calls will be made on.
     */
    private WebView mWebView;

    public WebViewOnUiThread() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView = new WebView(InstrumentationRegistry.getTargetContext());
                mWebView.setWebViewClient(new WaitForLoadedClient(WebViewOnUiThread.this));
                mWebView.setWebChromeClient(new WaitForProgressClient(WebViewOnUiThread.this));
            }
=======
        this(WebkitUtils.onMainThreadSync(() -> {
            return new WebView(ApplicationProvider.getApplicationContext());
        }));
    }

    /**
     * Create a new WebViewOnUiThread wrapping the provided {@link WebView}.
     */
    public WebViewOnUiThread(final WebView webView) {
        WebkitUtils.onMainThreadSync(() -> {
            mWebView = webView;
            mWebView.setWebViewClient(new WaitForLoadedClient(WebViewOnUiThread.this));
            mWebView.setWebChromeClient(new WaitForProgressClient(WebViewOnUiThread.this));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        });
    }

    private static class Holder {
        volatile WebView mView;
    }

    public static WebView createWebView() {
        final Holder h = new Holder();
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        final Context ctx = InstrumentationRegistry.getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                h.mView = new WebView(ctx);
            }
=======
        final Context ctx = ApplicationProvider.getApplicationContext();
        WebkitUtils.onMainThreadSync(() -> {
            h.mView = new WebView(ctx);
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        });
        return h.mView;
    }

    /**
     * Called after a test is complete and the WebView should be disengaged from
     * the tests.
     */
    public void cleanUp() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView.clearHistory();
                mWebView.clearCache(true);
                mWebView.setWebChromeClient(null);
                mWebView.setWebViewClient(null);
                mWebView.destroy();
            }
=======
        WebkitUtils.onMainThreadSync(() -> {
            mWebView.clearHistory();
            mWebView.clearCache(true);
            mWebView.setWebChromeClient(null);
            mWebView.setWebViewClient(null);
            mWebView.destroy();
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        });
    }

    /**
     * Called from WaitForLoadedClient.
     */
    @SuppressWarnings("EmptyMethod")
    synchronized void onPageStarted() {}

    /**
     * Called from WaitForLoadedClient, this is used to indicate that
     * the page is loaded, but not drawn yet.
     */
    synchronized void onPageFinished() {
        mLoaded = true;
        this.notifyAll();
    }

    /**
     * Called from the WebChrome client, this sets the current progress
     * for a page.
     * @param progress The progress made so far between 0 and 100.
     */
    synchronized void onProgressChanged(int progress) {
        mProgress = progress;
        this.notifyAll();
    }

    public static void destroy(final WebView webView) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.destroy();
            }
        });
=======
        WebkitUtils.onMainThreadSync(() -> webView.destroy());
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public void setWebViewClient(final WebViewClient webviewClient) {
        setWebViewClient(mWebView, webviewClient);
    }

    public static void setWebViewClient(
            final WebView webView, final WebViewClient webviewClient) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.setWebViewClient(webviewClient);
            }
        });
=======
        WebkitUtils.onMainThreadSync(() -> webView.setWebViewClient(webviewClient));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public void setWebChromeClient(final WebChromeClient webChromeClient) {
        setWebChromeClient(mWebView, webChromeClient);
    }

    public static void setWebChromeClient(
            final WebView webView, final WebChromeClient webChromeClient) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.setWebChromeClient(webChromeClient);
            }
        });
=======
        WebkitUtils.onMainThreadSync(() -> webView.setWebChromeClient(webChromeClient));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
=======
    public void setWebViewRenderProcessClient(
            final WebViewRenderProcessClient webViewRenderProcessClient) {
        setWebViewRenderProcessClient(mWebView, webViewRenderProcessClient);
    }

    public static void setWebViewRenderProcessClient(
            final WebView webView, final WebViewRenderProcessClient webViewRenderProcessClient) {
        WebkitUtils.onMainThreadSync(() -> {
            WebViewCompat.setWebViewRenderProcessClient(webView, webViewRenderProcessClient);
        });
    }

    public void setWebViewRenderProcessClient(
            final Executor executor, final WebViewRenderProcessClient webViewRenderProcessClient) {
        setWebViewRenderProcessClient(mWebView, executor, webViewRenderProcessClient);
    }

    public static void setWebViewRenderProcessClient(
            final WebView webView,
            final Executor executor,
            final WebViewRenderProcessClient webViewRenderProcessClient) {
        WebkitUtils.onMainThreadSync(() -> {
            WebViewCompat.setWebViewRenderProcessClient(
                    webView, executor, webViewRenderProcessClient);
        });
    }

    public WebViewRenderProcessClient getWebViewRenderProcessClient() {
        return getWebViewRenderProcessClient(mWebView);
    }

    public static WebViewRenderProcessClient getWebViewRenderProcessClient(
            final WebView webView) {
        return WebkitUtils.onMainThreadSync(() -> {
            return WebViewCompat.getWebViewRenderProcessClient(webView);
        });
    }

>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    public WebMessagePortCompat[] createWebMessageChannelCompat() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        return getValue(new ValueGetter<WebMessagePortCompat[]>() {
            @Override
            public WebMessagePortCompat[] capture() {
                return WebViewCompat.createWebMessageChannel(mWebView);
            }
        });
=======
        return WebkitUtils.onMainThreadSync(() -> WebViewCompat.createWebMessageChannel(mWebView));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public void postWebMessageCompat(final WebMessageCompat message, final Uri targetOrigin) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewCompat.postWebMessage(mWebView, message, targetOrigin);
            }
=======
        WebkitUtils.onMainThreadSync(() -> {
            WebViewCompat.postWebMessage(mWebView, message, targetOrigin);
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        });
    }

    public void addJavascriptInterface(final Object object, final String name) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView.addJavascriptInterface(object, name);
            }
        });
=======
        WebkitUtils.onMainThreadSync(() -> mWebView.addJavascriptInterface(object, name));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    /**
     * Calls loadUrl on the WebView and then waits onPageFinished
     * and onProgressChange to reach 100.
     * Test fails if the load timeout elapses.
     * @param url The URL to load.
     */
    void loadUrlAndWaitForCompletion(final String url) {
        callAndWait(() -> mWebView.loadUrl(url));
    }

    public void loadUrl(final String url) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
=======
        WebkitUtils.onMainThreadSync(() -> mWebView.loadUrl(url));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    /**
     * Calls {@link WebView#loadData} on the WebView and then waits onPageFinished
     * and onProgressChange to reach 100.
     * Test fails if the load timeout elapses.
     * @param data The data to load.
     * @param mimeType The mimeType to pass to loadData.
     * @param encoding The encoding to pass to loadData.
     */
    public void loadDataAndWaitForCompletion(@NonNull final String data,
            @Nullable final String mimeType, @Nullable final String encoding) {
        callAndWait(() -> mWebView.loadData(data, mimeType, encoding));
    }

    public void loadDataWithBaseURLAndWaitForCompletion(final String baseUrl,
            final String data, final String mimeType, final String encoding,
            final String historyUrl) {
        callAndWait(() -> {
            mWebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding,
                    historyUrl);
        });
    }

    /**
     * Use this only when JavaScript causes a page load to wait for the
     * page load to complete. Otherwise use loadUrlAndWaitForCompletion or
     * similar functions.
     */
    void waitForLoadCompletion() {
        waitForCriteria(LOAD_TIMEOUT, () -> isLoaded());
        clearLoad();
    }

    private void waitForCriteria(long timeout, Callable<Boolean> doneCriteria) {
        if (isUiThread()) {
            waitOnUiThread(timeout, doneCriteria);
        } else {
            waitOnTestThread(timeout, doneCriteria);
        }
    }

    public String getTitle() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        return getValue(new ValueGetter<String>() {
            @Override
            public String capture() {
                return mWebView.getTitle();
            }
        });
=======
        return WebkitUtils.onMainThreadSync(() -> mWebView.getTitle());
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public WebSettings getSettings() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        return getValue(new ValueGetter<WebSettings>() {
            @Override
            public WebSettings capture() {
                return mWebView.getSettings();
            }
        });
=======
        return WebkitUtils.onMainThreadSync(() -> mWebView.getSettings());
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public String getUrl() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        return getValue(new ValueGetter<String>() {
            @Override
            public String capture() {
                return mWebView.getUrl();
            }
        });
=======
        return WebkitUtils.onMainThreadSync(() -> mWebView.getUrl());
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public void postVisualStateCallbackCompat(final long requestId,
            final WebViewCompat.VisualStateCallback callback) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewCompat.postVisualStateCallback(mWebView, requestId, callback);
            }
=======
        WebkitUtils.onMainThreadSync(() -> {
            WebViewCompat.postVisualStateCallback(mWebView, requestId, callback);
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        });
    }

    void evaluateJavascript(final String script, final ValueCallback<String> result) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView.evaluateJavascript(script, result);
            }
        });
=======
        WebkitUtils.onMainThreadSync(() -> mWebView.evaluateJavascript(script, result));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public WebViewClient getWebViewClient() {
        return getWebViewClient(mWebView);
    }

    public static WebViewClient getWebViewClient(final WebView webView) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        return getValue(new ValueGetter<WebViewClient>() {
            @Override
            public WebViewClient capture() {
                return WebViewCompat.getWebViewClient(webView);
            }
        });
=======
        return WebkitUtils.onMainThreadSync(() -> WebViewCompat.getWebViewClient(webView));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    public WebChromeClient getWebChromeClient() {
        return getWebChromeClient(mWebView);
    }

    public static WebChromeClient getWebChromeClient(final WebView webView) {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        return getValue(new ValueGetter<WebChromeClient>() {
            @Override
            public WebChromeClient capture() {
                return WebViewCompat.getWebChromeClient(webView);
            }
        });
=======
        return WebkitUtils.onMainThreadSync(() -> WebViewCompat.getWebChromeClient(webView));
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    WebView getWebViewOnCurrentThread() {
        return mWebView;
    }

    private static <T> T getValue(ValueGetter<T> getter) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(getter);
        return getter.getValue();
    }

<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
    private abstract static class ValueGetter<T> implements Runnable {
        private T mValue;

        @Override
        public void run() {
            mValue = capture();
        }

        protected abstract T capture();

        public T getValue() {
            return mValue;
        }
=======
    /**
     * Capture a bitmap representation of the current WebView state.
     *
     * This synchronises so that the bitmap contents reflects the current DOM state, rather than
     * potentially capturing a previously generated frame.
     */
    public Bitmap captureBitmap() {
        getSettings().setOffscreenPreRaster(true);
        waitForDOMReadyToRender();
        return WebkitUtils.onMainThreadSync(() -> {
            Bitmap bitmap = Bitmap.createBitmap(mWebView.getWidth(), mWebView.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            mWebView.draw(canvas);
            return bitmap;
        });
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    }

    /**
     * Returns true if the current thread is the UI thread based on the
     * Looper.
     */
    private static boolean isUiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    /**
     * @return Whether or not the load has finished.
     */
    private synchronized boolean isLoaded() {
        return mLoaded && mProgress == 100;
    }

    /**
     * Makes a WebView call, waits for completion and then resets the
     * load state in preparation for the next load call.
     * @param call The call to make on the UI thread prior to waiting.
     */
    private void callAndWait(Runnable call) {
        assertTrue("WebViewOnUiThread.load*AndWaitForCompletion calls "
                        + "may not be mixed with load* calls directly on WebView "
                        + "without calling waitForLoadCompletion after the load",
                !isLoaded());
        clearLoad(); // clear any extraneous signals from a previous load.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(call);
        waitForLoadCompletion();
    }

    /**
     * Called whenever a load has been completed so that a subsequent call to
     * waitForLoadCompletion doesn't return immediately.
     */
    private synchronized void clearLoad() {
        mLoaded = false;
        mProgress = 0;
    }

    /**
     * Uses a polling mechanism, while pumping messages to check when the
     * criteria is met.
     */
    private void waitOnUiThread(long timeout, final Callable<Boolean> doneCriteria) {
        new PollingCheck(timeout) {
            @Override
            protected boolean check() {
                pumpMessages();
                try {
                    return doneCriteria.call();
                } catch (Exception e) {
                    fail("Unexpected error while checking the criteria: " + e.getMessage());
                    return true;
                }
            }
        }.run();
    }

    /**
     * Uses a wait/notify to check when the criteria is met.
     */
    private synchronized void waitOnTestThread(long timeout, Callable<Boolean> doneCriteria) {
        try {
            long waitEnd = SystemClock.uptimeMillis() + timeout;
            long timeRemaining = timeout;
            while (!doneCriteria.call() && timeRemaining > 0) {
                this.wait(timeRemaining);
                timeRemaining = waitEnd - SystemClock.uptimeMillis();
            }
            assertTrue("Action failed to complete before timeout", doneCriteria.call());
        } catch (InterruptedException e) {
            // We'll just drop out of the loop and fail
        } catch (Exception e) {
            fail("Unexpected error while checking the criteria: " + e.getMessage());
        }
    }

    /**
     * Pumps all currently-queued messages in the UI thread and then exits.
     * This is useful to force processing while running tests in the UI thread.
     */
    private void pumpMessages() {
        class ExitLoopException extends RuntimeException {
        }

        // Force loop to exit when processing this. Loop.quit() doesn't
        // work because this is the main Loop.
        mWebView.getHandler().post(new Runnable() {
            @Override
            public void run() {
                throw new ExitLoopException(); // exit loop!
            }
        });
        try {
            // Pump messages until our message gets through.
            Looper.loop();
        } catch (ExitLoopException e) {
        }
    }

    /**
     * A WebChromeClient used to capture the onProgressChanged for use
     * in waitFor functions. If a test must override the WebChromeClient,
     * it can derive from this class or call onProgressChanged
     * directly.
     */
    public static class WaitForProgressClient extends WebChromeClient {
        private WebViewOnUiThread mOnUiThread;

        WaitForProgressClient(WebViewOnUiThread onUiThread) {
            mOnUiThread = onUiThread;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mOnUiThread.onProgressChanged(newProgress);
        }
    }

    /**
     * A WebViewClient that captures the onPageFinished for use in
     * waitFor functions. Using initializeWebView sets the WaitForLoadedClient
     * into the WebView. If a test needs to set a specific WebViewClient and
     * needs the waitForCompletion capability then it should derive from
     * WaitForLoadedClient or call WebViewOnUiThread.onPageFinished.
     */
    public static class WaitForLoadedClient extends WebViewClientCompat {
        private WebViewOnUiThread mOnUiThread;

        WaitForLoadedClient(WebViewOnUiThread onUiThread) {
            mOnUiThread = onUiThread;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mOnUiThread.onPageFinished();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mOnUiThread.onPageStarted();
        }
    }
}
