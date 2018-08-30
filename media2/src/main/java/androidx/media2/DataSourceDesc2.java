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
 * Structure for data source descriptor. Used by {@link MediaItem2}.
 *
 * @see MediaItem2
 */
// TODO(jaewan): Rename this to the MediaItem2 after removing current MediaItem2
// TODO(jaewan): Make it versioned parcelable
public abstract class DataSourceDesc2 {

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
    @IntDef(flag = true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
    public @interface Flags { }

    /**
     * Flag: Indicates that the item has children of its own.
     * @see #getFlags()
     * @hide
     */
    // TODO(jaewan): Unhide this
    public static final int FLAG_BROWSABLE = 1 << 0;

    /**
     * Flag: Indicates that the item is playable.
     * <p>
     * The id of this item may be passed to
     * {@link MediaController2#playFromMediaId(String, Bundle)}
     * @see #getFlags()
     * @hide
     */
    // TODO(jaewan): Unhide this
    public static final int FLAG_PLAYABLE = 1 << 1;

    // TODO(jaewan): Add @ParcelField
    int mFlags;
    ParcelUuid mParcelUuid;
    MediaMetadata2 mMetadata;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    String mMediaId;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mStartPositionMs = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mEndPositionMs = POSITION_UNKNOWN;

    /**
     * Used by {@link Builder} and its subclasses
     */
    // TODO(jaewan): Change it protected
    // This needs to be protected to allow 3rd party media library to subclass this as well.
    DataSourceDesc2(Builder builder) {
        this(builder.mUuid, builder.mMediaId, builder.mFlags, builder.mMetadata,
                builder.mStartPositionMs, builder.mEndPositionMs);
    }

    DataSourceDesc2(@Nullable UUID uuid, @Nullable String mediaId, int flags,
            @Nullable MediaMetadata2 metadata, long startPositionMs, long endPositionMs) {
        if (startPositionMs > endPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + startPositionMs + " : " + endPositionMs);
        }

        mParcelUuid = new ParcelUuid((uuid != null) ? uuid : UUID.randomUUID());
        if (metadata != null) {
            mMediaId = metadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID);
        }
        mMediaId = mMediaId != null ? mMediaId : mediaId;
        mFlags = flags;
        mMetadata = metadata;
        mStartPositionMs = startPositionMs;
        mEndPositionMs = endPositionMs;
    }

    /**
     * Gets the flags of the item.
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public @Flags int getFlags() {
        return mFlags;
    }

    /**
     * Returns whether this item is browsable.
     * @see #FLAG_BROWSABLE
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Returns whether this item is playable.
     * @see #FLAG_PLAYABLE
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public boolean isPlayable() {
        return (mFlags & FLAG_PLAYABLE) != 0;
    }

    /**
     * Set a metadata. If the metadata is not null, its id should be matched with this instance's
     * media id.
     *
     * @param metadata metadata to update
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
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
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public @Nullable MediaMetadata2 getMetadata() {
        return mMetadata;
    }

    /**
     * Return the media Id of data source.
     * @return the media Id of data source
     */
    public @Nullable String getMediaId() {
        return mMediaId;
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
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataSourceDesc2{");
        sb.append("mMediaId=").append(mMediaId);
        sb.append(", mFlags=").append(mFlags);
        sb.append(", mMetadata=").append(mMetadata);
        sb.append(", mStartPositionMs=").append(mStartPositionMs);
        sb.append(", mEndPositionMs=").append(mEndPositionMs);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return mParcelUuid.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof DataSourceDesc2)) {
            return false;
        }
        DataSourceDesc2 other = (DataSourceDesc2) obj;
        return mParcelUuid.equals(other.mParcelUuid);
    }

    UUID getUuid() {
        return mParcelUuid.getUuid();
    }

    /**
     * Builder class for {@link DataSourceDesc2} objects.
     *
     * @param <T> The Builder of the derived classe.
     */
    public abstract static class Builder<T extends Builder> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Flags int mFlags;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        MediaMetadata2 mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        UUID mUuid;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        String mMediaId;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;

        /**
         * Constructs a new Builder with the defaults.
         */
        Builder() {
            this(FLAG_PLAYABLE);
        }

        Builder(@Flags int flags) {
            mFlags = flags;
        }

        /**
         * Constructs a new Builder from a given {@link DataSourceDesc2} instance
         * @param dsd the {@link DataSourceDesc2} object whose data will be reused
         * in the new Builder.
         */
        Builder(@NonNull DataSourceDesc2 dsd) {
            mMediaId = dsd.mMediaId;
            mStartPositionMs = dsd.mStartPositionMs;
            mEndPositionMs = dsd.mEndPositionMs;
        }

        /**
         * Sets the media Id of this data source.
         *
         * @param mediaId the media Id of this data source
         * @return the same Builder instance.
         */
        public @NonNull T setMediaId(String mediaId) {
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
         * @hide
         */
        // TODO(jaewan): Unhide this
        @RestrictTo(LIBRARY)
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
         *
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
    }
}
