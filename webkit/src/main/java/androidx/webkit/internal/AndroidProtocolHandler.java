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

package androidx.webkit.internal;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
  * Implements the Java side of Android URL protocol jobs.
  */
public class AndroidProtocolHandler {
    private static final String TAG = "AndroidProtocolHandler";

    private static final String FILE_SCHEME = "file";
    private static final String CONTENT_SCHEME = "content";

    @NonNull private Context mContext;

    public AndroidProtocolHandler(@NonNull Context context) {
        this.mContext = context;
        // Use the application context for resolving the resource package name so that we do
        // not use the browser's own resources. Note that if 'context' here belongs to the
        // test suite, it does not have a separate application context. In that case we use
        // the original context object directly.
        if (mContext.getApplicationContext() != null) {
            mContext = mContext.getApplicationContext();
        }
    }

    @VisibleForTesting
    public AndroidProtocolHandler() {
        this.mContext = null;
    }

    @Nullable
    private static InputStream handleSvgzStream(@NonNull Uri uri, @Nullable InputStream stream) {
        if (stream != null && uri.getLastPathSegment().endsWith(".svgz")) {
            try {
                stream = new GZIPInputStream(stream);
            } catch (IOException e) {
                Log.e(TAG, "Error decompressing " + uri + " - " + e.getMessage());
                return null;
            }
        }
        return stream;
    }

    // Assumes input string is in the format "foo.bar.baz" and strips out the last component.
    // Returns null on failure.
    @Nullable
    private static String removeOneSuffix(@Nullable String input) {
        if (input == null) return null;
        int lastDotIndex = input.lastIndexOf('.');
        if (lastDotIndex == -1) return null;
        return input.substring(0, lastDotIndex);
    }

    @NonNull
    private Class<?> getClazz(@NonNull String packageName, @NonNull String assetType)
            throws ClassNotFoundException {
        return mContext.getClassLoader().loadClass(packageName + ".R$" + assetType);
    }

    private int getFieldId(@NonNull String assetType, @NonNull String assetName)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = null;
        String packageName = mContext.getPackageName();
        try {
            clazz = getClazz(packageName, assetType);
        } catch (ClassNotFoundException e) {
            // Strip out components from the end so gradle generated application suffix such as
            // com.example.my.pkg.pro works. This is by no means bulletproof.
            while (clazz == null) {
                packageName = removeOneSuffix(packageName);
                // Throw original exception which contains the entire package id.
                if (packageName == null) throw e;
                try {
                    clazz = getClazz(packageName, assetType);
                } catch (ClassNotFoundException ignored) {
                    // Strip and try again.
                }
            }
        }

        java.lang.reflect.Field field = clazz.getField(assetName);
        int id = field.getInt(null);
        return id;
    }

    private int getValueType(int fieldId) {
        TypedValue value = new TypedValue();
        mContext.getResources().getValue(fieldId, value, true);
        return value.type;
    }

    /**
     * Open an InputStream for an Android resource.
     *
     * @param url The url to load.
     * @return An InputStream to the Android resource.
     */
    @Nullable
    public InputStream openResource(@NonNull Uri uri) {
        assert uri.getScheme().equals(FILE_SCHEME);
        assert uri.getPath() != null;
        // The path must be of the form "/android_res/asset_type/asset_name.ext".
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            Log.e(TAG, "Incorrect resource path: " + uri);
            return null;
        }
        String assetType = pathSegments.get(1);
        String assetName = pathSegments.get(2);

        // Drop the file extension.
        assetName = assetName.split("\\.")[0];
        try {
            int fieldId = getFieldId(assetType, assetName);
            int valueType = getValueType(fieldId);
            if (valueType == TypedValue.TYPE_STRING) {
                return handleSvgzStream(uri, mContext.getResources().openRawResource(fieldId));
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

    /**
     * Open an InputStream for an Android asset.
     *
     * @param url The url to load.
     * @return An InputStream to the Android asset.
     */
    @Nullable
    public InputStream openAsset(@NonNull Uri uri) {
        assert uri.getScheme().equals(FILE_SCHEME);
        assert uri.getPath() != null;
        String path = uri.getPath();
        try {
            AssetManager assets = mContext.getAssets();
            return handleSvgzStream(uri, assets.open(path, AssetManager.ACCESS_STREAMING));
        } catch (IOException e) {
            Log.e(TAG, "Unable to open asset URL: " + uri);
            return null;
        }
    }

    /**
     * Determine the mime type for an Android resource.
     *
     * @param stream  The opened input stream which to examine.
     * @param url     The url from which the stream was opened.
     * @return The mime type or null if the type is unknown.
     */
    @Nullable
    public String getMimeType(@Nullable InputStream stream, @Nullable String url) {
        Uri uri = verifyUrl(url);
        if (uri == null) {
            return null;
        }
        try {
            String path = uri.getPath();
            // The content URL type can be queried directly.
            if (uri.getScheme().equals(CONTENT_SCHEME)) {
                return mContext.getContentResolver().getType(uri);
                // Asset files may have a known extension.
            } else if (uri.getScheme().equals(FILE_SCHEME)) {
                String mimeType = URLConnection.guessContentTypeFromName(path);
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unable to get mime type" + url);
            return null;
        }
        // Fall back to sniffing the type from the stream.
        try {
            if (stream == null) {
                return null;
            }
            return URLConnection.guessContentTypeFromStream(stream);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Make sure the given string URL is correctly formed and parse it into a Uri.
     *
     * @return a Uri instance, or null if the URL was invalid.
     */
    @Nullable
    private static Uri verifyUrl(@Nullable String url) {
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
}