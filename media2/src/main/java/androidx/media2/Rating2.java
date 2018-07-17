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

import androidx.core.util.ObjectsCompat;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * A class to encapsulate rating information used as content metadata.
 */
@VersionedParcelize
public class Rating2 implements VersionedParcelable {
    static final float RATING_NOT_RATED = -1.0f;

    @ParcelField(1)
    float mRatingValue;

    /**
     * Create a Rating2 instance with no rating.
     */
    Rating2() {
        mRatingValue = RATING_NOT_RATED;
    }

    /**
     * Create a Rating2 instance with the given rating value.
     *
     * @param value the rating value
     */
    Rating2(float value) {
        mRatingValue = value;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mRatingValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Rating2)) {
            return false;
        }
        return mRatingValue == ((Rating2) obj).mRatingValue;
    }

    @Override
    public String toString() {
        return "Rating2: rating=" + (mRatingValue < 0.0f
                ? "unrated" : String.valueOf(mRatingValue));
    }

    /**
     * Return whether there is a rating value available.
     * @return true if the instance was created with a non-negative rating value.
     */
    public boolean isRated() {
        return mRatingValue >= 0.0f;
    }
}
