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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * Describes a media item.
 *
 * @see SessionPlayer2
 * @see MediaSession2
 * @see MediaController2
 * @hide
 */
// New version of MediaItem, QueueItem, and DataSource.
@RestrictTo(LIBRARY)
@TargetApi(Build.VERSION_CODES.P)
@VersionedParcelize
public class MediaItemDesc2 implements VersionedParcelable {
    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {FLAG_BROWSABLE, FLAG_PLAYABLE})
    public @interface Flags {
    }

    /**
     * Flag: Indicates that the item has children of its own.
     */
    public static final int FLAG_BROWSABLE = 1 << 0;

    /**
     * Flag: Indicates that the item is playable.
     * <p>
     * The id of this item may be passed to
     * {@link MediaController2#playFromMediaId(String, Bundle)}
     */
    public static final int FLAG_PLAYABLE = 1 << 1;

    @ParcelField(1)
    ParcelUuid mParcelUuid;
    @ParcelField(2)
    String mMediaId;
    @ParcelField(3)
    int mFlags;
    @ParcelField(4)
    MediaMetadata2 mMetadata;
    @ParcelField(5)
    long mStartPositionMs = 0;
    @ParcelField(6)
    long mEndPositionMs = POSITION_UNKNOWN;

    /**
     * Used for VersionedParcelable
     */
    MediaItemDesc2() {
        // no-op
    }

    /**
     * Copy constructor that resets the UUID.
     */
    MediaItemDesc2(@NonNull MediaItemDesc2 desc) {
        this(/* uuid */ null, desc.getMediaId(), desc.getFlags(), desc.getMetadata(),
                desc.getStartPosition(), desc.getEndPosition());
    }

    /**
     * Used by {@link Builder} and its subclasses
     */
    MediaItemDesc2(Builder builder) {
        this(builder.mUuid, builder.mMediaId, builder.mFlags, builder.mMetadata,
                builder.mStartPositionMs, builder.mEndPositionMs);
    }

    private MediaItemDesc2(@Nullable UUID uuid, @Nullable String mediaId, int flags,
            @Nullable MediaMetadata2 metadata, long startPositionMs, long endPositionMs) {
        mParcelUuid = new ParcelUuid((uuid != null) ? uuid : UUID.randomUUID());
        if (metadata != null) {
            mMediaId = metadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID);
        }
        mMediaId = mediaId != null ? mMediaId : mediaId;
        mFlags = flags;
        mMetadata = metadata;
        mStartPositionMs = startPositionMs;
        mEndPositionMs = endPositionMs;
    }

    @Override
    public @NonNull String toString() {
        final StringBuilder sb = new StringBuilder("MediaItemDesc2{");
        sb.append("mMediaId=").append(mMediaId);
        sb.append(", mFlags=").append(mFlags);
        sb.append(", mMetadata=").append(mMetadata);
        sb.append(", mStartPositionMs=").append(mStartPositionMs);
        sb.append(", mEndPositionMs=").append(mEndPositionMs);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Gets the flags of the item.
     */
    public @Flags int getFlags() {
        return mFlags;
    }

    /**
     * Returns whether this item is browsable.
     *
     * @see #FLAG_BROWSABLE
     */
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Returns whether this item is playable.
     *
     * @see #FLAG_PLAYABLE
     */
    public boolean isPlayable() {
        return (mFlags & FLAG_PLAYABLE) != 0;
    }

    /**
     * Set a metadata. If the metadata is not null, its id should be matched with this instance's
     * media id.
     *
     * @param metadata metadata to update
     */
    public void setMetadata(@Nullable MediaMetadata2 metadata) {
        if (metadata != null && !TextUtils.equals(mMediaId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaId");
        }
        mMetadata = metadata;
    }

    /**
     * Returns the metadata of the media.
     *
     * @return metadata from the session
     */
    public @Nullable MediaMetadata2 getMetadata() {
        return mMetadata;
    }

    /**
     * Returns the media id for this item. If it's not {@code null}, it's a persistent unique key
     * for the underlying media content.
     *
     * @return media Id from the session
     */
    public @Nullable
    String getMediaId() {
        return mMediaId;
    }

    /**
     * Return the position in milliseconds at which the playback will start.
     *
     * @return the position in milliseconds at which the playback will start
     */
    public long getStartPosition() {
        return mStartPositionMs;
    }

    /**
     * Return the position in milliseconds at which the playback will end.
     * {@link #POSITION_UNKNOWN} means ending at the end of source content.
     *
     * @return the position in milliseconds at which the playback will end
     */
    public long getEndPosition() {
        return mEndPositionMs;
    }

    @Override
    public int hashCode() {
        return mParcelUuid.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MediaItemDesc2)) {
            return false;
        }
        MediaItemDesc2 other = (MediaItemDesc2) obj;
        return mParcelUuid.equals(other.mParcelUuid);
    }

    /**
     * Gets the UUID of this object which is used to check object equality across the processes.
     *
     * @return UUID.
     */
    @NonNull UUID getUuid() {
        return mParcelUuid.getUuid();
    }

    /**
     * Builder class for {@link MediaItemDesc2} objects.
     *
     * @param <T> The Builder of the derived classe.
     */
    public static final class Builder<T extends MediaItemDesc2> {
        @Flags int mFlags;
        String mMediaId;
        MediaMetadata2 mMetadata;
        UUID mUuid;
        long mStartPositionMs = 0;
        long mEndPositionMs = POSITION_UNKNOWN;

        /**
         * Constructor for {@link MediaItemDesc2.Builder}
         */
        public Builder(@Flags int flags) {
            mFlags = flags;
        }

        /**
         * Set the media id of this instance. {@code null} for unset.
         * <p>
         * If used, this should be a persistent unique key for the underlying content so session
         * and controller can uniquely identify a media content.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead.
         *
         * @param mediaId media id
         * @return this instance for chaining
         */
        public @NonNull Builder<T> setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return this;
        }

        /**
         * Set the metadata of this instance. {@code null} for unset.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead.
         *
         * @param metadata metadata
         * @return this instance for chaining
         */
        public @NonNull Builder<T> setMetadata(@Nullable MediaMetadata2 metadata) {
            mMetadata = metadata;
            return this;
        }

        /**
         * Sets the start position in milliseconds at which the playback will start.
         * Any negative number is treated as 0.
         *
         * @param position the start position in milliseconds at which the playback will start
         * @return the same Builder instance.
         */
        public @NonNull Builder<T> setStartPosition(long position) {
            if (position < 0) {
                position = 0;
            }
            mStartPositionMs = position;
            return this;
        }

        /**
         * Sets the end position in milliseconds at which the playback will end.
         * Any negative number is treated as maximum length of the data source.
         *
         * @param position the end position in milliseconds at which the playback will end
         * @return the same Builder instance.
         */
        public @NonNull Builder<T> setEndPosition(long position) {
            if (position < 0) {
                position = POSITION_UNKNOWN;
            }
            mEndPositionMs = position;
            return this;
        }

        Builder<T> setUuid(UUID uuid) {
            mUuid = uuid;
            return this;
        }

        /**
         * Build {@link MediaItemDesc2}.
         *
         * @return a new {@link MediaItemDesc2}.
         */
        public @NonNull T build() {
            return (T) new MediaItemDesc2(this);
        }
    }
}