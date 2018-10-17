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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewCompatTest {
    WebViewOnUiThread mWebViewOnUiThread;

    private static final long TEST_TIMEOUT = 20000L;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testVisualStateCallbackCalled. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testVisualStateCallbackCalled() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);

        final CountDownLatch callbackLatch = new CountDownLatch(1);
        final long kRequest = 100;

        mWebViewOnUiThread.loadUrl("about:blank");

        mWebViewOnUiThread.postVisualStateCallbackCompat(kRequest,
                new WebViewCompat.VisualStateCallback() {
                        public void onComplete(long requestId) {
                            assertEquals(kRequest, requestId);
                            callbackLatch.countDown();
                        }
                });

        assertTrue(callbackLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCheckThread() {
        // Skip this test if VisualStateCallback is not supported.
        AssumptionUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);
        try {
            WebViewCompat.postVisualStateCallback(mWebViewOnUiThread.getWebViewOnCurrentThread(), 5,
                    new WebViewCompat.VisualStateCallback() {
                        @Override
                        public void onComplete(long requestId) {
                        }
                    });
        } catch (RuntimeException e) {
            return;
        }
        fail("Calling a WebViewCompat method on the wrong thread must cause a run-time exception");
    }

    private static class MockContext extends ContextWrapper {
        private boolean mGetApplicationContextWasCalled;

        MockContext(Context context) {
            super(context);
        }

        public Context getApplicationContext() {
            mGetApplicationContextWasCalled = true;
            return super.getApplicationContext();
        }

        public boolean wasGetApplicationContextCalled() {
            return mGetApplicationContextWasCalled;
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingUseApplicationContext. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingUseApplicationContext() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        final MockContext ctx =
                new MockContext(InstrumentationRegistry.getTargetContext().getApplicationContext());
        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.startSafeBrowsing(ctx, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                assertTrue(ctx.wasGetApplicationContextCalled());
                resultLatch.countDown();
            }
        });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingWithNullCallbackDoesntCrash.
     * Modifications to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingWithNullCallbackDoesntCrash() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        WebViewCompat.startSafeBrowsing(InstrumentationRegistry.getTargetContext(), null);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingInvokesCallback. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingInvokesCallback() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.startSafeBrowsing(
                InstrumentationRegistry.getTargetContext().getApplicationContext(),
                new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        assertTrue(Looper.getMainLooper().isCurrentThread());
                        resultLatch.countDown();
                    }
                });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testSetSafeBrowsingWhitelistWithMalformedList. Modifications
     * to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    @Test
    public void testSetSafeBrowsingWhitelistWithMalformedList() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.SAFE_BROWSING_WHITELIST);

        List<String> whitelist = new ArrayList<>();
        // Protocols are not supported in the whitelist
        whitelist.add("http://google.com");
        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                assertFalse(success);
                resultLatch.countDown();
            }
        });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testSetSafeBrowsingWhitelistWithValidList. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @FlakyTest(bugId = 111690396)
    @Test
    public void testSetSafeBrowsingWhitelistWithValidList() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.SAFE_BROWSING_WHITELIST);
        // This test relies on the onSafeBrowsingHit callback to verify correctness.
        AssumptionUtils.checkFeature(WebViewFeature.SAFE_BROWSING_HIT);

        List<String> whitelist = new ArrayList<>();
        whitelist.add("safe-browsing");
        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                assertTrue(success);
                resultLatch.countDown();
            }
        });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));

        final CountDownLatch resultLatch2 = new CountDownLatch(1);
        mWebViewOnUiThread.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageFinished(WebView view, String url) {
                resultLatch2.countDown();
            }

            @Override
            public void onSafeBrowsingHit(@NonNull WebView view,
                    @NonNull WebResourceRequest request, int threatType,
                    @NonNull SafeBrowsingResponseCompat callback) {
                Assert.fail("Should not invoke onSafeBrowsingHit");
            }
        });

        mWebViewOnUiThread.loadUrl("chrome://safe-browsing/match?type=malware");

        // Wait until page load has completed
        assertTrue(resultLatch2.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetSafeBrowsingPrivacyPolicyUrl. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetSafeBrowsingPrivacyPolicyUrl() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL);

        assertNotNull(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl());
        try {
            new URL(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString());
        } catch (MalformedURLException e) {
            Assert.fail("The privacy policy URL should be a well-formed URL");
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetWebViewClient. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetWebViewClient() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_CLIENT);

        // Create a new WebView because WebViewOnUiThread sets a WebViewClient during
        // construction.
        WebView webView = WebViewOnUiThread.createWebView();

        // getWebViewClient should return a default WebViewClient if it hasn't been set yet
        WebViewClient client = WebViewOnUiThread.getWebViewClient(webView);
        assertNotNull(client);
        assertTrue(client instanceof WebViewClient);

        // getWebViewClient should return the client after it has been set
        WebViewClient client2 = new WebViewClient();
        assertNotSame(client, client2);
        WebViewOnUiThread.setWebViewClient(webView, client2);
        assertSame(client2, WebViewOnUiThread.getWebViewClient(webView));

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetWebChromeClient. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetWebChromeClient() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.GET_WEB_CHROME_CLIENT);

        // Create a new WebView because WebViewOnUiThread sets a WebChromeClient during
        // construction.
        WebView webView = WebViewOnUiThread.createWebView();

        // getWebChromeClient should return null if the client hasn't been set yet
        WebChromeClient client = WebViewOnUiThread.getWebChromeClient(webView);
        assertNull(client);

        // getWebChromeClient should return the client after it has been set
        WebChromeClient client2 = new WebChromeClient();
        assertNotSame(client, client2);
        WebViewOnUiThread.setWebChromeClient(webView, client2);
        assertSame(client2, WebViewOnUiThread.getWebChromeClient(webView));

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * WebViewCompat.getCurrentWebViewPackage should be null on pre-L devices.
     * On L+ devices WebViewCompat.getCurrentWebViewPackage should be null only in exceptional
     * circumstances - like when the WebView APK is being updated, or for Wear devices. The L+
     * devices used in support library testing should have a non-null WebView package.
     */
    @Test
    public void testGetCurrentWebViewPackage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            assertNull(WebViewCompat.getCurrentWebViewPackage(
                    InstrumentationRegistry.getTargetContext()));
        } else {
            assertNotNull(
                    WebViewCompat.getCurrentWebViewPackage(
                            InstrumentationRegistry.getTargetContext()));
        }
    }

    @Test
    public void testMockWebServerNotNull() throws Exception {
        MockWebServer mockServer = new MockWebServer();
        assertNotNull(mockServer);
    }

    /**
     * This test should have an equivalent in android.webkit.cts.WebViewTest
     * when this function is available in the framework.
     */
    @Test
    public void testProxyOverrideNullString() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);
        try {
            WebViewCompat.setProxyOverride(null, 200, null);
        } catch (NullPointerException e) {
            return;
        }
        fail("SetProxyOverride should not accept a null host string");
    }

    /**
     * This test should have an equivalent in android.webkit.cts.WebViewTest
     * when this function is available in the framework.
     */
    @Test
    public void testProxyOverrideNullStringExclusion() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);
        try {
            String[] exclusionList = {"excludedurl.com", null};
            WebViewCompat.setProxyOverride("myproxy.com", 200, exclusionList, null);
        } catch (NullPointerException e) {
            return;
        }
        fail("SetProxyOverride should not accept a null string in the excluded list");
    }

    /**
     * This test should have an equivalent in android.webkit.cts.WebViewTest
     * when this function is available in the framework.
     */
    @Test
    public void testProxyOverrideRuns() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);
        WebViewCompat.setProxyOverride("myproxy.com", 200, null);
        WebViewCompat.clearProxyOverride(null);
        WebViewCompat.setProxyOverride("myproxy.com", 200, null, null);
        WebViewCompat.clearProxyOverride(null);
        WebViewCompat.setProxyOverride("myproxy.com", 200, null, new Runnable() {
            @Override
            public void run() {
            }
        });
        WebViewCompat.clearProxyOverride(new Runnable() {
            @Override
            public void run() {
            }
        });
        WebViewCompat.setProxyOverride("myproxy.com", 200, new Runnable() {
            @Override
            public void run() {
            }
        });
        WebViewCompat.clearProxyOverride(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    /**
     * This test should have an equivalent in android.webkit.cts.WebViewTest
     * when this function is available in the framework.
     */
    @Test
    public void testProxyOverrideCallback() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);
        final CountDownLatch callbackLatch1 = new CountDownLatch(1);
        WebViewCompat.clearProxyOverride(new Runnable() {
            @Override
            public void run() {
                callbackLatch1.countDown();
            }
        });
        assertTrue(callbackLatch1.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        final CountDownLatch callbackLatch2 = new CountDownLatch(1);
        WebViewCompat.setProxyOverride("myproxy.com", 200, null, new Runnable() {
            @Override
            public void run() {
                callbackLatch2.countDown();
            }
        });
        assertTrue(callbackLatch2.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        final CountDownLatch callbackLatch3 = new CountDownLatch(1);
        WebViewCompat.clearProxyOverride(new Runnable() {
            @Override
            public void run() {
                callbackLatch3.countDown();
            }
        });
        assertTrue(callbackLatch3.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * This test should have an equivalent in android.webkit.cts.WebViewTest
     * when this function is available in the framework.
     */
    @Test
    public void testProxyOverride() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);

        long startTime = System.currentTimeMillis();

        // Server 1 will be the default server
        MockWebServer server1 = new MockWebServer();
        assertNotNull(server1);
        server1.start();
        final String server1url = server1.url("/").toString();

        // Server 2 will be the overriden proxy server
        MockWebServer server2 = new MockWebServer();
        assertNotNull(server2);
        server2.start();

        final WebView webView = WebViewOnUiThread.createWebView();
        WebViewCompat.clearProxyOverride(null);
        loadUrlOnMain(webView, server1url);
        assertNotNull(server1.takeRequest(2500, TimeUnit.MILLISECONDS));
        int server1RequestCount = server1.getRequestCount();

        // set proxy override
        WebViewCompat.setProxyOverride(server2.getHostName(), server2.getPort(), null);
        // regular url
        loadUrlOnMain(webView, server1url);
        assertNotNull(server2.takeRequest(2500, TimeUnit.MILLISECONDS));
        int server2RequestCount = server2.getRequestCount();
        assertEquals(server1RequestCount, server1.getRequestCount());

        // clear proxy override
        WebViewCompat.clearProxyOverride(null);
        loadUrlOnMain(webView, server1url);
        assertNotNull(server1.takeRequest(2500, TimeUnit.MILLISECONDS));
        assertEquals(server2RequestCount, server2.getRequestCount());

        server1.shutdown();
        server2.shutdown();
    }

    private void loadUrlOnMain(final WebView webView, final String url) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }
}
