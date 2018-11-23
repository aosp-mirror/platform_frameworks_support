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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
  * Implements the Java side of Android URL protocol jobs.
  */
public class AndroidProtocolHandler {
    private static final String TAG = "AndroidProtocolHandler";

    @NonNull private Context mContext;

    public AndroidProtocolHandler(@NonNull Context context) {
        this.mContext = context;
    }

    @Nullable
    public InputStream openAsset(@NonNull String path) throws IOException {
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