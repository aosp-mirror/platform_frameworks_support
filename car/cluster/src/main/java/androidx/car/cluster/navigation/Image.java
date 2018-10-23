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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Reference to an image. This class encapsulates a 'content://' style URI plus metadata that would
 * help consumers understand how the image could be handled.
 * <p>
 * <b>Sizing:</b> All images will have default dimension which define their aspect ratio.
 * Consumers might request a different size (by specifying width, height or both). Producers can
 * optionally select a version of the requested image that matches the requested size most closely
 * and resize it to exactly match the size constraints. Regardless on whether the producer resizes
 * the image or not, this class provides helper methods to ensure the right image is retrieved.
 * <b>Color:</b> Images can be either "tintable" or not. A "tintable" image is such that all the
 * relevant information of the image is in its alpha channel, and its color can be altered (e.g.:
 * icons). Non "tintable" images contains color information that must be preserved (e.g.: photos).
 * <b>Caching:</b> Every image reference should point to a single image. Given the same reference
 * and the same requested size, the result should be exactly the same image. This means that it
 * should be safe for the consumer to cache an image once downloaded.
 */
@VersionedParcelize
public class Image {
    private static final String SCHEME = "content://";
    private static final String WIDTH_HINT_PARAMETER = "w";
    private static final String HEIGHT_HINT_PARAMETER = "h";

    @ParcelField(1)
    String mContentUri;
    @ParcelField(2)
    int mDefaultWidth;
    @ParcelField(3)
    int mDefaultHeight;
    @ParcelField(4)
    boolean mIsTintable;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Image() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Image(@NonNull String contentUri, int defaultWidth, int defaultHeight, boolean isTintable) {
        mContentUri = Preconditions.checkNotNull(contentUri);
        mDefaultWidth = defaultWidth;
        mDefaultHeight = defaultHeight;
        mIsTintable = isTintable;
    }

    /**
     * Builder for creating a {@link Lane}
     */
    public static final class Builder {
        String mContentUri;
        int mDefaultWidth;
        int mDefaultHeight;
        boolean mIsTintable;

        /**
         * Sets a'content://' style URI
         */
        @NonNull
        public Builder setContentUri(@NonNull String contentUri) {
            Preconditions.checkArgument(contentUri.startsWith(SCHEME));
            mContentUri = Preconditions.checkNotNull(contentUri);
            return this;
        }

        /**
         * Sets the default dimensions of this image. Both dimensions must be greater than 0.
         */
        @NonNull
        public Builder setDefaultSize(int width, int height) {
            Preconditions.checkArgument(width > 0 && height > 0);
            mDefaultWidth = width;
            mDefaultHeight = height;
            return this;
        }

        /**
         * Sets whether this image is "tintable" or not. An image is "tintable" when all its
         * content is defined in its alpha-channel, designed to be colorized (e.g. using
         * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition).
         */
        @NonNull
        public Builder setIsTintable(boolean isTintable) {
            mIsTintable = isTintable;
            return this;
        }

        /**
         * Returns a {@link Image} built with the provided information.
         */
        @NonNull
        public Image build() {
            return new Image(mContentUri, mDefaultWidth, mDefaultHeight, mIsTintable);
        }
    }

    /**
     * Returns a 'content://' style URI that can be used to retrieve the actual image, or an empty
     * string if the URI provided by the producer doesn't comply with the format requirements. The
     * image returned when using this URI as-is will have the size defined by
     * {@link #getDefaultWidth()} and {@link #getDefaultHeight}.
     */
    @NonNull
    public String getRawContentUri() {
        String value = Common.nonNullOrEmpty(mContentUri);
        return value.startsWith(SCHEME) ? value : "";
    }

    /**
     * Returns a fully formed {@link Uri} that can be used to retrieve the actual image, including
     * sizing hints. Producers can optionally use this hints to provide an optimized version of the
     * image, but the resulting image might still not match the requested size exactly.
     * <p>
     * Consumers must confirm the size of the received image and resize it accordingly if it doesn't
     * match the desired dimensions.
     *
     * @param width desired width hint, or 0 if width should be left unconstrained.
     * @param height desired height hint, or 0 if height should be left unconstrained.
     * @return fully formed {@link Uri} including all provided hints.
     */
    @Nullable
    public Uri getContentUri(int width, int height) {
        String contentUri = getRawContentUri();
        if (contentUri.isEmpty()) {
            // We have an invalid content URI.
            return null;
        }
        Uri.Builder builder = Uri.parse(contentUri).buildUpon();
        if (width > 0) {
            builder.appendQueryParameter(WIDTH_HINT_PARAMETER, String.valueOf(width));
        }
        if (height > 0) {
            builder.appendQueryParameter(HEIGHT_HINT_PARAMETER, String.valueOf(height));
        }
        return builder.build();
    }

    /**
     * Returns the image's default width
     */
    public int getDefaultWidth() {
        return mDefaultWidth;
    }

    /**
     * Returns the image's default height
     */
    public int getDefaultHeight() {
        return mDefaultHeight;
    }

    /**
     * Returns whether this image is "tintable" or not. An image is "tintable" when all its
     * content is defined in its alpha-channel, designed to be colorized (e.g. using
     * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition).
     */
    public boolean isTintable() {
        return mIsTintable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Image image = (Image) o;
        return Objects.equals(getRawContentUri(), image.getRawContentUri())
                && getDefaultWidth() == image.getDefaultWidth()
                && getDefaultHeight() == image.getDefaultHeight()
                && isTintable() == image.isTintable();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawContentUri(), getDefaultWidth(), getDefaultHeight(),
                isTintable());
    }

    // DefaultLocale suppressed as this method is only offered for debugging purposes.
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{contentUri: '%s', defaultWidth: %d, defaultHeight: %d, "
                        + "isTintable: %s}",
                mContentUri, mDefaultWidth, mDefaultHeight, mIsTintable);
    }
}
