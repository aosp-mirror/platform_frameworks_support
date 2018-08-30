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

import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * @hide
 */
// TODO(jaewan): Unhide this
// TODO(jaewan): Make it versioned parcelable
// TODO(jaewan): Replace this with MediaItemDesc2, and remove it.
public class MediaItemDesc2 {
    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final long POSITION_UNKNOWN = LONG_MAX;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
    public @interface Flags { }

    /**
     * Flag: Indicates that the item has children of its own.
     * @see #getFlags()
     */
    public static final int FLAG_BROWSABLE = 1 << 0;

    /**
     * Flag: Indicates that the item is playable.
     * <p>
     * The id of this item may be passed to
     * {@link MediaController2#playFromMediaId(String, Bundle)}
     * @see #getFlags()
     */
    public static final int FLAG_PLAYABLE = 1 << 1;

    // TODO(jaewan): Add @ParcelField
    String mId;
    int mFlags;
    ParcelUuid mParcelUuid;
    MediaMetadata2 mMetadata;
    long mStartPositionMs = 0;
    long mEndPositionMs = POSITION_UNKNOWN;

    /**
     * Used for VersionedParcelable
     */
    MediaItemDesc2() {
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
    // Note: It's intentionally protected to allow 3rd party media library to subclass this as well.
    protected MediaItemDesc2(Builder builder) {
        this(builder.mUuid, builder.mMediaId, builder.mFlags, builder.mMetadata,
                builder.mStartPositionMs, builder.mEndPositionMs);
    }

    private MediaItemDesc2(@Nullable UUID uuid, @Nullable String mediaId, int flags,
            @Nullable MediaMetadata2 metadata, long startPositionMs, long endPositionMs) {
        mParcelUuid = new ParcelUuid((uuid != null) ? uuid : UUID.randomUUID());
        if (metadata != null) {
            mId = metadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID);
        }
        mId = mediaId != null ? mId : mediaId;
        mFlags = flags;
        mMetadata = metadata;
        mStartPositionMs = startPositionMs;
        mEndPositionMs = endPositionMs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaItemDesc2{");
        sb.append("mId=").append(mId);
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
     * @see #FLAG_BROWSABLE
     */
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Returns whether this item is playable.
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
        if (metadata != null && !TextUtils.equals(mId, metadata.getMediaId())) {
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
    public @Nullable String getMediaId() {
        return mId;
    }

    /**
     * Return the position in milliseconds at which the playback will start.
     * @return the position in milliseconds at which the playback will start
     */
    public long getStartPosition() {
        return mStartPositionMs;
    }

    /**
     * Return the position in milliseconds at which the playback will end.
     * {@link #POSITION_UNKNOWN} means ending at the end of source content.
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

    UUID getUuid() {
        return mParcelUuid.getUuid();
    }

    /**
     * @hide
     */
    // TODO(jaewan): Replace this with Builder
    @RestrictTo(LIBRARY)
    public static class Builder<T extends Builder> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Flags int mFlags;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        String mMediaId;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        MediaMetadata2 mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        UUID mUuid;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;

        /**
         * Constructor of {@link Builder}
         */
        public Builder(@Flags int flags) {
            mFlags = flags;
        }

        /**
         * Sets the media id of this instance. {@code null} for unset.
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
        public @NonNull T setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return (T) this;
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
        public @NonNull T setMetadata(@Nullable MediaMetadata2 metadata) {
            mMetadata = metadata;
            return (T) this;
        }

        /**
         * Sets the start position in milliseconds at which the playback will start.
         * Any negative number is treated as 0.
         *
         * @param position the start position in milliseconds at which the playback will start
         * @return the same Builder instance.
         */
        public @NonNull T setStartPosition(long position) {
            if (position < 0) {
                position = 0;
            }
            mStartPositionMs = position;
            return (T) this;
        }

        /**
         * Sets the end position in milliseconds at which the playback will end.
         * Any negative number is treated as maximum length of the data source.
         *
         * @param position the end position in milliseconds at which the playback will end
         * @return the same Builder instance.
         */
        public @NonNull T setEndPosition(long position) {
            if (position < 0) {
                position = POSITION_UNKNOWN;
            }
            mEndPositionMs = position;
            return (T) this;
        }

        @NonNull T setUuid(UUID uuid) {
            mUuid = uuid;
            return (T) this;
        }

        /**
         * Builds {@link MediaItemDesc2}.
         *
         * @return a new {@link MediaItemDesc2}.
         */
        public @NonNull MediaItemDesc2 build() {
            return new MediaItemDesc2(this);
        }
    }
}