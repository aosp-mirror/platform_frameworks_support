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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewProxyCompatTest {
    WebViewOnUiThread mWebViewOnUiThread;

    private static final long TEST_TIMEOUT = 5000L;

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
     * This test should have an equivalent in CTS when this function is implemented in the framework
     */
    @Test
    public void testProxyOverrideRuns() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);

        clearProxyOverrideSync();
        setProxyOverrideSync(new WebViewProxyCompat.ProxyRules().addRule("myproxy.com:200"));
        clearProxyOverrideSync();
        clearProxyOverrideSync();
    }

    /**
     * This test should have an equivalent in CTS when this function is implemented in the framework
     */
    @Test
    public void testProxyOverride() throws Exception {
        AssumptionUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);

        // Server 1 will be the default server
        MockWebServer server1 = null;
        // Server 2 will be the overriden proxy server
        MockWebServer server2 = null;

        try {
            server1 = new MockWebServer();
            server2 = new MockWebServer();
            server1.start();
            server2.start();
            final String server1url = server1.url("/").toString();

            // Clear proxy override and load server 1 url
            clearProxyOverrideSync();
            mWebViewOnUiThread.loadUrl(server1url);
            assertNotNull(server1.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
            int server1RequestCount = server1.getRequestCount();

            // Set proxy override and load server 1 url
            // Localhost should use proxy with "<-loopback>" rule
            setProxyOverrideSync(new WebViewProxyCompat.ProxyRules()
                    .addRule(server2.getHostName() + ":" + server2.getPort())
                    .addExclusionRule("<-loopback>"));
            mWebViewOnUiThread.loadUrl(server1url);
            assertNotNull(server2.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
            int server2RequestCount = server2.getRequestCount();
            assertEquals(server1RequestCount, server1.getRequestCount());

            // Set proxy override and load server 1 url
            // Localhost should not use proxy
            setProxyOverrideSync(new WebViewProxyCompat.ProxyRules()
                    .addRule(server2.getHostName() + ":" + server2.getPort()));
            mWebViewOnUiThread.loadUrl(server1url);
            assertNotNull(server1.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
            assertEquals(server2RequestCount, server2.getRequestCount());


            // Clear proxy override and load server 1 url
            clearProxyOverrideSync();
            mWebViewOnUiThread.loadUrl(server1url);
            assertNotNull(server1.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
            assertEquals(server2RequestCount, server2.getRequestCount());
        } finally {
            if (server1 != null) {
                server1.shutdown();
            }
            if (server2 != null) {
                server2.shutdown();
            }
        }
    }

    private void setProxyOverrideSync(WebViewProxyCompat.ProxyRules proxyRules)
            throws Exception {
        final CountDownLatch listenerLatch = new CountDownLatch(1);
        Runnable listener = new Runnable() {
            @Override
            public void run() {
                listenerLatch.countDown();
            }
        };
        WebViewProxyCompat.setProxyOverride(proxyRules, listener, null);
        assertTrue(listenerLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private void clearProxyOverrideSync() throws Exception {
        final CountDownLatch listenerLatch = new CountDownLatch(1);
        Runnable listener = new Runnable() {
            @Override
            public void run() {
                listenerLatch.countDown();
            }
        };
        WebViewProxyCompat.clearProxyOverride(listener, null);
        assertTrue(listenerLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
