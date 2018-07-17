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

import androidx.versionedparcelable.VersionedParcelize;

/**
 * A class for rating expressed as a percentage.
 */
@VersionedParcelize
public final class PercentageRating2 extends Rating2 {
    /**
     * Creates a unrated PercentageRating2 instance.
     */
    public PercentageRating2() {
        super();
    }

    /**
     * Creates a PercentageRating2 instance with the given percentage.
     * If {@code percent} is less than 0f or greater than 100f, it will create unrated instance.
     *
     * @param percent the value of the rating
     */
    public PercentageRating2(float percent) {
        super(((percent < 0.0f) || (percent > 100.0f)) ? RATING_NOT_RATED : percent);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PercentageRating2)) {
            return false;
        }
        return mRatingValue == ((Rating2) obj).mRatingValue;
    }

    @Override
    public String toString() {
        return "PercentageRating2: "
                + (isRated() ? "percentage=" + mRatingValue : "unrated");
    }

    /**
     * Returns the percentage-based rating value.
     *
     * @return a rating value greater or equal to 0.0f, or a negative value if it is unrated.
     */
    public float getPercentRating() {
        return mRatingValue;
    }
}
