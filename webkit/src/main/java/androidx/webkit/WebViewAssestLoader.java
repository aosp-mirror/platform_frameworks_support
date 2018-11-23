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

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.internal.UriMatcher;
import androidx.webkit.internal.AndroidProtocolHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class meant to be used with the android.webkit.WebView class to enable hosting assets,
 * resources and other data on 'virtual' http(s):// URL.
 * Hosting assets and resources on http(s):// URLs is desirable as it is compatible with the
 * Same-Origin policy.
 *
 * This class is intended to be used from within the
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
 * android.webkit.WebResourceRequest)}
 * methods.
 * <pre>
 *     WebViewAssestLoader localServer = new WebViewAssestLoader(this);
 *     // For security WebViewAssestLoader uses a unique subdomain by default.
 *     AssetHostingDetails ahd = localServer.hostAssets("/www");
 *     webView.setWebViewClient(new WebViewClient() {
 *         @Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
 *             return localServer.shouldInterceptRequest(request);
 *         }
 *     });
 *     // If your application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://{uuid}.androidplatform.net/assets/index.html
 *     webview.loadUrl(ahd.getHttpsPrefix().buildUpon().appendPath("index.html").build().toString());
 *
 * </pre>
 *
 * @hide
 */
public class WebViewAssestLoader {
    private static String TAG = "WebViewAssestLoader";
    /**
     * Using http(s):// URL to access local resources may conflict with a real website. This means
     * that local resources should only be hosted on domains that the user has control of or which
     * have been dedicated for this purpose.
     *
     * The androidplatform.net domain currently belongs to Google and has been reserved for the
     * purpose of Android applications intercepting navigations/requests directed there. It'll be
     * used by default unless the user specified a different domain.
     */
    public final static String KNOWN_UNUSED_AUTHORITY = "androidplatform.net";

    private final static String sHttpScheme = "http";
    private final static String sHttpsScheme = "https";

    @NonNull private final UriMatcher mUriMatcher;
    @NonNull private final AndroidProtocolHandler mProtocolHandler;
    @NonNull private final String mAuthority;

    /**
     * A handler that produces responses for paths on the virtual asset server.
     *
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     *
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoked.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spent blocking in this method should be kept to an absolute
     * minimum.
     */
    /*package*/ abstract static class PathHandler {
        @Nullable private String mMimeType;
        @Nullable private String mEncoding;
        @Nullable private String mCharset;
        @Nullable private int mStatusCode;
        @Nullable private String mReasonPhrase;
        @Nullable private Map<String, String> mResponseHeaders;

        public PathHandler() {
            this(null, null, null, 200, "OK", null);
        }

        public PathHandler(@Nullable String mimeType, @Nullable String encoding, @Nullable String charset, int statusCode,
                        @Nullable String reasonPhrase, @Nullable Map<String, String> responseHeaders) {
            this.mMimeType = mimeType;
            this.mEncoding = encoding;
            this.mCharset = charset;
            this.mStatusCode = statusCode;
            this.mReasonPhrase = reasonPhrase;
            this.mResponseHeaders = responseHeaders;
        }

        @Nullable
        public InputStream handle(@NonNull WebResourceRequest request) {
            return handle(request.getUrl());
        }

        @Nullable
        abstract public InputStream handle(@NonNull Uri url);

        @Nullable
        public String getMimeType() {
            return mMimeType;
        }

        @Nullable
        public String getEncoding() {
            return mEncoding;
        }

        @Nullable
        public String getCharset() {
            return mCharset;
        }

        @Nullable
        public int getStatusCode() {
            return mStatusCode;
        }

        @Nullable
        public String getReasonPhrase() {
            return mReasonPhrase;
        }

        @Nullable
        public Map<String, String> getResponseHeaders() {
            return mResponseHeaders;
        }
    }

    /**
     * Information about the URLs used to host the assets in the WebView.
     */
    public static class AssetHostingDetails {
        @Nullable private Uri httpPrefix;
        @Nullable private Uri httpsPrefix;

        /*package*/ AssetHostingDetails(@Nullable Uri httpPrefix, @Nullable Uri httpsPrefix) {
            this.httpPrefix = httpPrefix;
            this.httpsPrefix = httpsPrefix;
        }

        /**
         * Gets the http: scheme prefix at which assets are hosted.
         * @return  the http: scheme prefix at which assets are hosted. Can return null.
         */
        @Nullable
        public Uri getHttpPrefix() {
            return httpPrefix;
        }

        /**
         * Gets the https: scheme prefix at which assets are hosted.
         * @return  the https: scheme prefix at which assets are hosted. Can return null.
         */
        @Nullable
        public Uri getHttpsPrefix() {
            return httpsPrefix;
        }
    }

    /*package*/ WebViewAssestLoader(@NonNull AndroidProtocolHandler mProtocolHandler) {
        mUriMatcher = new UriMatcher(null);
        this.mProtocolHandler = mProtocolHandler;
        mAuthority = UUID.randomUUID().toString() + "." + KNOWN_UNUSED_AUTHORITY;
    }

    /**
     * Creates a new instance of the WebView local server.
     *
     * @param context context used to resolve resources/assets/
     */
    public WebViewAssestLoader(@NonNull Context context) {
        // We only need the context to resolve assets and resources so the ApplicationContext is
        // sufficient while holding on to an Activity context could cause leaks.
        this(new AndroidProtocolHandler(context.getApplicationContext()));
    }

    @Nullable
    private static Uri parseAndVerifyUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        Uri uri = Uri.parse(url);
        if (uri == null) {
            Log.e(TAG, "Malformed URL: " + url);
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            Log.e(TAG, "URL does not have a path: " + url);
            return null;
        }
        return uri;
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>request</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
     * android.webkit.WebResourceRequest)}.
     *
     * @param request the request to process.
     * @return a response if the request URL had a matching handler, null if no handler was found.
     */
    @Nullable
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        PathHandler handler = null;
        synchronized (mUriMatcher) {
            handler = (PathHandler) mUriMatcher.match(request.getUrl());
        }
        if (handler == null)
            return null;

        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(),
                handler.getStatusCode(), handler.getReasonPhrase(), handler.getResponseHeaders(),
                new LollipopLazyInputStream(handler, request));
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>url</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url the url to process.
     * @return a response if the request URL had a matching handler, null if no handler was found.
     */
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@Nullable String url) {
        PathHandler handler = null;
        Uri uri = parseAndVerifyUrl(url);
        if (uri != null) {
            synchronized (mUriMatcher) {
                handler = (PathHandler) mUriMatcher.match(uri);
            }
        }
        if (handler == null)
            return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(),
                    new LegacyLazyInputStream(handler, uri));
        } else {
            InputStream is = handler.handle(uri);
            return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(),
                    is);
        }
    }

    /**
     * Registers a handler for the given <code>uri</code>. The <code>handler</code> will be invoked
     * every time the <code>shouldInterceptRequest</code> method of the instance is called with
     * a matching <code>uri</code>.
     *
     * @param uri the {@link android.net.Uri} to use the handler for. The scheme and authority
     *            (domain) will be matched exactly. The path may contain a '*' element which will
     *            match a single element of a path (so a handler registered for /a/* will be
     *            invoked for /a/b and /a/c.html but not for /a/b/b) or the '**' element which will
     *            match any number of path elements.
     * @param handler the handler to use for the uri.
     */
    void register(@NonNull Uri uri, @NonNull PathHandler handler) {
        synchronized (mUriMatcher) {
            mUriMatcher.addURI(uri.getScheme(), uri.getAuthority(), uri.getPath(), handler);
        }
    }

    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{uuid}.androidplatform.net/assets/...</code>.
     *
     * @param assetPath the local path in the application's asset folder which will be made
     *                  available by the server (for example "/www").
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull String assetPath) {
        return hostAssets(mAuthority, assetPath, "/assets", true, true);
    }


    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{uuid}.androidplatform.net/{virtualAssetPath}/...</code>.
     *
     * @param assetPath the local path in the application's asset folder which will be made
     *                  available by the server (for example "/www").
     * @param virtualAssetPath the path on the local server under which the assets should be hosted.
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull final String assetPath, @NonNull final String virtualAssetPath,
                                          boolean enableHttp, boolean enableHttps) {
        return hostAssets(mAuthority, assetPath, virtualAssetPath, enableHttp,
                enableHttps);
    }

    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     * @param assetPath the local path in the application's asset folder which will be made
     *                  available by the server (for example "/www").
     * @param virtualAssetPath the path on the local server under which the assets should be hosted.
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull final String domain,
                                          @NonNull final String assetPath, @NonNull final String virtualAssetPath,
                                          boolean enableHttp, boolean enableHttps) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(sHttpScheme);
        uriBuilder.authority(domain);
        uriBuilder.path(virtualAssetPath);

        if (assetPath.indexOf('*') != -1) {
            throw new IllegalArgumentException("assetPath cannot contain the '*' character.");
        }
        if (virtualAssetPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualAssetPath cannot contain the '*' character.");
        }

        Uri httpPrefix = null;
        Uri httpsPrefix = null;

        PathHandler handler = new PathHandler() {
            @Override
            public InputStream handle(Uri url) {
                InputStream stream;
                String path = url.getPath().replaceFirst(virtualAssetPath, assetPath);
                try {
                    stream = mProtocolHandler.openAsset(path);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to open asset URL: " + url);
                    return null;
                }

                String mimeType = null;
                try {
                    mimeType = URLConnection.guessContentTypeFromName(path);
                    if (mimeType == null)
                        mimeType = URLConnection.guessContentTypeFromStream(stream);
                } catch (Exception ex) {
                    Log.e(TAG, "Unable to get mime type" + url);
                }

                return stream;
            }
        };

        if (enableHttp) {
            httpPrefix = uriBuilder.build();
            register(Uri.withAppendedPath(httpPrefix, "**"), handler);
        }
        if (enableHttps) {
            uriBuilder.scheme(sHttpsScheme);
            httpsPrefix = uriBuilder.build();
            register(Uri.withAppendedPath(httpsPrefix, "**"), handler);
        }
        return new AssetHostingDetails(httpPrefix, httpsPrefix);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://{uuid}.androidplatform.net/res/{resource_type}/{resource_name}</code>.
     *
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources() {
        return hostResources(mAuthority, "/res", true, true);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://{uuid}.androidplatform.net/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param virtualResourcesPath the path on the local server under which the resources
     *                             should be hosted.
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources(@NonNull final String virtualResourcesPath, boolean enableHttp,
                                             boolean enableHttps) {
        return hostResources(mAuthority, virtualResourcesPath, enableHttp, enableHttps);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://{domain}/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     *               If untrusted content is to be loaded into the WebView it is advised to make
     *               this random.
     * @param virtualResourcesPath the path on the local server under which the resources
     *                             should be hosted.
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources(@NonNull final String domain,
                                             @NonNull final String virtualResourcesPath, boolean enableHttp,
                                             boolean enableHttps) {
        if (virtualResourcesPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualResourcesPath cannot contain the '*' character.");
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(sHttpScheme);
        uriBuilder.authority(domain);
        uriBuilder.path(virtualResourcesPath);

        Uri httpPrefix = null;
        Uri httpsPrefix = null;

        PathHandler handler = new PathHandler() {
            @Override
            public InputStream handle(Uri url) {
                InputStream stream  = mProtocolHandler.openResource(url);
                String mimeType = null;
                try {
                    mimeType = URLConnection.guessContentTypeFromStream(stream);
                } catch (Exception ex) {
                    Log.e(TAG, "Unable to get mime type" + url);
                }

                return stream;
            }
        };

        if (enableHttp) {
            httpPrefix = uriBuilder.build();
            register(Uri.withAppendedPath(httpPrefix, "**"), handler);
        }
        if (enableHttps) {
            uriBuilder.scheme(sHttpsScheme);
            httpsPrefix = uriBuilder.build();
            register(Uri.withAppendedPath(httpsPrefix, "**"), handler);
        }
        return new AssetHostingDetails(httpPrefix, httpsPrefix);
    }

    /**
     * The KitKat WebView reads the InputStream on a separate threadpool. We can use that to
     * parallelize loading.
     */
    private static abstract class LazyInputStream extends InputStream {
        protected final PathHandler handler;
        private InputStream is = null;

        public LazyInputStream(PathHandler handler) {
            this.handler = handler;
        }

        private InputStream getInputStream() {
            if (is == null) {
                is = handle();
            }
            return is;
        }

        protected abstract InputStream handle();

        @Override
        public int available() throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.available() : 0;
        }

        @Override
        public int read() throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.read() : -1;
        }

        @Override
        public int read(byte b[]) throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.read(b) : -1;
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.read(b, off, len) : -1;
        }

        @Override
        public long skip(long n) throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.skip(n) : 0;
        }
    }

    // For earlier than L.
    private static class LegacyLazyInputStream extends LazyInputStream {
        private Uri uri;
        private InputStream is;

        public LegacyLazyInputStream(PathHandler handler, Uri uri) {
            super(handler);
            this.uri = uri;
        }

        @Override
        protected InputStream handle() {
            return handler.handle(uri);
        }
    }

    // For L and above.
    private static class LollipopLazyInputStream extends LazyInputStream {
        private WebResourceRequest request;
        private InputStream is;

        public LollipopLazyInputStream(PathHandler handler, WebResourceRequest request) {
            super(handler);
            this.request = request;
        }

        @Override
        protected InputStream handle() {
            return handler.handle(request);
        }
    }
}
