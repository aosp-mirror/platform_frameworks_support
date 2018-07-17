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

import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * A class for rating expressed as a percentage.
 */
@VersionedParcelize
public final class StarRating2 extends Rating2 {
    @ParcelField(1)
    int mMaxStars;

    /*
     * Used for VersionedParcelable
     */
    StarRating2() {
    }

    /**
     * Creates a unrated StarRating2 instance with {@code maxStars}.
     *
     * @param maxStars a range of this star rating from 0.0f to {@code maxStarts}
     */
    public StarRating2(int maxStars) {
        super();
    }

    /**
     * Creates a StarRating2 instance with {@code maxStars} and the given integer or fractional
     * number of stars. Non integer values can for instance be used to represent an average rating
     * value, which might not be an integer number of stars.
     * If {@code maxStars} is not a positive integer or {@code starRating} has invalid value,
     * it will create unrated star rating instance.
     *
     * @param maxStars a range of this star rating from 0.0f to {@code maxStarts}
     * @param starRating a number ranging from 0.0f to {@code maxStars}
     */
    public StarRating2(int maxStars, float starRating) {
        super(((starRating < 0.0f) || (starRating > maxStars)) ? RATING_NOT_RATED : starRating);
        mMaxStars = maxStars;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StarRating2)) {
            return false;
        }
        StarRating2 other = (StarRating2) obj;
        return mMaxStars == other.mMaxStars && mRatingValue == other.mRatingValue;
    }

    @Override
    public String toString() {
        return "StarRating2: maxStars=" + mMaxStars
                + (isRated() ? ", starRating=" + mRatingValue : ", unrated");
    }

    /**
     * Returns the max stars.
     *
     * @return a max number of stars for this star rating.
     */
    public int getMaxStars() {
        return mMaxStars;
    }

    /**
     * Returns the star-based rating value.
     *
     * @return a rating value greater or equal to 0.0f, or a negative value if it is unrated.
     */
    public float getStarRating() {
        return mRatingValue;
    }
}
