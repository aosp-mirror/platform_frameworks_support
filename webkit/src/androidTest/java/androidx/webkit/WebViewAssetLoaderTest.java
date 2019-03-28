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

import android.net.Uri;
import android.webkit.WebResourceResponse;

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

    @Test
    @SmallTest
    public void testCustomPathHandler() throws Throwable {
        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper());
        final String contents = "Some content for testing\n";
        final String encoding = "utf-8";

        assetLoader.mResourcesHandler =
                new WebViewAssetLoader.PathHandler("appassets.androidplatform.net", "/test/",
                        true) {
            @Override
            public String getEncoding() {
                return encoding;
            }

            @Override
            public InputStream handle(Uri url) {
                try {
                    return new ByteArrayInputStream(contents.getBytes(encoding));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://appassets.androidplatform.net/test/");
        Assert.assertNotNull("didn't match the exact registered URL", response);

        Assert.assertEquals(encoding, response.getEncoding());
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));
<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)

        Assert.assertNull(assetLoader.shouldInterceptRequest("http://foo.bar/"));
=======
        Assert.assertNull("opened a non-registered URL - should return null",
                            assetLoader.shouldInterceptRequest("http://foo.bar/"));
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)
    }

    @Test
    @SmallTest
    public void testHostAssets() throws Throwable {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper() {
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

<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
        assetLoader.hostAssets("/assets/", true);
        Assert.assertEquals(assetLoader.getAssetsHttpPrefix(),
                                    Uri.parse("http://appassets.androidplatform.net/assets/"));
=======
        Assert.assertNull("HTTP is not allowed - getAssetsHttpPrefix should return null",
                                assetLoader.getAssetsHttpPrefix());
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)
        Assert.assertEquals(assetLoader.getAssetsHttpsPrefix(),
                                    Uri.parse("https://appassets.androidplatform.net/assets/"));

        WebResourceResponse response =
<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
                assetLoader.shouldInterceptRequest("http://appassets.androidplatform.net/assets/www/test.html");
        Assert.assertNotNull(response);
=======
                assetLoader.shouldInterceptRequest("https://appassets.androidplatform.net/assets/www/test.html");
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the URL but not the file and returned a null InputStream",
                                    response.getData());
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostResources() throws Throwable {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper() {
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

<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
        assetLoader.hostResources("/res/", true);
        Assert.assertEquals(assetLoader.getResourcesHttpPrefix(), Uri.parse("http://appassets.androidplatform.net/res/"));
        Assert.assertEquals(assetLoader.getResourcesHttpsPrefix(), Uri.parse("https://appassets.androidplatform.net/res/"));
=======
        Assert.assertNull("HTTP is not allowed - getResourcesHttpPrefix should return null",
                                assetLoader.getResourcesHttpPrefix());
        Assert.assertEquals(assetLoader.getResourcesHttpsPrefix(),
                                    Uri.parse("https://appassets.androidplatform.net/res/"));
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)

        WebResourceResponse response =
<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
                 assetLoader.shouldInterceptRequest("http://appassets.androidplatform.net/res/raw/test.html");
        Assert.assertNotNull(response);
=======
                 assetLoader.shouldInterceptRequest("https://appassets.androidplatform.net/res/raw/test.html");
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the prefix URL but not the file",
                                    response.getData());
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostAssetsOnCustomUri() throws Throwable {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder(new MockContext());
        builder.setDomain("example.com")
                        .setAssetsHostingPath("/android_assets/")
                        .allowHttp();
        WebViewAssetLoader assetLoader = builder.buildForTest(new MockAssetHelper() {
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

        Assert.assertEquals(assetLoader.getAssetsHttpPrefix(),
                                    Uri.parse("http://example.com/android_assets/"));
        Assert.assertEquals(assetLoader.getAssetsHttpsPrefix(),
                                    Uri.parse("https://example.com/android_assets/"));

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://example.com/android_assets/www/test.html");
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the prefix URL but not the file",
                                    response.getData());
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostResourcesOnCustomUri() throws Throwable {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder(new MockContext());
        builder.setDomain("example.com")
                        .setResourcesHostingPath("/android_res/")
                        .allowHttp();
        WebViewAssetLoader assetLoader = builder.buildForTest(new MockAssetHelper() {
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

        Assert.assertEquals(assetLoader.getResourcesHttpPrefix(),
                                    Uri.parse("http://example.com/android_res/"));
        Assert.assertEquals(assetLoader.getResourcesHttpsPrefix(),
                                    Uri.parse("https://example.com/android_res/"));

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://example.com/android_res/raw/test.html");
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the prefix URL but not the file",
                                    response.getData());
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }
}
