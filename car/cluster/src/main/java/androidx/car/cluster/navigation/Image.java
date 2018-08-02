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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * A reference to visual navigation element.
 */
@VersionedParcelize
public final class Image implements VersionedParcelable {
    public enum Mode {
        TINTABLE,
    }

    @ParcelField(1)
    IconCompat mImage;
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
     * Creates an image based on the given {@link IconCompat}.
     *
     * @param image image to be transmitted.
     * @param isTintable Indicates whether this image designed to be tinted or not. If this is true,
     *                   the image has all its content in its alpha-channel, designed to be
     *                   colorized (e.g. using {@link android.graphics.PorterDuff.Mode#SRC_ATOP}
     *                   image composition).
     */
    public Image(@NonNull IconCompat image, boolean isTintable) {
        mImage = Preconditions.checkNotNull(image);
        mIsTintable = isTintable;
    }

    /**
     * Returns the image as a {@link IconCompat}
     */
    @NonNull
    public IconCompat getImage() {
        return mImage;
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
