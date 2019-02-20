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
import androidx.annotation.VisibleForTesting;
import androidx.webkit.internal.AssetHelper;

import java.io.InputStream;
import java.net.URLConnection;

/**
 * Helper class to enable hosting assets and resources under a 'virtual' http(s):// URL to be used
 * in {@link android.webkit.WebView} class.
 *
 * Hosting assets and resources on http(s):// URLs is desirable as it is compatible with the
 * Same-Origin policy.
 *
 * <p>
 * This class is intended to be used from within the
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, android.webkit.WebResourceRequest)}
 * method.
 *
 * <p>
 * Using http(s):// URL to access local resources may conflict with a real website. This means
 * that local resources should only be hosted on domains that the user has control of or which
 * have been dedicated for this purpose.
 *
 * <p>
 * The <code>androidplatform.net</code> domain currently belongs to Google and has been reserved
 * for the purpose of Android applications intercepting navigations/requests directed there.
 * It'll be used by default unless the user specified a different domain. A subdomain
 * <code>appassets</code> is used to even make sure no such collisons would happen.
 *
 * <p>
 * A typical usage would be like:
 * <pre>
 *     WebViewAssetLoader assetLoader = new WebViewAssetLoader(this);
 *     webView.setWebViewClient(new WebViewClient() {
 *         @Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view,
 *                                          WebResourceRequest request) {
 *             return assetLoader.shouldInterceptRequest(request);
 *         }
 *     });
 *     // This will host assets under http(s)://appassets.androidplatform.net/assets/...
 *     assetLoader.hostAssets();
 *     // If the application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://appassets.androidplatform.net/assets/www/index.html
 *     webview.loadUrl(assetLoader.getAssetsHttpsPrefix().buildUpon()
 *                                      .appendPath("www")
 *                                      .appendPath("index.html")
 *                                      .build().toString());
 *
 * </pre>
 */
public class WebViewAssetLoader {
    private static final String TAG = "WebViewAssetLoader";

    /**
     * Reserved domain by Google for Android applications to intercept requests for app assets.
     *
     * It'll be used by default unless the user specified a different domain.
     */
    public static final String KNOWN_UNUSED_AUTHORITY = "appassets.androidplatform.net";

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    /*package*/ @NonNull final AssetHelper mAssetHelper;
    /*package*/ @Nullable @VisibleForTesting PathHandler mAssetsHandler;
    /*package*/ @Nullable @VisibleForTesting PathHandler mResourcesHandler;

    /**
     * A handler that produces responses for the registered paths.
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
    @VisibleForTesting
    /*package*/ abstract static class PathHandler {
        @Nullable private String mMimeType;
        @Nullable private String mEncoding;

        final boolean mHttpEnabled;
        @NonNull final String mAuthority;
        @NonNull final String mPath;

        /**
         * Constractor to create Handler for a new path.
         *
         * Matches URIs on the form: "http(s)://authority/path/**", HTTPS is always enabled.
         *
         * @param authority The authority to match (For example example.com)
         * @param path The prefix path to match. Should start and end with a slash "/".
         * @param httpEnabled Enable hosting under the HTTP scheme, HTTPS is always enabled.
         */
        PathHandler(@NonNull final String authority, @NonNull final String path,
                            boolean httpEnabled) {
            if (path.isEmpty() || path.charAt(0) != '/') {
                throw new IllegalArgumentException("Path should start with a slash '/'.");
            }
            if (!path.endsWith("/")) {
                throw new IllegalArgumentException("Path should end with a slash '/'");
            }
            this.mMimeType = null;
            this.mEncoding = null;
            this.mAuthority = authority;
            this.mPath = path;
            this.mHttpEnabled = httpEnabled;
        }

        /**
         * Should be invoked when a match happens.
         *
         * @param url Path that has been matched.
         */
        @Nullable
        public abstract InputStream handle(@NonNull Uri url);

        /**
         * Match against registered scheme, authority and path prefix.
         *
         * Match happens when:
         *      <li> Scheme is "https". </li>
         *      <li> OR the scheme is "http" and http is enabled.</li>
         *      <li> AND authority exact matches the given URI's authority.</li>
         *      <li> AND path is a prefix of the given URI's path.</li>
         * @param uri The URI whose path we will match against.
         *
         * @return True if a match happens, false otherwise.
         */
        public boolean match(@NonNull Uri uri) {
            // Only match HTTP_SCHEME if caller enabled HTTP matches.
            if (uri.getScheme().equals(HTTP_SCHEME) && !mHttpEnabled) {
                return false;
            }
            // Don't match non-HTTP(S) schemes.
            if (!uri.getScheme().equals(HTTP_SCHEME) && !uri.getScheme().equals(HTTPS_SCHEME)) {
                return false;
            }
            if (!uri.getAuthority().equals(mAuthority)) {
                return false;
            }
            return uri.getPath().startsWith(mPath);
        }

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

    @VisibleForTesting
    /*package*/ WebViewAssetLoader(@NonNull AssetHelper assetHelper) {
        this.mAssetHelper = assetHelper;
    }

    /**
     * @param context Context used to resolve resources/assets.
     */
    public WebViewAssetLoader(@NonNull Context context) {
        this(new AssetHelper(context.getApplicationContext()));
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
     * Resolve the coming <code>request</code> and attempt to load the requested resource or asset.
     *
     * If the URL matches a registered path but the requested resource or asset is not found, a
     * <code>WebResourceResponse</code> with a null stream will be returned. If the URL doesn't
     * match any registered URL, it returns null and fails back to network.
     * <p>
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, android.webkit.WebResourceRequest)}.
     *
     * @param request The request to process.
     * @return A response if the request URL had a matching registered url, null otherwise.
     */
    @RequiresApi(21)
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@NonNull WebResourceRequest request) {
        PathHandler handler;

        if (mAssetsHandler != null && mAssetsHandler.match(request.getUrl())) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler != null && mResourcesHandler.match(request.getUrl())) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(request.getUrl());
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
    }

    /**
     * Resolve the coming <code>url</code> and attempt to load the requested resource or asset.
     * If the URL matches a registered path but the requested resource or asset is not found, a
     * <code>WebResourceResponse</code> with a null stream will be returned. If the URL doesn't
     * match any registered URL, it returns null and fails back to network.
     * <p>
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url The url string to process.
     * @return A response if the request URL had a matching registered url, null otherwise.
     */
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@Nullable String url) {
        PathHandler handler = null;
        Uri uri = parseAndVerifyUrl(url);
        if (uri == null) {
            return null;
        }

        if (mAssetsHandler != null && mAssetsHandler.match(uri)) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler != null && mResourcesHandler.match(uri)) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(uri);
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
    }

    /**
     * Host application's assets under
     * <code>http(s)://appassets.androidplatform.net/assets/...</code>.
     */
    @NonNull
    public void hostAssets() {
        hostAssets(KNOWN_UNUSED_AUTHORITY, "/assets/", true);
    }

    /**
     * Host application's assets under
     * <code>http(s)://appassets.androidplatform.net/{virtualAssetPath}/...</code>.
     *
     * @param virtualAssetPath The virtual path under which the assets should be hosted. It Should
     *                         have a leading and trailing slash (for example "/assets/www/").
     * @param enableHttp Enable hosting under the HTTP scheme. HTTPS is always enabled.
     */
    @NonNull
    public void hostAssets(@NonNull final String virtualAssetPath, boolean enableHttp) {
        hostAssets(KNOWN_UNUSED_AUTHORITY, virtualAssetPath, enableHttp);
    }

    /**
     * Host application's assets under <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     *
     * @param domain Custom domain under which resources should be hosted
     *               (for instance <code>"example.com"</code>). The developer should always be in
     *               control of this domain or the default domain
     *               <code>appassets.androidplatform.net</code> should be used instead.
     * @param virtualAssetPath Virtual path under which the assets should be hosted. Should have
     *                         a leading and trailing slash (for example "/assets/www/").
     * @param enableHttp Enable hosting under the HTTP scheme. HTTPS is always enabled.
     */
    @NonNull
    public void hostAssets(@NonNull final String domain, @NonNull final String virtualAssetPath,
                                    boolean enableHttp) {
        final Uri uriPrefix = createUriPrefix(domain, virtualAssetPath);

        mAssetsHandler = new PathHandler(uriPrefix.getAuthority(), uriPrefix.getPath(),
                                            enableHttp) {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(this.mPath, "");
                Uri.Builder assetUriBuilder = new Uri.Builder();
                assetUriBuilder.path(path);
                Uri assetUri = assetUriBuilder.build();

                InputStream stream = mAssetHelper.openAsset(assetUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(assetUri.getPath()));

                return stream;
            }
        };
    }

    /**
     * Host application's resources under
     * <code>http(s)://appassets.androidplatform.net/res/{resource_type}/{resource_name}</code>.
     */
    @NonNull
    public void hostResources() {
        hostResources(KNOWN_UNUSED_AUTHORITY, "/res/", true);
    }

    /**
     * Host application's resources under
     * <code>http(s)://appassets.androidplatform.net/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param virtualResourcesPath Virtual path under which resources should be hosted. Should
     *                             have a leading and trailing slash (for example "/resources/").
     * @param enableHttp Enable hosting under the HTTP scheme. HTTPS is always enabled.
     */
    @NonNull
    public void hostResources(@NonNull final String virtualResourcesPath, boolean enableHttp) {
        hostResources(KNOWN_UNUSED_AUTHORITY, virtualResourcesPath, enableHttp);
    }

    /**
     * Host application's resources under
     * <code>http(s)://{domain}/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param domain Custom domain under which resources should be hosted
     *               (for instance <code>"example.com"</code>). The developer should always be in
     *               control of this domain or the default domain
     *               <code>appassets.androidplatform.net</code> should be used instead.
     * @param virtualResourcesPath Virtual path under which resources should be hosted. Should
     *                             have a leading and trailing slash (for example "/resources/").
     * @param enableHttp Enable hosting under the HTTP scheme. HTTPS is always enabled.
     */
    @NonNull
    public void hostResources(@NonNull final String domain,
                                    @NonNull final String virtualResourcesPath,
                                    boolean enableHttp) {
        final Uri uriPrefix = createUriPrefix(domain, virtualResourcesPath);

        mResourcesHandler = new PathHandler(uriPrefix.getAuthority(), uriPrefix.getPath(),
                                            enableHttp) {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(uriPrefix.getPath(), "");
                Uri.Builder resourceUriBuilder = new Uri.Builder();
                resourceUriBuilder.path(path);
                Uri resourceUri = resourceUriBuilder.build();

                InputStream stream  = mAssetHelper.openResource(resourceUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(resourceUri.getPath()));

                return stream;
            }
        };
    }

    @NonNull
    private static Uri createUriPrefix(@NonNull String domain, @NonNull String virtualPath) {
        if (virtualPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualPath cannot contain the '*' character.");
        }
        if (virtualPath.isEmpty() || virtualPath.charAt(0) != '/') {
            throw new IllegalArgumentException(
                    "virtualPath should start with a slash '/'.");
        }
        if (!virtualPath.endsWith("/")) {
            throw new IllegalArgumentException(
                    "virtualPath should end with a slash '/'.");
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(domain);
        uriBuilder.path(virtualPath);

        return uriBuilder.build();
    }

    /**
     * Get HTTP URL under which assets are hosted.
     *
     * @return HTTP URL under which assets are hosted, or null if no URL has been registered
     *         or HTTP was not enabled.
     */
    @Nullable
    public Uri getAssetsHttpPrefix() {
        if (mAssetsHandler == null || !mAssetsHandler.mHttpEnabled) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mAssetsHandler.mAuthority);
        uriBuilder.path(mAssetsHandler.mPath);
        uriBuilder.scheme(HTTP_SCHEME);

        return uriBuilder.build();
    }

    /**
     * Get HTTPS URL under which assets are hosted.
     *
     * @return HTTPS URL under which assets are hosted, or null if no URL has been registered.
     */
    @Nullable
    public Uri getAssetsHttpsPrefix() {
        if (mAssetsHandler == null) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mAssetsHandler.mAuthority);
        uriBuilder.path(mAssetsHandler.mPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        return uriBuilder.build();
    }

    /**
     * Get HTTP URL under which resources are hosted.
     *
     * @return HTTP URL under which resources are hosted, or null if no URL has been registered
     *         or HTTP was not enabled.
     */
    @Nullable
    public Uri getResourcesHttpPrefix() {
        if (mResourcesHandler == null || !mResourcesHandler.mHttpEnabled) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mResourcesHandler.mAuthority);
        uriBuilder.path(mResourcesHandler.mPath);
        uriBuilder.scheme(HTTP_SCHEME);

        return uriBuilder.build();
    }

    /**
     * Get HTTPS URL under which resources are hosted.
     *
     * @return HTTPS URL under which resources are hosted, or null if no URL has been registered.
     */
    @Nullable
    public Uri getResourcesHttpsPrefix() {
        if (mResourcesHandler == null) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mResourcesHandler.mAuthority);
        uriBuilder.path(mResourcesHandler.mPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        return uriBuilder.build();
    }
}
