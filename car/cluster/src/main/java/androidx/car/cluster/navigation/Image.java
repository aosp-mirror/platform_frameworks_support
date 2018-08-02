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

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * A reference to visual navigation element.
 */
@VersionedParcelize
public final class Image implements VersionedParcelable {
    @ParcelField(1)
    Icon mImage;
    @ParcelField(2)
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
     * Creates an image based on the given URI.
     *
     * @param uri A uri referring to local content:// or android.resource:// image data.
     * @param isTintable Indicates whether this image designed to be tinted or not. If this is true,
     *                   the image has all its content in its alpha-channel, designed to be
     *                   colorized (e.g. using {@link android.graphics.PorterDuff.Mode#SRC_ATOP}
     *                   image composition).
     */
    public Image(@NonNull String uri, boolean isTintable) {
        mImage = Icon.createWithContentUri(uri);
        mIsTintable = isTintable;
    }

    /**
     * Creates an image based on the given resource.
     *
     * @param context The context for the application whose resources should be used to resolve the
     *                given resource ID.
     * @param resId ID of the drawable resource
     * @param isTintable Indicates whether this image designed to be tinted or not. If this is true,
     *                   the image has all its content in its alpha-channel, designed to be
     *                   colorized (e.g. using {@link android.graphics.PorterDuff.Mode#SRC_ATOP}
     *                   image composition).
     */
    public Image(@NonNull Context context, @DrawableRes int resId, boolean isTintable) {
        mImage = Icon.createWithResource(context, resId);
        mIsTintable = isTintable;
    }

    /**
     * Returns a Drawable that can be used to draw the image inside this Icon, constructing it
     * if necessary.
     *
     * @param context {@link android.content.Context Context} in which to load the drawable; used
     *                to access {@link android.content.res.Resources Resources}, for example.
     * @param size Optimal image size desired. A provider should return an image bigger but as close
     *             to this size as possible. The returned image will be stretched by the consumer
     *             to fit in this size.
     * @return A fresh instance of a drawable for this image, yours to keep.
     */
    @NonNull
    public Drawable loadDrawable(Context context, Point size) {
        // TODO: Find a way to pass the size to the DocumentProvider.
        return mImage.loadDrawable(context);
    }

    /**
     * Indicates whether this image designed to be tinted or not. If this is true, the image has all
     * its content in its alpha-channel, designed to be colorized (e.g. using
     * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition). Otherwise, the image
     * should be used as-is.
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
        return Objects.equals(mImage, image.mImage)
                && mIsTintable == image.mIsTintable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mImage, mIsTintable);
    }
}
