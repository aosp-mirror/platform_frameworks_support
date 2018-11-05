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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;

import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaUtils2;
import androidx.media2.UriMediaItem2;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.versionedparcelable.ParcelImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link MediaItem2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaItem2Test {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testSubclass_sameProcess() {
        final UriMediaItem2 testUriItem = new UriMediaItem2.Builder(
                mContext, Uri.parse("test://test")).build();
        final ParcelImpl parcel = MediaUtils2.toParcelable(testUriItem);

        final MediaItem2 testRemoteItem = MediaUtils2.fromParcelable(parcel);
        assertEquals(testRemoteItem, testRemoteItem);
    }

    @Test
    public void testSubclass_acrossProcess() {
        final MediaMetadata2 testMetadata = new MediaMetadata2.Builder()
                .putString("testSubclass_acrossProcess", "testSubclass_acrossProcess").build();
        final long testStartPosition = 1;
        final long testEndPosition = 100;
        final UriMediaItem2 testUriItem =
                new UriMediaItem2.Builder(mContext, Uri.parse("test://test"))
                        .setMetadata(testMetadata)
                        .setStartPosition(testStartPosition)
                        .setEndPosition(testEndPosition)
                        .build();

        // Mocks the binder call across the processes by using writeParcelable/readParcelable
        // which only happens between processes. Code snippets are copied from
        // VersionedParcelIntegTest#parcelCopy.
        final Parcel p = Parcel.obtain();
        p.writeParcelable(MediaUtils2.toParcelable(testUriItem), 0);
        p.setDataPosition(0);
        final MediaItem2 testRemoteItem = MediaUtils2.fromParcelable(
                (ParcelImpl) p.readParcelable(MediaItem2.class.getClassLoader()));

        assertFalse(testRemoteItem instanceof UriMediaItem2);
        assertEquals(testUriItem.getStartPosition(), testRemoteItem.getStartPosition());
        assertEquals(testUriItem.getEndPosition(), testRemoteItem.getEndPosition());
        TestUtils.equals(testUriItem.getMetadata().toBundle(),
                testRemoteItem.getMetadata().toBundle());
    }
}
