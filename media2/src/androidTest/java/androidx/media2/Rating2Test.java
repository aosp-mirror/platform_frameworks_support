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

import static org.junit.Assert.assertEquals;

import android.os.Parcel;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link Rating2} and its subclasses.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class Rating2Test extends MediaTestBase {
    @Test
    public void testUnratedRating2() {
        Rating2 rating2 = new Rating2();
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedRating2() {
        Rating2 rating2 = new Rating2(2f);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedHeartRating2() {
        HeartRating2 rating2 = new HeartRating2();
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedHeartRating2() {
        HeartRating2 rating2 = new HeartRating2(true);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedPercentageRating2() {
        PercentageRating2 rating2 = new PercentageRating2();
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedPercentageRating2() {
        PercentageRating2 rating2 = new PercentageRating2(20f);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedThumbRating2() {
        ThumbRating2 rating2 = new ThumbRating2();
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedThumbRating2() {
        ThumbRating2 rating2 = new ThumbRating2(false);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedStarRating2() {
        StarRating2 rating2 = new StarRating2(5);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedStarRating2() {
        StarRating2 rating2 = new StarRating2(5, 3.1f);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    private Rating2 writeToParcelAndCreateRating2(Rating2 rating2) {
        ParcelImpl parcelImpl = (ParcelImpl) ParcelUtils.toParcelable(rating2);
        Parcel parcel = Parcel.obtain();
        parcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ParcelImpl newParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return ParcelUtils.fromParcelable(newParcelImpl);
    }
}
