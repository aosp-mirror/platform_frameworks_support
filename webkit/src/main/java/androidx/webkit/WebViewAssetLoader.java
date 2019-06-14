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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.webkit.internal.AssetHelper;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to enable accessing the application's static assets and resources and other files
 * accessable by the application under an http(s):// URL to be loaded by
 * {@link android.webkit.WebView} class.
 * Hosting assets and resources this way is desirable as it is compatible with the Same-Origin
 * policy.
 *
 * <p>
 * For more context about application's assets and resources and how to normally access them please
 * refer to <a href="https://developer.android.com/guide/topics/resources/providing-resources">
 * Android Developer Docs: App resources overview</a>.
 *
 * <p class='note'>
 * This class is expected to be used within
 * {@link android.webkit.WebViewClient#shouldInterceptRequest}, which is invoked on a different
 * thread than application's main thread. Although instances are themselves thread-safe (and may be
 * safely constructed on the application's main thread), exercise caution when accessing private
 * data or the view system.
 * <p>
 * Using http(s):// URLs to access local resources may conflict with a real website. This means
 * that local resources should only be hosted on domains your organization owns (at paths reserved
 * for this purpose) or the default domain reserved for this: {@code appassets.androidplatform.net}.
 * <p>
 * A typical usage would be like:
 * <pre class="prettyprint">
 *     final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
 *              .register(new AssetsPathHandler("/assets/", this))
 *              .register(new ResourcesPathHandler("/res/", this))
 *              .build();
 *
 *     webView.setWebViewClient(new WebViewClient() {
 *         {@literal @}Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view,
 *                                          WebResourceRequest request) {
 *             return assetLoader.shouldInterceptRequest(request.getUrl());
 *         }
 *     });
 *     // Assets are hosted under http(s)://appassets.androidplatform.net/assets/... .
 *     // If the application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://appassets.androidplatform.net/assets/www/index.html
 *     webview.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
 *
 * </pre>
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public final class WebViewAssetLoader {
    private static final String TAG = "WebViewAssetLoader";

    /**
     * An unused domain reserved for Android applications to intercept requests for app assets.
     * <p>
     * It'll be used by default unless the user specified a different domain.
     */
    public static final String KNOWN_UNUSED_AUTHORITY = "appassets.androidplatform.net";

    private final List<PathMatcher> mMatchers;

    /**
     * A handler that produces responses for a registered path.
     *
     * <p>
     * This class can be extended to handle other cases according to application needs.
     * <p>
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     * <p>
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoked.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spent blocking in this method should be kept to an absolute
     * minimum.
     */
    public abstract static class PathHandler {
        private final @NonNull String mRegisteredPath;

        /**
         * Constructor for the PathHandler.
         *
         * The path must start and end with {@code "/"}.
         * <p>
         * A custom prefix path can be used in conjunction with a custom domain, to
         * avoid conflicts with real paths which may be hosted at that domain.
         *
         * @param registeredPath the registered path prefix that this class handles reguestes to.
         */
        public PathHandler(@NonNull String registeredPath) {
            mRegisteredPath = registeredPath;
        }

        /**
         * Handles the requested URL by opening the appropriate file.
         *
         * @param url the URL to be handled.
         * @return {@link InputStream} for the requested URL.
         */
        @Nullable
        public abstract InputStream handle(@NonNull Uri url);

        /**
         * @return the registeredPath where this handler would be called.
         */
        @NonNull
        public String getRegisteredPath() {
            return mRegisteredPath;
        }
    }

    /**
     * Handler class to open a file from application assets directory.
     */
    public static final class AssetsPathHandler extends PathHandler {
        private AssetHelper mAssetHelper;

        /**
         * @param registeredPath the registered path prefix where apps assets are hosted.
         * @param context {@link Context} used to resolve assets.
         */
        public AssetsPathHandler(@NonNull String registeredPath, @NonNull Context context) {
            super(registeredPath);
            mAssetHelper = new AssetHelper(context);
        }

        @VisibleForTesting
        /*package*/ AssetsPathHandler(@NonNull String registeredPath,
                @NonNull AssetHelper assetHelper) {
            super(registeredPath);
            mAssetHelper = assetHelper;
        }

        /**
         * Opens the requested file from application's assets directory.
         *
         * @param url the URL to be handled.
         * @return {@link InputStream} for the requested file or {@code null} if file is not found.
         */
        @Override
        @Nullable
        public InputStream handle(Uri url) {
            String path = url.getPath().replaceFirst(getRegisteredPath(), "");
            Uri uri = new Uri.Builder()
                    .path(path)
                    .build();
            return mAssetHelper.openAsset(uri);
        }
    }

    /**
     * Handler class to open a file from application resources directory.
     */
    public static final class ResourcesPathHandler extends PathHandler {
        private AssetHelper mAssetHelper;

        /**
         * @param registeredPath the registered path prefix where apps resources are hosted.
         * @param context {@link Context} used to resolve resources.
         */
        public ResourcesPathHandler(@NonNull String registeredPath, @NonNull Context context) {
            super(registeredPath);
            mAssetHelper = new AssetHelper(context);
        }

        @VisibleForTesting
        /*package*/ ResourcesPathHandler(@NonNull String registeredPath,
                @NonNull AssetHelper assetHelper) {
            super(registeredPath);
            mAssetHelper = assetHelper;
        }

        /**
         * Opens the requested file from application's resources directory.
         *
         * @param url the URL to be handled.
         * @return {@link InputStream} for the requested file or {@code null} if file is not found.
         */
        @Override
        public InputStream handle(Uri url) {
            String path = url.getPath().replaceFirst(getRegisteredPath(), "");
            Uri uri = new Uri.Builder()
                    .path(path)
                    .build();
            return mAssetHelper.openResource(uri);
        }

    }

    /**
     * Matches URIs on the form: {@code "http(s)://authority/path/**"}, HTTPS is always enabled.
     *
     * <p>
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     * <p>
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoked.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spent blocking in this method should be kept to an absolute
     * minimum.
     */
    @VisibleForTesting
    /*package*/ static class PathMatcher {
        static final String HTTP_SCHEME = "http";
        static final String HTTPS_SCHEME = "https";

        final boolean mHttpEnabled;
        @NonNull final String mAuthority;
        @NonNull final String mPath;
        @NonNull final PathHandler mHandler;

        /**
         * @param authority the authority to match (For instance {@code "example.com"})
         * @param path the prefix path to match, it should start and end with a {@code "/"}.
         * @param httpEnabled enable hosting under the HTTP scheme, HTTPS is always enabled.
         * @param handler the {@link PathHandler} the handler class for this URI.
         */
        public PathMatcher(@NonNull final String authority, @NonNull final String path,
                            boolean httpEnabled, @NonNull final PathHandler handler) {
            if (path.isEmpty() || path.charAt(0) != '/') {
                throw new IllegalArgumentException("Path should start with a slash '/'.");
            }
            if (!path.endsWith("/")) {
                throw new IllegalArgumentException("Path should end with a slash '/'");
            }
            mAuthority = authority;
            mPath = path;
            mHttpEnabled = httpEnabled;
            mHandler = handler;
        }

        /**
         * Match against registered scheme, authority and path prefix.
         *
         * Match happens when:
         * <ul>
         *      <li>Scheme is "https" <b>or</b> the scheme is "http" and http is enabled.</li>
         *      <li>Authority exact matches the given URI's authority.</li>
         *      <li>Path is a prefix of the given URI's path.</li>
         * </ul>
         *
         * @param uri the URI whose path we will match against.
         *
         * @return {@code true} if a match happens, {@code false} otherwise.
         */
        @Nullable
        public PathHandler match(@NonNull Uri uri) {
            // Only match HTTP_SCHEME if caller enabled HTTP matches.
            if (uri.getScheme().equals(HTTP_SCHEME) && !mHttpEnabled) {
                return null;
            }
            // Don't match non-HTTP(S) schemes.
            if (!uri.getScheme().equals(HTTP_SCHEME) && !uri.getScheme().equals(HTTPS_SCHEME)) {
                return null;
            }
            if (!uri.getAuthority().equals(mAuthority)) {
                return null;
            }
            if (!uri.getPath().startsWith(mPath)) {
                return null;
            }
            return mHandler;
        }
    }

    /**
     * A builder class for constructing {@link WebViewAssetLoader} objects.
     */
    public static final class Builder {
        private boolean mAllowHttp;
        private String mDomain;
        @NonNull private List<PathMatcher> mBuilderMatcherList;

        /**
         * @param context {@link Context} used to resolve resources/assets.
         */
        public Builder() {
            mAllowHttp = false;
            mDomain = KNOWN_UNUSED_AUTHORITY;
            mBuilderMatcherList = new ArrayList<>();
        }

        /**
         * Set the domain under which app assets can be accessed.
         * The default domain is {@code "appassets.androidplatform.net"}
         *
         * @param domain the domain on which app assets should be hosted.
         * @return {@link Builder} object.
         */
        @NonNull
        public Builder onDomain(@NonNull String domain) {
            mDomain = domain;
            return this;
        }

        /**
         * Allow using the HTTP scheme in addition to HTTPS.
         * The default is to not allow HTTP.
         *
         * @return {@link Builder} object.
         */
        @NonNull
        public Builder allowHttp(boolean allowHttp) {
            mAllowHttp = allowHttp;
            return this;
        }

        /**
         * Register a {@link PathHandler} for a specific path.
         *
         * @return {@link Builder} object.
         * @throws IllegalArgumentException if the path is invalid or matches an already registered
         *                                  path.
         */
        @NonNull
        public Builder register(@NonNull PathHandler handler) {
            // check that the new registered path doesn't collide with an already registered path.
            for (PathMatcher matcher : mBuilderMatcherList) {
                if (handler.getRegisteredPath().startsWith(matcher.mPath)) {
                    throw new IllegalArgumentException("The path " + handler.getRegisteredPath()
                            + " matches an already registered path: " + matcher.mPath);
                }
            }
            mBuilderMatcherList.add(new PathMatcher(mDomain, handler.getRegisteredPath(),
                      mAllowHttp, handler));
            return this;
        }

        /**
         * Build and return a {@link WebViewAssetLoader} object.
         *
         * @return immutable {@link WebViewAssetLoader} object.
         */
        @NonNull
        public WebViewAssetLoader build() {
            return new WebViewAssetLoader(mBuilderMatcherList);
        }
    }

    /*package*/ WebViewAssetLoader(@NonNull List<PathMatcher> pathMatchers) {
        mMatchers = pathMatchers;
    }

    /**
     * Attempt to resolve the {@code url} to an application resource or asset, and return
     * a {@link WebResourceResponse} for the content.
     * <p>
     * The prefix path used shouldn't be a prefix of a real web path. Thus, in case of having a URL
     * that matches a registered prefix path but the requested asset cannot be found or opened a
     * {@link WebResourceResponse} object with a {@code null} {@link InputStream} will be returned
     * instead of {@code null}. This saves the time of falling back to network and trying to
     * resolve a path that doesn't exist. A {@link WebResourceResponse} with {@code null}
     * {@link InputStream} will be received as an HTTP response with status code {@code 404} and
     * no body.
     * <p>
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url the URL to process.
     * @return {@link WebResourceResponse} if the request URL matches a registered URL,
     *         {@code null} otherwise.
     */
    @WorkerThread
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@NonNull Uri url) {
        PathHandler handler = null;
        for (PathMatcher matcher : mMatchers) {
            handler = matcher.match(url);
            // found a match.
            if (handler != null) break;
        }

        // no match found, a null reposne is returned so WebView falls back to Network.
        if (handler == null) {
            return null;
        }

        InputStream is = handler.handle(url);
        String mimeType = URLConnection.guessContentTypeFromName(url.getPath());

        return new WebResourceResponse(mimeType, null, is);
    }
}
