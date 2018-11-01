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

package androidx.media.test.service.tests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaMetadata2.Builder;
import androidx.media2.MediaUtils2;
import androidx.media2.Rating2;
import androidx.media2.ThumbRating2;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaMetadata2Test {
    private static final String TAG = "MediaMetadata2Test";

    @Test
    public void testBuilder() {
        final Bundle extras = new Bundle();
        extras.putString("MediaMetadata2Test", "testBuilder");
        final String title = "title";
        final long discNumber = 10;
        final Rating2 rating = new ThumbRating2(true);

        Builder builder = new Builder();
        builder.setExtras(extras);
        builder.putString(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, title);
        builder.putLong(MediaMetadata2.METADATA_KEY_DISC_NUMBER, discNumber);
        builder.putRating(MediaMetadata2.METADATA_KEY_USER_RATING, rating);

        MediaMetadata2 metadata = builder.build();
        assertTrue(TestUtils.equals(extras, metadata.getExtras()));
        assertEquals(title, metadata.getString(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE));
        assertEquals(discNumber, metadata.getLong(MediaMetadata2.METADATA_KEY_DISC_NUMBER));
        assertEquals(rating, metadata.getRating(MediaMetadata2.METADATA_KEY_USER_RATING));
    }

    @Test
    public void testPuttingSmallBitmap() {
        // 128 x 128 with ARGB_8888 is the maximum bitmap that can be set.
        Bitmap smallBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        int size = MediaUtils2.getBitmapSizeInBytes(smallBitmap);
        Log.d(TAG, "Small Bitmap size=" + size);
        assertTrue("Bitmap used in this test should be equal or less than " + size + " bytes",
                size <= Builder.MAX_BITMAP_SIZE_IN_BYTES);

        Builder builder = new Builder();
        // This should not fail.
        builder.putBitmap(MediaMetadata2.METADATA_KEY_ALBUM_ART, smallBitmap);
    }

    @Test
    public void testPuttingLargeBitmapThrowsException() {
        Bitmap largeBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        int size = MediaUtils2.getBitmapSizeInBytes(largeBitmap);
        Log.d(TAG, "Large Bitmap size=" + size);
        assertTrue("Bitmap used in this test should be greater than " + size + " bytes",
                size > Builder.MAX_BITMAP_SIZE_IN_BYTES);

        Builder builder = new Builder();
        try {
            builder.putBitmap(MediaMetadata2.METADATA_KEY_ALBUM_ART, largeBitmap);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
