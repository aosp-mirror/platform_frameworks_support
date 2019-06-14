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

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderTest {
    private static final String TAG = "WebViewAssetLoaderTest";

    private static final String CONTENTS = "Some content for testing";
    private static final String ENCODING = "utf-8";

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

    private static class TestPathHandler extends PathHandler {

        public TestPathHandler(@NonNull String registeredPath) {
            super(registeredPath);
        }

        @Override
        public WebResourceResponse handle(Uri url) {
            try {
                InputStream is = new ByteArrayInputStream(CONTENTS.getBytes(ENCODING));
                return new WebResourceResponse(null, null, is);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() throws Throwable {
        PathHandler pathHandler = new TestPathHandler("/test/");
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(CONTENTS, readAsString(response.getData(), ENCODING));

        Assert.assertNull("opened a non-registered URL - should return null",
                assetLoader.shouldInterceptRequest(Uri.parse("https://foo.bar/")));
    }

    @Test
    @SmallTest
    public void testCustomDomain() throws Throwable {
        PathHandler pathHandler = new TestPathHandler("/test/");
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .onDomain("test.myDomain.net")
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://test.myDomain.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(CONTENTS, readAsString(response.getData(), ENCODING));

        Assert.assertNull("opened a non-registered URL - should return null",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("https://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testAllowingHttp() throws Throwable {
        PathHandler pathHandler = new TestPathHandler("/test/");
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .allowHttp(true)
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(CONTENTS, readAsString(response.getData(), ENCODING));

        Assert.assertNotNull("didn't match HTTP URL despite allowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testDisallowingHttp() throws Throwable {
        PathHandler pathHandler = new TestPathHandler("/test/");
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .register(pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        Assert.assertNotNull("didn't match the registered URL", response);
        Assert.assertEquals(CONTENTS, readAsString(response.getData(), ENCODING));

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
                                return new ByteArrayInputStream(testHtmlContents.getBytes(ENCODING));
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
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), ENCODING));
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
                                return new ByteArrayInputStream(testHtmlContents.getBytes(ENCODING));
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
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), ENCODING));
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlers() throws Throwable {
        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder();
        for (int i = 1; i <= 5; ++i) {
            final String testContent = CONTENTS + Integer.toString(i);
            builder.register(new PathHandler("/test_path_" + Integer.toString(i) + "/") {
                @Override
                public WebResourceResponse handle(Uri url) {
                    try {
                        InputStream is = new ByteArrayInputStream(testContent.getBytes(ENCODING));
                        return new WebResourceResponse(null, null, is);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        WebViewAssetLoader assetLoader = builder.build();

        for (int i = 5; i >= 1; --i) {
            WebResourceResponse response = assetLoader.shouldInterceptRequest(
                    Uri.parse("https://appassets.androidplatform.net/test_path_"
                            + Integer.toString(i) + "/"));
            Assert.assertNotNull("failed to match the URL and returned null response", response);
            Assert.assertNotNull("matched the URL but not the file and returned a null InputStream",
                    response.getData());
            Assert.assertEquals(CONTENTS + Integer.toString(i),
                    readAsString(response.getData(), ENCODING));
        }
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlersOnTheSamePath() throws Throwable {
        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder();
        List<PathHandler> pathHandlers = new ArrayList<>();
        pathHandlers.add(new PathHandler("/test_path/") {
            @Override
            public WebResourceResponse handle(Uri url) {
                try {
                    if (url.getPath().endsWith(".zip")) {
                        InputStream is = new ByteArrayInputStream("this is zip".getBytes(ENCODING));
                        return new WebResourceResponse(null, null, is);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
        pathHandlers.add(new PathHandler("/test_path/") {
            @Override
            public WebResourceResponse handle(Uri url) {
                try {
                    if (url.getPath().endsWith(".txt")) {
                        InputStream is = new ByteArrayInputStream("this is txt".getBytes(ENCODING));
                        return new WebResourceResponse(null, null, is);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
        // Should always work regardless of the order handlers are registered.
        Collections.shuffle(pathHandlers);
        builder.register(pathHandlers.get(0));
        builder.register(pathHandlers.get(1));
        WebViewAssetLoader assetLoader = builder.build();

        {
            WebResourceResponse response = assetLoader.shouldInterceptRequest(
                    Uri.parse("https://appassets.androidplatform.net/test_path/file.zip"));
            Assert.assertNotNull("failed to match the URL and returned null response", response);
            Assert.assertNotNull("matched the URL but not the file and returned a null InputStream",
                    response.getData());
            Assert.assertEquals("this is zip", readAsString(response.getData(), ENCODING));
        }

        {
            WebResourceResponse response = assetLoader.shouldInterceptRequest(
                    Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
            Assert.assertNotNull("failed to match the URL and returned null response", response);
            Assert.assertNotNull("matched the URL but not the file and returned a null InputStream",
                    response.getData());
            Assert.assertEquals("this is txt", readAsString(response.getData(), ENCODING));
        }

        {
            WebResourceResponse response = assetLoader.shouldInterceptRequest(
                    Uri.parse("https://appassets.androidplatform.net/test_path/file.img"));
            Assert.assertNull("handled .img file, should return a null response", response);
        }
    }
}
