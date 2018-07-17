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
 * A class for rating with a single degree of rating, "heart" vs "no heart".
 * This can be used to indicate the content referred to is a favorite (or not).
 */
@VersionedParcelize
public final class HeartRating2 extends Rating2 {
    /**
     * Creates a unrated HeartRating2 instance.
     */
    public HeartRating2() {
        super();
    }

    /**
     * Creates a HeartRating2 instance with the given value.
     *
     * @param hasHeart true for a "heart selected" rating, false for "heart unselected".
     */
    public HeartRating2(boolean hasHeart) {
        super(hasHeart ? 1.0f : 0.0f);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HeartRating2)) {
            return false;
        }
        return mRatingValue == ((Rating2) obj).mRatingValue;
    }

    @Override
    public String toString() {
        return "HeartRating2: " + (isRated() ? "hasHeart=" + hasHeart() : "unrated");
    }

    /**
     * Returns whether the rating is "heart selected".
     *
     * @return true if the rating is "heart selected", false if the rating is "heart unselected",
     *         or if it is unrated.
     */
    public boolean hasHeart() {
        return mRatingValue == 1.0f;
    }
}
