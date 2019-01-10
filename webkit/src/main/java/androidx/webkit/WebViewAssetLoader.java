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

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.internal.AssetHelper;
import androidx.webkit.internal.WebUriMatcher;

import java.io.InputStream;
import java.net.URLConnection;

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
 *     WebViewAssetLoader localServer = new WebViewAssetLoader(this);
 *     // For security WebViewAssetLoader uses a unique subdomain by default.
 *     AssetHostingDetails ahd = localServer.hostAssets("/www");
 *     webView.setWebViewClient(new WebViewClient() {
 *         @Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view,
 *                                          WebResourceRequest request) {
 *             return localServer.shouldInterceptRequest(request);
 *         }
 *     });
 *     // If your application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://{uuid}.androidplatform.net/assets/index.html
 *     webview.loadUrl(ahd.getHttpsPrefix().buildUpon().appendPath("index.html")
 *                              .build().toString());
 *
 * </pre>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WebViewAssetLoader {
    private static final String TAG = "WebViewAssetLoader";

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    @NonNull private final WebUriMatcher<PathHandler> mWebUriMatcher;
    @NonNull private final AssetHelper mAssetHelper;
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

        PathHandler() {
            this.mMimeType = null;
            this.mEncoding = null;
        }

        @Nullable
        public abstract InputStream handle(@NonNull Uri url);

        @Nullable
        public String getMimeType() {
            return mMimeType;
        }

        @Nullable
        public String getEncoding() {
            return mEncoding;
        }

        void setMimeType(@Nullable String mimeType) {
            mMimeType = mimeType;
        }

        void setEncoding(@Nullable String encoding) {
            mEncoding = encoding;
        }
    }

    /**
     * Information about the URLs used to host the assets in the WebView.
     */
    public static class AssetHostingDetails {
        @Nullable private Uri mHttpPrefix;
        @Nullable private Uri mHttpsPrefix;

        /*package*/ AssetHostingDetails(@Nullable Uri httpPrefix, @Nullable Uri httpsPrefix) {
            this.mHttpPrefix = httpPrefix;
            this.mHttpsPrefix = httpsPrefix;
        }

        /**
         * Gets the http: scheme prefix at which assets are hosted.
         * @return  the http: scheme prefix at which assets are hosted. Can return null.
         */
        @Nullable
        public Uri getHttpPrefix() {
            return mHttpPrefix;
        }

        /**
         * Gets the https: scheme prefix at which assets are hosted.
         * @return  the https: scheme prefix at which assets are hosted. Can return null.
         */
        @Nullable
        public Uri getHttpsPrefix() {
            return mHttpsPrefix;
        }
    }

    @VisibleForTesting
    /*package*/ WebViewAssetLoader(@NonNull AssetHelper assetHelper, @NonNull String authority) {
        mWebUriMatcher = new WebUriMatcher<>();
        this.mAssetHelper = assetHelper;
        this.mAuthority = authority;
    }

    /**
     * Creates a new instance of the WebView local server.
     *
     * @param context context used to resolve resources/assets
     */
    public WebViewAssetLoader(@NonNull Context context) {
        // We only need the context to resolve assets and resources so the ApplicationContext is
        // sufficient while holding on to an Activity context could cause leaks.
        this(new AssetHelper(context.getApplicationContext()),
                             context.getApplicationContext().getPackageName());
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
    @RequiresApi(21)
    @Nullable
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        PathHandler handler;
        synchronized (mWebUriMatcher) {
            handler = mWebUriMatcher.match(request.getUrl());
        }
        if (handler == null) {
            return null;
        }

        InputStream is = handler.handle(request.getUrl());
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
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
            synchronized (mWebUriMatcher) {
                handler = mWebUriMatcher.match(uri);
            }
        }
        if (handler == null) {
            return null;
        }

        InputStream is = handler.handle(uri);
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
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
        synchronized (mWebUriMatcher) {
            mWebUriMatcher.addUri(uri.getScheme(), uri.getAuthority(), uri.getPath(), handler);
        }
    }

    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{uuid}.androidplatform.net/assets/...</code>.
     *
     * @param assetPath the local path in the application's asset folder which will be made
     *                  available by the server. Should start with a leading slash (for example
     *                  "/www").
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
     *                  available by the server. Should start with a leading slash (for example
     *                  "/www").
     * @param virtualAssetPath the path on the local server under which the assets should be hosted.
     *                         Should start with a leading slash (for example "/assets/www").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull final String assetPath,
                                        @NonNull final String virtualAssetPath, boolean enableHttp,
                                        boolean enableHttps) {
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
     *                  available by the server. Should start with a leading slash (for example
     *                  "/www").
     * @param virtualAssetPath the path on the local server under which the assets should be hosted.
     *                         Should start with a leading slash (for example "/assets/www").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull final String domain,
                                          @NonNull final String assetPath,
                                          @NonNull final String virtualAssetPath,
                                          boolean enableHttp, boolean enableHttps) {
        if (assetPath.indexOf('*') != -1) {
            throw new IllegalArgumentException("assetPath cannot contain the '*' character.");
        }
        if (virtualAssetPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualAssetPath cannot contain the '*' character.");
        }

        PathHandler handler = new PathHandler() {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(virtualAssetPath, assetPath);
                Uri.Builder assetUriBuilder = new Uri.Builder();
                assetUriBuilder.path(path);
                Uri assetUri = assetUriBuilder.build();

                InputStream stream = mAssetHelper.openAsset(assetUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(assetUri.getPath()));

                return stream;
            }
        };

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(domain);
        uriBuilder.path(virtualAssetPath);

        Uri httpPrefix = null;
        Uri httpsPrefix = null;
        if (enableHttp) {
            uriBuilder.scheme(HTTP_SCHEME);
            httpPrefix = uriBuilder.build();
            register(httpPrefix, handler);
        }
        if (enableHttps) {
            uriBuilder.scheme(HTTPS_SCHEME);
            httpsPrefix = uriBuilder.build();
            register(httpsPrefix, handler);
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
     * <code>http(s)://{uuid}.androidplatform.net/{virtualResourcesPath}/{resource_type}/
     * {resource_name}</code>.
     *
     * @param virtualResourcesPath the path on the local server under which the resources should
     *                             be hosted. Should start with a leading slash (for example
     *                              "/resources").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources(@NonNull final String virtualResourcesPath,
                                             boolean enableHttp, boolean enableHttps) {
        return hostResources(mAuthority, virtualResourcesPath, enableHttp, enableHttps);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://{domain}/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     * @param virtualResourcesPath the path on the local server under which the resources
     *                             should be hosted. Should start with a leading slash (for example
     *                             "/resources").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @param enableHttps whether to enable hosting using the https scheme.
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources(@NonNull final String domain,
                                             @NonNull final String virtualResourcesPath,
                                             boolean enableHttp, boolean enableHttps) {
        if (virtualResourcesPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualResourcesPath cannot contain the '*' character.");
        }

        PathHandler handler = new PathHandler() {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(virtualResourcesPath, "");
                Uri.Builder resourceUriBuilder = new Uri.Builder();
                resourceUriBuilder.path(path);
                Uri resourceUri = resourceUriBuilder.build();

                InputStream stream  = mAssetHelper.openResource(resourceUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(resourceUri.getPath()));

                return stream;
            }
        };

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(domain);
        uriBuilder.path(virtualResourcesPath);

        Uri httpPrefix = null;
        Uri httpsPrefix = null;
        if (enableHttp) {
            uriBuilder.scheme(HTTP_SCHEME);
            httpPrefix = uriBuilder.build();
            register(httpPrefix, handler);
        }
        if (enableHttps) {
            uriBuilder.scheme(HTTPS_SCHEME);
            httpsPrefix = uriBuilder.build();
            register(httpsPrefix, handler);
        }
        return new AssetHostingDetails(httpPrefix, httpsPrefix);
    }
}
