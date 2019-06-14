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

package androidx.webkit;

import android.content.ContextWrapper;
import android.net.Uri;
import android.webkit.WebResourceResponse;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import static androidx.webkit.WebViewAssetLoader.PathHandler;
import static androidx.webkit.WebViewAssetLoader.AssetsPathHandler;
import static androidx.webkit.WebViewAssetLoader.ResourcesPathHandler;
import androidx.webkit.internal.AssetHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderTest {
    private static final String TAG = "WebViewAssetLoaderTest";

    private static String readAsString(InputStream is, String encoding) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return new String(os.toByteArray(), encoding);
    }

    private static class MockAssetHelper extends AssetHelper {
        MockAssetHelper() {
            super(null);
        }

        @Override
        public InputStream openAsset(Uri uri) {
            return null;
        }

        @Override
        public InputStream openResource(Uri uri) {
            return null;
        }
    }

    private static class MockContext extends ContextWrapper {
        MockContext() {
            super(ApplicationProvider.getApplicationContext());
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() throws Throwable {
        final String contents = "Some content for testing\n";
        final String encoding = "utf-8";

        PathHandler pathHandler =
                new PathHandler("/test/") {
                    @Override
                    public InputStream handle(Uri url) {
                        try {
                            return new ByteArrayInputStream(contents.getBytes(encoding));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNull("opened a non-registered URL - should return null",
                assetLoader.shouldInterceptRequest(Uri.parse("https://foo.bar/")));
    }

    @Test
    @SmallTest
    public void testCustomDomain() throws Throwable {
        final String contents = "Some content for testing\n";
        final String encoding = "utf-8";

        PathHandler pathHandler =
                new PathHandler("/test/") {
                    @Override
                    public InputStream handle(Uri url) {
                        try {
                            return new ByteArrayInputStream(contents.getBytes(encoding));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .onDomain("test.myDomain.net")
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://test.myDomain.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNull("opened a non-registered URL - should return null",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("https://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testAllowingHttp() throws Throwable {
        final String contents = "Some content for testing\n";
        final String encoding = "utf-8";

        PathHandler pathHandler =
                new PathHandler("/test/") {
                    @Override
                    public InputStream handle(Uri url) {
                        try {
                            return new ByteArrayInputStream(contents.getBytes(encoding));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .allowHttp(true)
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNotNull("didn't match HTTP URL despite allowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testDisallowingHttp() throws Throwable {
        final String contents = "Some content for testing\n";
        final String encoding = "utf-8";

        PathHandler pathHandler =
                new PathHandler("/test/") {
                    @Override
                    public InputStream handle(Uri url) {
                        try {
                            return new ByteArrayInputStream(contents.getBytes(encoding));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNull("matched HTTP URL despite disallowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testHostAssets() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        PathHandler assetsPathHandler = new AssetsPathHandler("/assets/",
                new MockAssetHelper() {
                    @Override
                    public InputStream openAsset(Uri url) {
                        if (url.getPath().equals("www/test.html")) {
                            try {
                                return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return null;
                    }
                });
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                      .register(assetsPathHandler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/assets/www/test.html"));
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the URL but not the file and returned a null InputStream",
                response.getData());
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostResources() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        PathHandler resourcesPathHandler = new ResourcesPathHandler("/res/",
                new MockAssetHelper() {
                    @Override
                    public InputStream openResource(Uri uri) {
                        if (uri.getPath().equals("raw/test.html")) {
                            try {
                                return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return null;
                    }
                });
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                      .register(resourcesPathHandler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/res/raw/test.html"));
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the URL but not the file and returned a null InputStream",
                response.getData());
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }
}
