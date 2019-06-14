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

import static androidx.webkit.WebViewAssetLoader.AssetsPathHandler;
import static androidx.webkit.WebViewAssetLoader.PathHandler;
import static androidx.webkit.WebViewAssetLoader.ResourcesPathHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
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

    private static class TestPathHandler implements PathHandler {
        @Override
        public WebResourceResponse handle(@NonNull String path) {
            try {
                InputStream stream = new ByteArrayInputStream(CONTENTS.getBytes(ENCODING));
                return new WebResourceResponse(null, null, stream);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .register("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNull("non-registered URL should return null response",
                assetLoader.shouldInterceptRequest(Uri.parse("https://foo.bar/")));
    }

    @Test
    @SmallTest
    public void testCustomDomain() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .onDomain("test.myDomain.net")
                                                .register("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://test.myDomain.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNull("non-registered URL should return null response",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("https://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testAllowingHttp() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .allowHttp(true)
                                                .register("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNotNull("didn't match HTTP URL despite allowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testDisallowingHttp() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .register("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNull("matched HTTP URL despite disallowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testHostAssets() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        PathHandler assetsPathHandler = new AssetsPathHandler(new MockAssetHelper() {
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
                                                      .register("/assets/", assetsPathHandler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/assets/www/test.html"));
        assertResponse(response, testHtmlContents);
    }

    @Test
    @SmallTest
    public void testHostResources() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        PathHandler resourcesPathHandler = new ResourcesPathHandler(new MockAssetHelper() {
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
                                                      .register("/res/", resourcesPathHandler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/res/raw/test.html"));
        assertResponse(response, testHtmlContents);
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlers() throws Throwable {
        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder();
        for (int i = 1; i <= 5; ++i) {
            final String testContent = CONTENTS + Integer.toString(i);
            builder.register("/test_path_" + Integer.toString(i) + "/", new PathHandler() {
                @Override
                public WebResourceResponse handle(@NonNull String path) {
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
            assertResponse(response, CONTENTS + Integer.toString(i));
        }
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlersOnTheSamePath() throws Throwable {
        List<PathHandler> pathHandlers = new ArrayList<>();
        // PathHandler for files ending with .zip
        pathHandlers.add(new PathHandler() {
            @Override
            public WebResourceResponse handle(@NonNull String path) {
                try {
                    if (path.endsWith(".png")) {
                        InputStream is = new ByteArrayInputStream("this is png".getBytes(ENCODING));
                        return new WebResourceResponse(null, null, is);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
        // PathHandler for files ending with .txt
        pathHandlers.add(new PathHandler() {
            @Override
            public WebResourceResponse handle(@NonNull String path) {
                try {
                    if (path.endsWith(".txt")) {
                        InputStream is = new ByteArrayInputStream("this is txt".getBytes(ENCODING));
                        return new WebResourceResponse(null, null, is);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder();
        builder.register("/test_path/", pathHandlers.get(0));
        builder.register("/test_path/", pathHandlers.get(1));
        WebViewAssetLoader assetLoader = builder.build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.png"));
        assertResponse(response, "this is png");

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
        assertResponse(response, "this is txt");

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.jpg"));
        Assert.assertNull("handled .jpg file, should return a null response", response);

        // Register in reverse order to make sure it works regardless of order.
        builder = new WebViewAssetLoader.Builder();
        builder.register("/test_path/", pathHandlers.get(1));
        builder.register("/test_path/", pathHandlers.get(0));
        assetLoader = builder.build();

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.png"));
        assertResponse(response, "this is png");

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
        assertResponse(response, "this is txt");
    }

    private static void assertResponse(@Nullable WebResourceResponse response,
              @NonNull String expectedContent) throws IOException {
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the URL but returned a null InputStream",
                response.getData());
        Assert.assertEquals(expectedContent, readAsString(response.getData(), ENCODING));
    }
}
