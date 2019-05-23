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
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.webkit.internal.AssetHelper;

import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to enable accessing files accessible by the application including application's
 * static assets and resources under an http(s):// URL to be loaded by
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
 * that local files should only be hosted on domains your organization owns (at paths reserved
 * for this purpose) or the default domain reserved for this: {@code appassets.androidplatform.net}.
 * <p>
 * A typical usage would be like:
 * <pre class="prettyprint">
 *     final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
 *              .register("/assets/", new AssetsPathHandler(this))
 *              .register("/res/", new ResourcesPathHandler(this))
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
     * Implement this interface to handle other use-cases according to your app's needs.
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
    public interface PathHandler {
        /**
         * Handles the requested URL by returning the appropriate response.
         *
         * @param path the suffix path to be handled.
         * @return {@link WebResourceResponse} for the requested path or {@code null} if it can't
         *                                     handle this path.
         */
        @WorkerThread
        @Nullable
        WebResourceResponse handle(@NonNull String path);
    }

    /**
     * Handler class to open a file from application assets directory.
     */
    public static final class AssetsPathHandler implements PathHandler {
        private AssetHelper mAssetHelper;

        /**
         * @param registeredPath the registered path prefix where apps assets are hosted.
         * @param context {@link Context} used to resolve assets.
         */
        public AssetsPathHandler(@NonNull Context context) {
            mAssetHelper = new AssetHelper(context);
        }

        @VisibleForTesting
        /*package*/ AssetsPathHandler(@NonNull AssetHelper assetHelper) {
            mAssetHelper = assetHelper;
        }

        /**
         * Opens the requested file from the application's assets directory.
         * <p>
         * The matched prefix path used shouldn't be a prefix of a real web path. Thus, if the
         * requested file cannot be found a {@link WebResourceResponse} object with a {@code null}
         * {@link InputStream} will be returned instead of {@code null}. This saves the time of
         * falling back to network and trying to resolve a path that doesn't exist. A
         * {@link WebResourceResponse} with {@code null} {@link InputStream} will be received as an
         * HTTP response with status code {@code 404} and no body.
         *
         * @param path the suffix path to be handled.
         * @return {@link WebResourceResponse} for the requested file.
         */
        @Override
        @WorkerThread
        @Nullable
        public WebResourceResponse handle(@NonNull String path) {
            Uri uri = new Uri.Builder()
                    .path(path)
                    .build();

            InputStream is = mAssetHelper.openAsset(uri);
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return new WebResourceResponse(mimeType, null, is);
        }
    }

    /**
     * Handler class to open a file from application resources directory.
     */
    public static final class ResourcesPathHandler implements PathHandler {
        private AssetHelper mAssetHelper;

        /**
         * @param registeredPath the registered path prefix where apps resources are hosted.
         * @param context {@link Context} used to resolve resources.
         */
        public ResourcesPathHandler(@NonNull Context context) {
            mAssetHelper = new AssetHelper(context);
        }

        @VisibleForTesting
        /*package*/ ResourcesPathHandler(@NonNull AssetHelper assetHelper) {
            mAssetHelper = assetHelper;
        }

        /**
         * Opens the requested file from application's resources directory.
         * <p>
         * The matched prefix path used shouldn't be a prefix of a real web path. Thus, if the
         * requested file cannot be found a {@link WebResourceResponse} object with a {@code null}
         * {@link InputStream} will be returned instead of {@code null}. This saves the time of
         * falling back to network and trying to resolve a path that doesn't exist. A
         * {@link WebResourceResponse} with {@code null} {@link InputStream} will be received as an
         * HTTP response with status code {@code 404} and no body.
         *
         * @param path the suffix path to be handled.
         * @return {@link WebResourceResponse} for the requested file.
         */
        @Override
        @WorkerThread
        @Nullable
        public WebResourceResponse handle(@NonNull String path) {
            Uri uri = new Uri.Builder()
                    .path(path)
                    .build();

            InputStream is = mAssetHelper.openResource(uri);
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return new WebResourceResponse(mimeType, null, is);
        }

    }

    /**
     * Handler class to open file from applicaltion internal storage.
     * For more information about android storage please refer to
     * <a href="https://developer.android.com/guide/topics/data/data-storage">Android Developers
     * Docs: Data and file storage overview</a>.
     * <p>
     * To avoid leaking user or app data to the web, make sure to choose {@code directory}
     * carefully, and assume any file under this directory could be leaked.
     * @hide
     */
    // TODO(b/132880733) unhide the API when it's ready.
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public static final class InternalStoragePathHandler implements PathHandler {
        @NonNull private final File mDirectory;

        /**
         * @param directory the exposed directory under that path.
         */
        public InternalStoragePathHandler(@NonNull File directory) {
            mDirectory = directory;
        }

        /**
         * Opens the requested file from the exposed data directory.
         * <p>
         * The matched prefix path used shouldn't be a prefix of a real web path. Thus, if the
         * requested file cannot be found a {@link WebResourceResponse} object with a {@code null}
         * {@link InputStream} will be returned instead of {@code null}. This saves the time of
         * falling back to network and trying to resolve a path that doesn't exist. A
         * {@link WebResourceResponse} with {@code null} {@link InputStream} will be received as an
         * HTTP response with status code {@code 404} and no body.
         *
         * @param path the suffix path to be handled.
         * @return {@link WebResourceResponse} for the requested file or {@code null} if the
         *         canonical path of the requested file is not in the registered directory.
         */
        @Override
        @WorkerThread
        @Nullable
        public WebResourceResponse handle(@NonNull String path) {
            try {
                InputStream is = AssetHelper.openFile(mDirectory, path);
                String mimeType = URLConnection.guessContentTypeFromName(path);
                return new WebResourceResponse(mimeType, null, is);
            } catch (AssetHelper.FileOutsideMountedDirectoryException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }
    }


    /**
     * Matches URIs on the form: {@code "http(s)://authority/path/**"}, HTTPS is always enabled.
     *
     * <p>
     * Methods of this class will be invoked on a background thread and care must be taken to
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
        PathMatcher(@NonNull final String authority, @NonNull final String path,
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
        @WorkerThread
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

        /**
         * Util method to get the suffix path of a matched path.
         *
         * @param path the full recieved path.
         * @return the suffix path.
         */
        @WorkerThread
        @NonNull
        public String getSuffixPath(@NonNull String path) {
            return path.replaceFirst(mPath, "");
        }
    }

    /**
     * A builder class for constructing {@link WebViewAssetLoader} objects.
     */
    public static final class Builder {
        private boolean mAllowHttp;
        private String mDomain;
        @NonNull private List<PathMatcher> mBuilderMatcherList;

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
         * <p>
         * The path should start and end with a {@code "/"} and it shouldn't collide with a real web
         * path.
         *
         * @param path the prefix path where this handler should be register.
         * @param handler {@link PathHandler} that handles requests for this path.
         * @return {@link Builder} object.
         * @throws IllegalArgumentException if the path is invalid.
         */
        @NonNull
        public Builder register(@NonNull String path, @NonNull PathHandler handler) {
            mBuilderMatcherList.add(new PathMatcher(mDomain, path, mAllowHttp, handler));
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
        for (PathMatcher matcher : mMatchers) {
            PathHandler handler = matcher.match(url);
            // The requested URL doesn't match the URL where this handler has been registered.
            if (handler == null) continue;
            String suffixPath = matcher.getSuffixPath(url.getPath());
            WebResourceResponse response = handler.handle(suffixPath);
            // Handler doesn't want to intercept this request, try next handler.
            if (response == null) continue;

            return response;
        }
        return null;
    }
}
