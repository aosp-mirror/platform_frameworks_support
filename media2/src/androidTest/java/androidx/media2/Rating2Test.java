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
        Rating2 srcRating2 = new Rating2();
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        Rating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testRatedRating2() {
        Rating2 srcRating2 = new Rating2(2f);
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        Rating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testUnratedHeartRating2() {
        HeartRating2 srcRating2 = new HeartRating2();
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        HeartRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testRatedHeartRating2() {
        HeartRating2 srcRating2 = new HeartRating2(true);
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        HeartRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testUnratedPercentageRating2() {
        PercentageRating2 srcRating2 = new PercentageRating2();
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        PercentageRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testRatedPercentageRating2() {
        PercentageRating2 srcRating2 = new PercentageRating2(20f);
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        PercentageRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testUnratedThumbRating2() {
        ThumbRating2 srcRating2 = new ThumbRating2();
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        ThumbRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testRatedThumbRating2() {
        ThumbRating2 srcRating2 = new ThumbRating2(false);
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        ThumbRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);
        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testUnratedStarRating2() {
        StarRating2 srcRating2 = new StarRating2(5);
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        StarRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);
        assertEquals(srcRating2, retRating2);
    }

    @Test
    public void testRatedStarRating2() {
        StarRating2 srcRating2 = new StarRating2(5, 3.1f);
        ParcelImpl srcParcelImpl = (ParcelImpl) ParcelUtils.toParcelable(srcRating2);

        Parcel parcel = Parcel.obtain();
        srcParcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParcelImpl retParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        StarRating2 retRating2 = ParcelUtils.fromParcelable(retParcelImpl);

        assertEquals(srcRating2, retRating2);
    }
}
