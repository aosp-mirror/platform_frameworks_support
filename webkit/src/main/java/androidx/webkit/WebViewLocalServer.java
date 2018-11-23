/*
Copyright 2018 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package androidx.webkit;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Helper class meant to be used with the android.webkit.WebView class to enable hosting assets,
 * resources and other data on 'virtual' http(s):// URL.
 * Hosting assets and resources on http(s):// URLs is desirable as it is compatible with the
 * Same-Origin policy.
 *
 * This class is intended to be used from within the
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)} and
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
 * android.webkit.WebResourceRequest)}
 * methods.
 * <pre>
 *     WebViewLocalServer localServer = new WebViewLocalServer(this);
 *     // For security WebViewLocalServer uses a unique subdomain by default.
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
 */
public class WebViewLocalServer {
    private static String TAG = "WebViewAssetServer";
    /**
     * The androidplatform.net domain currently belongs to Google and has been reserved for the
     * purpose of Android applications intercepting navigations/requests directed there.
     */
    public final static String KNOWN_UNUSED_AUTHORITY = "androidplatform.net";

    private final static String sHttpScheme = "http";
    private final static String sHttpsScheme = "https";

    @NonNull private final UriMatcher mUriMatcher;
    @NonNull private final AndroidProtocolHandler mProtocolHandler;
    @NonNull private final String mAuthority;

    static class UriMatcher {
        static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");

        private static final int EXACT = 0;
        private static final int TEXT = 1;
        private static final int REST = 2;

        @Nullable private Object mCode;
        private int mWhich;
        @Nullable private String mText;
        @NonNull private ArrayList<UriMatcher> mChildren;

        /**
         * Creates the root node of the URI tree.
         *
         * @param code the code to match for the root URI
         */
        public UriMatcher(@Nullable Object code) {
            mCode = code;
            mWhich = -1;
            mChildren = new ArrayList<UriMatcher>();
            mText = null;
        }

        private UriMatcher() {
            mCode = null;
            mWhich = -1;
            mChildren = new ArrayList<UriMatcher>();
            mText = null;
        }

        /**
         * Add a URI to match, and the code to return when this URI is
         * matched. URI nodes may be exact match string, the token "*"
         * that matches any text, or the token "#" that matches only
         * numbers.
         * <p>
         * Starting from API level {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2},
         * this method will accept a leading slash in the path.
         *
         * @param scheme the scheme (http/https) to match
         * @param authority the authority to match
         * @param path the path to match. * may be used as a wild card for
         * any text, and # may be used as a wild card for numbers.
         * @param code the code that is returned when a URI is matched
         * against the given components. Must be positive.
         */
        public void addURI(@NonNull String scheme, @NonNull String authority, @Nullable String path, @NonNull Object code) {
            if (code == null) {
                throw new IllegalArgumentException("Code can't be null");
            }

            String[] tokens = null;
            if (path != null) {
                String newPath = path;
                // Strip leading slash if present.
                if (path.length() > 0 && path.charAt(0) == '/') {
                    newPath = path.substring(1);
                }
                tokens = PATH_SPLIT_PATTERN.split(newPath);
            }

            int numTokens = tokens != null ? tokens.length : 0;
            UriMatcher node = this;
            for (int i = -2; i < numTokens; i++) {
                String token;
                if (i == -2)
                    token = scheme;
                else if (i == -1)
                    token = authority;
                else
                    token = tokens[i];
                ArrayList<UriMatcher> children = node.mChildren;
                int numChildren = children.size();
                UriMatcher child;
                int j;
                for (j = 0; j < numChildren; j++) {
                    child = children.get(j);
                    if (token.equals(child.mText)) {
                        node = child;
                        break;
                    }
                }
                if (j == numChildren) {
                    // Child not found, create it
                    child = new UriMatcher();
                    if (token.equals("**")) {
                        child.mWhich = REST;
                    } else if (token.equals("*")) {
                        child.mWhich = TEXT;
                    } else {
                        child.mWhich = EXACT;
                    }
                    child.mText = token;
                    node.mChildren.add(child);
                    node = child;
                }
            }
            node.mCode = code;
        }

        /**
         * Try to match against the path in a url.
         *
         * @param uri The url whose path we will match against.
         *
         * @return  The code for the matched node (added using addURI),
         * or null if there is no matched node.
         */
        @Nullable
        public Object match(@NonNull Uri uri) {
            final List<String> pathSegments = uri.getPathSegments();
            final int li = pathSegments.size();

            UriMatcher node = this;

            if (li == 0 && uri.getAuthority() == null) {
                return this.mCode;
            }

            for (int i=-2; i<li; i++) {
                String u;
                if (i == -2)
                    u = uri.getScheme();
                else if (i == -1)
                    u = uri.getAuthority();
                else
                    u = pathSegments.get(i);
                ArrayList<UriMatcher> list = node.mChildren;
                if (list == null) {
                    break;
                }
                node = null;
                int lj = list.size();
                for (int j=0; j<lj; j++) {
                    UriMatcher n = list.get(j);
                    which_switch:
                    switch (n.mWhich) {
                        case EXACT:
                            if (n.mText.equals(u)) {
                                node = n;
                            }
                            break;
                        case TEXT:
                            node = n;
                            break;
                        case REST:
                            return n.mCode;
                    }
                    if (node != null) {
                        break;
                    }
                }
                if (node == null) {
                    return null;
                }
            }

            return node.mCode;
        }
    }

    /**
     * Implements the Java side of Android URL protocol jobs.
     */
    public static class AndroidProtocolHandler {
        private static final String TAG = "AndroidProtocolHandler";

        @NonNull private Context mContext;

        public AndroidProtocolHandler(@NonNull Context context) {
            this.mContext = context;
        }

        public InputStream openAsset(String path) throws IOException {
            return mContext.getAssets().open(path, AssetManager.ACCESS_STREAMING);
        }

        @Nullable
        public InputStream openResource(@NonNull Uri uri) {
            assert uri.getPath() != null;
            // The path must be of the form ".../asset_type/asset_name.ext".
            List<String> pathSegments = uri.getPathSegments();
            String assetType = pathSegments.get(pathSegments.size() - 2);
            String assetName = pathSegments.get(pathSegments.size() - 1);

            // Drop the file extension.
            assetName = assetName.split("\\.")[0];
            try {
                // Use the application context for resolving the resource package name so that we do
                // not use the browser's own resources. Note that if 'context' here belongs to the
                // test suite, it does not have a separate application context. In that case we use
                // the original context object directly.
                if (mContext.getApplicationContext() != null) {
                    mContext = mContext.getApplicationContext();
                }
                int fieldId = getFieldId(mContext, assetType, assetName);
                int valueType = getValueType(mContext, fieldId);
                if (valueType == TypedValue.TYPE_STRING) {
                    return mContext.getResources().openRawResource(fieldId);
                } else {
                    Log.e(TAG, "Asset not of type string: " + uri);
                    return null;
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Unable to open resource URL: " + uri, e);
                return null;
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Unable to open resource URL: " + uri, e);
                return null;
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Unable to open resource URL: " + uri, e);
                return null;
            }
        }

        private static int getFieldId(@NonNull Context context, @NonNull String assetType, @NonNull String assetName)
                throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
            Class<?> d = context.getClassLoader()
                    .loadClass(context.getPackageName() + ".R$" + assetType);
            java.lang.reflect.Field field = d.getField(assetName);
            int id = field.getInt(null);
            return id;
        }

        private static int getValueType(@NonNull Context context, int fieldId) {
            TypedValue value = new TypedValue();
            context.getResources().getValue(fieldId, value, true);
            return value.type;
        }
    }

    /**
     * A handler that produces responses for paths on the virtual asset server.
     *
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     *
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoke.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spend blocking in this method should be kept to an absolute
     * minimum.
     */
    public abstract static class PathHandler {
        @Nullable private String mimeType;
        @Nullable private String encoding;
        @Nullable private String charset;
        @Nullable private int statusCode;
        @Nullable private String reasonPhrase;
        @Nullable private Map<String, String> responseHeaders;

        public PathHandler() {
            this(null, null, null, 200, "OK", null);
        }

        public PathHandler(@Nullable String mimeType, @Nullable String encoding, @Nullable String charset, int statusCode,
                        @Nullable String reasonPhrase, @Nullable Map<String, String> responseHeaders) {
            this.mimeType = mimeType;
            this.encoding = encoding;
            this.charset = charset;
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.responseHeaders = responseHeaders;
        }

        @Nullable
        public InputStream handle(WebResourceRequest request) {
            return handle(request.getUrl());
        }

        @Nullable
        abstract public InputStream handle(Uri url);

        @Nullable
        public String getMimeType() {
            return mimeType;
        }

        @Nullable
        public String getEncoding() {
            return encoding;
        }

        @Nullable
        public String getCharset() {
            return charset;
        }

        @Nullable
        public int getStatusCode() {
            return statusCode;
        }

        @Nullable
        public String getReasonPhrase() {
            return reasonPhrase;
        }

        @Nullable
        public Map<String, String> getResponseHeaders() {
            return responseHeaders;
        }
    }

    /**
     * Information about the URLs used to host the assets in the WebView.
     */
    public static class AssetHostingDetails {
        @NonNull private Uri httpPrefix;
        @NonNull private Uri httpsPrefix;

        /*package*/ AssetHostingDetails(@NonNull Uri httpPrefix, @NonNull Uri httpsPrefix) {
            this.httpPrefix = httpPrefix;
            this.httpsPrefix = httpsPrefix;
        }

        /**
         * Gets the http: scheme prefix at which assets are hosted.
         * @return  the http: scheme prefix at which assets are hosted. Can return null.
         */
        @NonNull
        public Uri getHttpPrefix() {
            return httpPrefix;
        }

        /**
         * Gets the https: scheme prefix at which assets are hosted.
         * @return  the https: scheme prefix at which assets are hosted. Can return null.
         */
        @NonNull
        public Uri getHttpsPrefix() {
            return httpsPrefix;
        }
    }

    /*package*/ WebViewLocalServer(@NonNull AndroidProtocolHandler mProtocolHandler) {
        mUriMatcher = new UriMatcher(null);
        this.mProtocolHandler = mProtocolHandler;
        mAuthority = UUID.randomUUID().toString() + "." + KNOWN_UNUSED_AUTHORITY;
    }

    /**
     * Creates a new instance of the WebView local server.
     *
     * @param context context used to resolve resources/assets/
     */
    public WebViewLocalServer(@NonNull Context context) {
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
    public WebResourceResponse shouldInterceptRequest(String url) {
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
     * @param uri the uri to use the handler for. The scheme and mAuthority (domain) will be matched
     *            exactly. The path may contain a '*' element which will match a single element of
     *            a path (so a handler registered for /a/* will be invoked for /a/b and /a/c.html
     *            but not for /a/b/b) or the '**' element which will match any number of path
     *            elements.
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
    public AssetHostingDetails hostAssets(String assetPath) {
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
