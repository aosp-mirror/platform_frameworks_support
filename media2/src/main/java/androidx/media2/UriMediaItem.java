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

package androidx.media2;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structure for media item descriptor for {@link Uri}.
 * <p>
 * Users should use {@link Builder} to create {@link UriMediaItem}.
 * <p>
 * You cannot directly send this object across the process through {@link ParcelUtils}. See
 * {@link MediaItem} for detail.
 *
 * @see MediaItem
 * @hide
 */
@VersionedParcelize(isCustom = true)
public class UriMediaItem extends MediaItem {
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Uri mUri;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, String> mUriHeader;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<HttpCookie> mUriCookies;

    /**
     * Used for VersionedParcelable
     */
    UriMediaItem() {
        // no-op
    }

    UriMediaItem(Builder builder) {
        super(builder);
        mUri = builder.mUri;
        mUriHeader = builder.mHeader;
        mUriCookies = builder.mCookies;
    }

    /**
     * Return the Uri of this media item.
     * @return the Uri of this media item
     */
    public @NonNull Uri getUri() {
        return mUri;
    }

    /**
     * Return the Uri headers of this media item.
     * @return the Uri headers of this media item
     */
    public @Nullable Map<String, String> getUriHeaders() {
        if (mUriHeader == null) {
            return null;
        }
        return new HashMap<String, String>(mUriHeader);
    }

    /**
     * Return the Uri cookies of this media item.
     * @return the Uri cookies of this media item
     */
    public @Nullable List<HttpCookie> getUriCookies() {
        if (mUriCookies == null) {
            return null;
        }
        return new ArrayList<HttpCookie>(mUriCookies);
    }
}
