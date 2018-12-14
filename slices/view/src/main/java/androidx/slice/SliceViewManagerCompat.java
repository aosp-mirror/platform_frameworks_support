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

package androidx.slice;

import static androidx.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.compat.SliceProviderCompat;
import androidx.slice.widget.SliceLiveData;

import java.util.Collection;


/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
class SliceViewManagerCompat extends SliceViewManagerBase {

    SliceViewManagerCompat(Context context) {
        super(context);
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void pinSlice(@NonNull Uri uri) {
        SliceProviderCompat.pinSlice(mContext, uri, SliceLiveData.SUPPORTED_SPECS);
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void unpinSlice(@NonNull Uri uri) {
        SliceProviderCompat.unpinSlice(mContext, uri, SliceLiveData.SUPPORTED_SPECS);
    }

    @Nullable
    @Override
    @SuppressLint("RestrictedApi")
    public Slice bindSlice(@NonNull Uri uri) {
        return SliceProviderCompat.bindSlice(mContext, uri, SUPPORTED_SPECS);
    }

    @Nullable
    @Override
    @SuppressLint("RestrictedApi")
    public Slice bindSlice(@NonNull Intent intent) {
        return SliceProviderCompat.bindSlice(mContext, intent, SUPPORTED_SPECS);
    }

    @Nullable
    @Override
    @SuppressLint("RestrictedApi")
    public Uri mapIntentToUri(@NonNull Intent intent) {
        return SliceProviderCompat.mapIntentToUri(mContext, intent);
    }

    @Override
    @SuppressLint("RestrictedApi")
    public Collection<Uri> getSliceDescendants(Uri uri) {
        return SliceProviderCompat.getSliceDescendants(mContext, uri);
    }
}
