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

package androidx.media.test.client;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.core.util.ObjectsCompat;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for tests.
 */
public final class MediaTestUtils {

    /**
     * Compares contents of two bundles.
     *
     * @param a a bundle
     * @param b another bundle
     * @return {@code true} if two bundles are the same. {@code false} otherwise. This may be
     *     incorrect if any bundle contains a bundle.
     */
    public static boolean equals(Bundle a, Bundle b) {
        return contains(a, b) && contains(b, a);
    }

    /**
     * Checks whether a Bundle contains another bundle.
     *
     * @param a a bundle
     * @param b another bundle
     * @return {@code true} if a contains b. {@code false} otherwise. This may be incorrect if any
     *      bundle contains a bundle.
     */
    public static boolean contains(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return b == null;
        }
        if (!a.keySet().containsAll(b.keySet())) {
            return false;
        }
        for (String key : b.keySet()) {
            if (!ObjectsCompat.equals(a.get(key), b.get(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a playlist for testing purpose
     * <p>
     * Caller's method name will be used for prefix of each media item's media id.
     *
     * @param size list size
     * @return the newly created playlist
     */
    public static List<MediaItem2> createPlaylist(int size) {
        final List<MediaItem2> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[1].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(new FileMediaItem2.Builder(new FileDescriptor())
                    .setMediaId(caller + "_item_" + (size + 1))
                    .build());
        }
        return list;
    }

    /**
     * Create a media item with the metadata for testing purpose.
     *
     * @return the newly created media item
     * @see #createMetadata()
     */
    public static MediaItem2 createMediaItemWithMetadata() {
        return new FileMediaItem2.Builder(new FileDescriptor())
                .setMetadata(createMetadata())
                .build();
    }

    /**
     * Create a media metadata for testing purpose.
     * <p>
     * Caller's method name will be used for the media id.
     *
     * @return the newly created media item
     */
    public static MediaMetadata2 createMetadata() {
        String mediaId = Thread.currentThread().getStackTrace()[1].getMethodName();
        return new MediaMetadata2.Builder()
                .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId).build();
    }

    public static ArrayList<Parcelable> playlistToParcelableArrayList(List<MediaItem2> playlist) {
        if (playlist == null) {
            return null;
        }
        ArrayList<Parcelable> result = new ArrayList<>();
        for (MediaItem2 item : playlist) {
            result.add(item.toBundle());
        }
        return result;
    }

    public static List<MediaItem2> playlistFromParcelableList(List<Parcelable> parcelables) {
        if (parcelables == null) {
            return null;
        }
        List<MediaItem2> result = new ArrayList<>();
        for (Parcelable item : parcelables) {
            result.add(MediaItem2.fromBundle((Bundle) item));
        }
        return result;
    }

    public static List<Bundle> mediaItem2ListToBundleList(List<MediaItem2> list) {
        if (list == null) {
            return null;
        }
        List<Bundle> result = new ArrayList<>();
        for (MediaItem2 item : list) {
            result.add(item.toBundle());
        }
        return result;
    }

    public static List<MediaItem2> mediaItem2ListFromBundleList(List<Bundle> list) {
        if (list == null) {
            return null;
        }
        List<MediaItem2> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(MediaItem2.fromBundle(list.get(i)));
        }
        return result;
    }
}
