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
import static org.junit.Assert.fail;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ProxyControllerTest {
    private WebViewOnUiThread mWebViewOnUiThread;
    private MockWebServer mServer1;
    private MockWebServer mServer2;

    private static final long TEST_TIMEOUT = 5000L;

    @Before
    public void setUp() throws IOException {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
        mServer1 = new MockWebServer();
        mServer2 = new MockWebServer();
        mServer1.start();
        mServer2.start();
    }

    @After
    public void tearDown() throws IOException {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
        if (mServer1 != null) {
            mServer1.shutdown();
        }
        if (mServer2 != null) {
            mServer2.shutdown();
        }
    }

    /**
     * This test should have an equivalent in CTS when this function is implemented in the framework
     */
    @Test
    public void testProxyOverrideRuns() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);

        clearProxyOverrideSync();
        setProxyOverrideSync(new ProxyController.ProxyRules.ProxyRulesBuilder()
                .addProxyRule("myproxy.com:200").build());
        clearProxyOverrideSync();
        clearProxyOverrideSync();
    }

    /**
     * This test should have an equivalent in CTS when this function is implemented in the framework
     */
    @Test
    public void testProxyOverride() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);

        final String server1url = mServer1.url("/").toString();

        // Clear proxy override and load server 1 url
        clearProxyOverrideSync();
        mWebViewOnUiThread.loadUrl(server1url);
        assertNotNull(mServer1.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        int server1RequestCount = mServer1.getRequestCount();

        // Set proxy override and load server 1 url
        // Localhost should use proxy with "<-loopback>" rule
        setProxyOverrideSync(new ProxyController.ProxyRules.ProxyRulesBuilder()
                .addProxyRule(mServer2.getHostName() + ":" + mServer2.getPort())
                .addBypassRule("<-loopback>")
                .build());
        mWebViewOnUiThread.loadUrl(server1url);
        assertNotNull(mServer2.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        int server2RequestCount = mServer2.getRequestCount();
        assertEquals(server1RequestCount, mServer1.getRequestCount());

        // Set proxy override and load server 1 url
        // Localhost should not use proxy
        setProxyOverrideSync(new ProxyController.ProxyRules.ProxyRulesBuilder()
                .addProxyRule(mServer2.getHostName() + ":" + mServer2.getPort()).build());
        mWebViewOnUiThread.loadUrl(server1url);
        assertNotNull(mServer1.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(server2RequestCount, mServer2.getRequestCount());


        // Clear proxy override and load server 1 url
        clearProxyOverrideSync();
        mWebViewOnUiThread.loadUrl(server1url);
        assertNotNull(mServer1.takeRequest(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(server2RequestCount, mServer2.getRequestCount());
    }

    /**
     * This test should have an equivalent in CTS when this function is implemented in the framework
     */
    @Test
    public void testProxyOverrideValidInput() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);
        ProxyController.ProxyRules validRules = new ProxyController.ProxyRules
                .ProxyRulesBuilder()
                .addProxyRule("direct://")
                .addProxyRule("www.example.com")
                .addProxyRule("http://www.example.com")
                .addProxyRule("https://www.example.com")
                .addProxyRule("www.example.com:123")
                .addProxyRule("http://www.example.com:123")
                .addProxyRule("10.0.0.1")
                .addProxyRule("10.0.0.1:123")
                .addProxyRule("http://10.0.0.1")
                .addProxyRule("https://10.0.0.1")
                .addProxyRule("http://10.0.0.1:123")
                .addProxyRule("[FE80:CD00:0000:0CDE:1257:0000:211E:729C]")
                .addProxyRule("[FE80:CD00:0:CDE:1257:0:211E:729C]")
                .addBypassRule("www.rule.com")
                .addBypassRule("*.rule.com")
                .addBypassRule("*rule.com")
                .addBypassRule("www.*.com")
                .addBypassRule("www.rule*")
                .build();

        try {
            setProxyOverrideSync(validRules);
        } catch (Exception e) {
            fail("Exception in valid proxy urls and bypass rules");
        }
    }

    /**
     * This test should have an equivalent in CTS when this function is implemented in the framework
     */
    @Test
    public void testProxyOverrideInvalidInput() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);
        String[] proxyUrls = {
                null,
                "",
                "www.example1.com,www.example2.com",
                "www.exam,ple.com"};

        for (String proxyUrl : proxyUrls) {
            try {
                setProxyOverrideSync(new ProxyController.ProxyRules.ProxyRulesBuilder()
                        .addProxyRule(proxyUrl).build());
                fail("No exception in invalid proxy url: " + proxyUrl);
            } catch (Exception e) {
                // do nothing
            }
        }

        String[] bypassRules = {
                null,
                "",
                "20:example.com",
                "www.example1.com,www.example2.com",
                "www.exam,ple.com",
                "http://",
                "example.com:-20"};

        for (String rule : bypassRules) {
            try {
                setProxyOverrideSync(new ProxyController.ProxyRules.ProxyRulesBuilder()
                        .addBypassRule(rule).build());
                fail("No exception in invalid bypass rule: " + rule);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private void setProxyOverrideSync(final ProxyController.ProxyRules proxyRules)
            throws Exception {
        final ResolvableFuture<Void> future = ResolvableFuture.create();
        final Runnable listener = new Runnable() {
            @Override
            public void run() {
                future.set(null);
            }
        };
        onMainThread(new Callable<Void>() {
            @Override
            public Void call() {
                ProxyController.getInstance().setProxyOverride(proxyRules, listener);
                return null;
            }}).get();
        future.get();
    }

    private void clearProxyOverrideSync() throws Exception {
        final ResolvableFuture<Void> future = ResolvableFuture.create();
        final Runnable listener = new Runnable() {
            @Override
            public void run() {
                future.set(null);
            }
        };
        onMainThread(new Callable<Void>() {
            @Override
            public Void call() {
                ProxyController.getInstance().clearProxyOverride(listener);
                return null;
            }}).get();
        future.get();
    }

    private <T> ListenableFuture<T> onMainThread(final Callable<T> callable)  {
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
}
