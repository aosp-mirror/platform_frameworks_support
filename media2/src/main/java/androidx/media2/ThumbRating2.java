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
 * A class for rating with a single degree of rating, "thumb up" vs "thumb down".
 */
@VersionedParcelize
public final class ThumbRating2 extends Rating2 {
    /**
     * Creates a unrated ThumbRating2 instance.
     */
    public ThumbRating2() {
        super();
    }

    /**
     * Creates a ThumbRating2 instance with the given percentage.
     *
     * @param thumbIsUp true for a "thumb up" rating, false for "thumb down".
     */
    public ThumbRating2(boolean thumbIsUp) {
        super(thumbIsUp ? 1.0f : 0.0f);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ThumbRating2)) {
            return false;
        }
        return mRatingValue == ((Rating2) obj).mRatingValue;
    }

    @Override
    public String toString() {
        return "ThumbRating2: " + (isRated() ? "isThumbUp=" + isThumbUp() : "unrated");
    }

    /**
     * Returns whether the rating is "thumb up".
     *
     * @return true if the rating is "thumb up", false if the rating is "thumb down",
     *         or if it is unrated.
     */
    public boolean isThumbUp() {
        return mRatingValue == 1.0f;
    }
}
