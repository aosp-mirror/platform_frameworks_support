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
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
  * Handles opening resources and assets and finds MimeType
  * Forked from the chromuim project org.chromium.android_webview.AndroidProtocolHandler
  */
public class AssetHelper {
    private static final String TAG = "AssetHelper";

    @NonNull private Context mContext;

    public AssetHelper(@NonNull Context context) {
        this.mContext = context;
    }

    @VisibleForTesting
    public AssetHelper() {
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
        String packageName = mContext.getPackageName();

        int id = mContext.getResources().getIdentifier(assetName, assetType, packageName);
        // Asset id is found.
        if (id != 0) {
            return id;
        }
        // Else lookup the R class using reflection.
        Class<?> clazz = null;
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
        id = field.getInt(null);
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
     * @param uri The uri to load. The path must be of the form "asset_type/asset_name.ext".
     * @return An InputStream to the Android resource.
     */
    @Nullable
    public InputStream openResource(@NonNull Uri uri) {
        // assert uri.getPath() != null;
        if (uri.getPath() == null) {
            throw new AssertionError("Uri.path cannot be null");
        }
        // The path must be of the form "asset_type/asset_name.ext".
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 2) {
            Log.e(TAG, "Incorrect resource path: " + pathSegments.size());
            return null;
        }
        String assetType = pathSegments.get(0);
        String assetName = pathSegments.get(1);
        Log.e(TAG, "assetType: " + assetType + " - assetName: " + assetName);

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
     * @param uri The uri to load.
     * @return An InputStream to the Android asset.
     */
    @Nullable
    public InputStream openAsset(@NonNull Uri uri) {
        // assert uri.getPath() != null;
        if (uri.getPath() == null) {
            throw new AssertionError("Uri.path cannot be null");
        }
        String path = uri.getPath();
        // Strip leading slash if present.
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        try {
            AssetManager assets = mContext.getAssets();
            return handleSvgzStream(uri, assets.open(path, AssetManager.ACCESS_STREAMING));
        } catch (IOException e) {
            Log.e(TAG, "Unable to open asset URL: " + uri);
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
