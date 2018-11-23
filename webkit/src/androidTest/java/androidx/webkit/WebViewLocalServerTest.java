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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.filters.SmallTest;
import androidx.test.filters.MediumTest;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
public class WebViewLocalServerTest {
    private final static String TAG = "WebViewAssetServerTest";

    private static class RandomString {
        private static final Random random = new Random();

        public static String next(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; ++i) {
                sb.append('a' + random.nextInt('z' - 'a'));
            }
            return sb.toString();
        }
    }

    private static String readAsString(InputStream is, String encoding) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return new String(os.toByteArray(), encoding);
        } catch (IOException e) {
            Log.e(TAG, "exception when reading the string", e);
            return "";
        }
    }

    private static class MockProtocolHandler extends WebViewLocalServer.AndroidProtocolHandler {
        public MockProtocolHandler() {
            super(null);
        }

        @Override
        public InputStream openAsset(String path) throws IOException {
            return null;
        }

        @Override
        public InputStream openResource(Uri uri) {
            return null;
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() {
        WebViewLocalServer assetServer = new WebViewLocalServer(new MockProtocolHandler());
        final String contents = RandomString.next(2000);
        final String encoding = "utf-8";

        WebViewLocalServer.PathHandler pathHandler = new WebViewLocalServer.PathHandler() {
            @Override
            public String getEncoding() {
                return encoding;
            }

            @Override
            public InputStream handle(Uri url) {
                try {
                    return new ByteArrayInputStream(contents.getBytes(encoding));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "exception when creating response", e);
                }
                return null;
            }
        };

        String url = "http://androidplatform.net/test";
        Uri uri = Uri.parse(url);
        assetServer.register(uri, pathHandler);
        WebResourceResponse response = assetServer.shouldInterceptRequest(url);

        Assert.assertEquals(encoding, response.getEncoding());
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNull(assetServer.shouldInterceptRequest("http://foo.bar/"));
    }

    @Test
    @SmallTest
    public void testHostAssets() {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewLocalServer assetServer = new WebViewLocalServer(new MockProtocolHandler() {
            @Override
            public InputStream openAsset(String path) throws IOException {
                if (path.equals("/www/test.html")) {
                    return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                }
                return null;
            }
        });

        WebViewLocalServer.AssetHostingDetails details =
                assetServer.hostAssets("androidplatform.net", "/www", "/assets", true, true);
        Assert.assertEquals(details.getHttpPrefix(), Uri.parse("http://androidplatform.net/assets"));
        Assert.assertEquals(details.getHttpsPrefix(), Uri.parse("https://androidplatform.net/assets"));

        WebResourceResponse response =
                assetServer.shouldInterceptRequest("http://androidplatform.net/assets/test.html");
        Assert.assertNotNull(response);
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostResources() {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewLocalServer assetServer = new WebViewLocalServer(new MockProtocolHandler() {
            @Override
            public InputStream openResource(Uri uri) {
                Log.i(TAG, "host res: " + uri);
                try {
                    if (uri.getPath().equals("/res/raw/test.html")) {
                        return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "exception when creating response", e);
                }
                return null;
            }
        });

        WebViewLocalServer.AssetHostingDetails details =
            assetServer.hostResources("androidplatform.net", "/res", true, true);
        Assert.assertEquals(details.getHttpPrefix(), Uri.parse("http://androidplatform.net/res"));
        Assert.assertEquals(details.getHttpsPrefix(), Uri.parse("https://androidplatform.net/res"));

        WebResourceResponse response =
                assetServer.shouldInterceptRequest("http://androidplatform.net/res/raw/test.html");
        Assert.assertNotNull(response);
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }


    public static class TestActivity extends Activity {
        private class MyWebViewClient extends WebViewClient {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mOnPageFinishedUrl.add(url);
            }

            @SuppressWarnings({"deprecated"})
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return mAssetServer.shouldInterceptRequest(url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return mAssetServer.shouldInterceptRequest(request);
            }
        }

        private WebViewLocalServer mAssetServer;
        private WebView mWebView;
        private ArrayBlockingQueue<String> mOnPageFinishedUrl = new ArrayBlockingQueue<String>(5);

        public WebViewLocalServer getAssetServer() {
            return mAssetServer;

        }

        public WebView getWebView() {
            return mWebView;
        }

        public ArrayBlockingQueue<String> getOnPageFinishedUrl() {
            return mOnPageFinishedUrl;
        }

        private void setUpWebView(WebView view) {
            view.setWebViewClient(new MyWebViewClient());
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAssetServer = new WebViewLocalServer(this);
            mWebView = new WebView(this);
            setUpWebView(mWebView);
            setContentView(mWebView);
        }

       @Override
       protected void onDestroy() {
           super.onDestroy();
           mWebView.destroy();
           mWebView = null;
       }
    }


    @Rule
    public final ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Test
    @MediumTest
    public void integrationTest_testAssetHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();
        final AtomicReference<String> url = new AtomicReference<String>();
        final String test_with_title_path = "test_with_title.html";

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewLocalServer.AssetHostingDetails hostingDetails =
                        activity.getAssetServer().hostAssets("www/", "/", true, true);
                Uri.Builder testPath =
                        hostingDetails.getHttpPrefix().buildUpon().appendPath(test_with_title_path);
                url.set(testPath.toString());
                android.util.Log.i("test", "loading: " + url.get());
                activity.getWebView().loadUrl(url.get());
            }
        });

        String onPageFinishedUrl = activity.getOnPageFinishedUrl().take();
        Assert.assertEquals(url.get(), onPageFinishedUrl);

        final AtomicReference<String> title = new AtomicReference<String>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                title.set(activity.getWebView().getTitle());
            }
        });
        Assert.assertEquals("WebViewVirtualServerTest", title.get());
    }

    @Test
    @MediumTest
    public void integrationTest_testResourcesHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();
        final AtomicReference<String> url = new AtomicReference<String>();
        final String test_with_title_path = "test_with_title.html";

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewLocalServer.AssetHostingDetails hostingDetails =
                    activity.getAssetServer().hostResources();
                Uri.Builder testPath =
                        hostingDetails.getHttpPrefix().buildUpon()
                        .appendPath("res")
                        .appendPath("raw")
                        .appendPath(test_with_title_path);
                url.set(testPath.toString());
                activity.getWebView().loadUrl(url.get());
            }
        });

        String onPageFinishedUrl = activity.getOnPageFinishedUrl().take();
        Assert.assertEquals(url.get(), onPageFinishedUrl);

        final AtomicReference<String> title = new AtomicReference<String>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                title.set(activity.getWebView().getTitle());
            }
        });
        Assert.assertEquals("WebViewVirtualServerTest", title.get());
    }
}
