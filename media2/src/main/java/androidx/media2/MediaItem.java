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

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Pair;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A class with information on a single media item with the metadata information. Here are use
 * cases.
 * <ul>
 * <li>Specify media items to {@link SessionPlayer} for playback.
 * <li>Share media items across the processes.
 * </ul>
 * <p>
 * Subclasses of the session player may only accept the media items which are created by
 * {@link Builder}. Check the player documentation that you're interested in.
 * <p>
 * When it's shared across the processes, we cannot guarantee that they contain the right values
 * because media items are application dependent especially for the metadata.
 * <p>
 * When an object of the {@link MediaItem}'s subclass is sent across the process between
 * {@link MediaSession}/{@link MediaController} or
 * {@link androidx.media2.MediaLibraryService.MediaLibrarySession}/{@link MediaBrowser}, the
 * object will sent as if it's {@link MediaItem}. The recipient cannot get the object with the
 * subclasses' type. This will sanitize process specific information.
 * <p>
 * This object is thread safe.
 */
@VersionedParcelize(isCustom = true)
public class MediaItem extends CustomVersionedParcelable {
    private static final String TAG = "MediaItem";

    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    @NonParcelField
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    @ParcelField(1)
    MediaMetadata mMetadata;
    @ParcelField(2)
    long mStartPositionMs = 0;
    @ParcelField(3)
    long mEndPositionMs = POSITION_UNKNOWN;

    @GuardedBy("mLock")
    @NonParcelField
    private final List<Pair<OnMetadataChangedListener, Executor>> mListeners = new ArrayList<>();

    /**
     * Used for VersionedParcelable
     */
    MediaItem() {
    }

    /**
     * Used by {@link MediaItem.Builder} and its subclasses
     */
    // Note: Needs to be protected when we want to allow 3rd party player to define customized
    //       MediaItem.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem(Builder builder) {
        this(builder.mMetadata, builder.mStartPositionMs, builder.mEndPositionMs);
    }

    MediaItem(MediaItem item) {
        this(item.mMetadata, item.mStartPositionMs, item.mEndPositionMs);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem(@Nullable MediaMetadata metadata, long startPositionMs, long endPositionMs) {
        if (startPositionMs > endPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + startPositionMs + " : " + endPositionMs);
        }
        if (metadata != null && metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
            long durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (durationMs != SessionPlayer.UNKNOWN_TIME && endPositionMs != POSITION_UNKNOWN
                    && endPositionMs > durationMs) {
                throw new IllegalStateException("endPositionMs shouldn't be greater than"
                        + " duration in the metdata, endPositionMs=" + endPositionMs
                        + ", durationMs=" + durationMs);
            }
        }
        mMetadata = metadata;
        mStartPositionMs = startPositionMs;
        mEndPositionMs = endPositionMs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        synchronized (mLock) {
            sb.append("{mMetadata=").append(mMetadata);
            sb.append(", mStartPositionMs=").append(mStartPositionMs);
            sb.append(", mEndPositionMs=").append(mEndPositionMs);
            sb.append('}');
        }
        return sb.toString();
    }

    /**
     * Sets metadata. If the metadata is not {@code null}, its id should be matched with this
     * instance's media id.
     *
     * @param metadata metadata to update
     * @see MediaMetadata#METADATA_KEY_MEDIA_ID
     */
    public void setMetadata(@Nullable MediaMetadata metadata) {
        List<Pair<OnMetadataChangedListener, Executor>> listeners = new ArrayList<>();
        synchronized (mLock) {
            if (mMetadata != null && metadata != null
                    && !TextUtils.equals(getMediaId(), metadata.getMediaId())) {
                Log.d(TAG, "MediaItem's media ID shouldn't be changed");
                return;
            }
            mMetadata = metadata;
            listeners.addAll(mListeners);
        }

        for (Pair<OnMetadataChangedListener, Executor> pair : listeners) {
            final OnMetadataChangedListener listener = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onMetadataChanged(MediaItem.this);
                }
            });
        }
    }

    /**
     * Gets the metadata of the media.
     *
     * @return metadata from the session
     */
    public @Nullable MediaMetadata getMetadata() {
        synchronized (mLock) {
            return mMetadata;
        }
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

    /**
     * Gets the media id for this item. If it's not {@code null}, it's a persistent unique key
     * for the underlying media content.
     *
     * @return media Id from the session
     * @hide
     */
    // TODO: Remove
    @RestrictTo(LIBRARY_GROUP)
    public @Nullable String getMediaId() {
        synchronized (mLock) {
            return mMetadata != null
                    ? mMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) : null;
        }
    }

    void addOnMetadataChangedListener(Executor executor, OnMetadataChangedListener listener) {
        synchronized (mLock) {
            for (Pair<OnMetadataChangedListener, Executor> pair : mListeners) {
                if (pair.first == listener) {
                    return;
                }
            }
            mListeners.add(new Pair<>(listener, executor));
        }
    }

    void removeOnMetadataChangedListener(OnMetadataChangedListener listener) {
        synchronized (mLock) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                if (mListeners.get(i).first == listener) {
                    mListeners.remove(i);
                    return;
                }
            }
        }
    }

    /**
     * Builder for {@link MediaItem}.
     */
    public static class Builder {
        private static final int SOURCE_TYPE_UNKNOWN = 0;
        private static final int SOURCE_TYPE_URI = 1;
        private static final int SOURCE_TYPE_FILE = 2;
        private static final int SOURCE_TYPE_CALLBACK = 3;

        private int mSourceType = SOURCE_TYPE_UNKNOWN;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        MediaMetadata mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;

        // For UriMediaItem
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Uri mUri;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Map<String, String> mHeader;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        List<HttpCookie> mCookies;

        // For FileMediaItem
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        ParcelFileDescriptor mPFD;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mOffset = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mLength = FileMediaItem.FD_LENGTH_UNKNOWN;

        // For CallbackMediaItem
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        DataSourceCallback mDataSourceCallback;

        /**
         * Default constructor
         */
        public Builder() {
        }

        /**
         * Set the metadata of this instance. {@code null} for unset.
         *
         * @param metadata metadata
         * @return this instance for chaining
         */
        @NonNull
        public Builder setMetadata(@Nullable MediaMetadata metadata) {
            mMetadata = metadata;
            return this;
        }

        /**
         * Sets the start position in milliseconds at which the playback will start.
         * Any negative number is treated as 0.
         *
         * @param position the start position in milliseconds at which the playback will start
         * @return this instance for chaining
         */
        @NonNull
        public Builder setStartPosition(long position) {
            if (position < 0) {
                position = 0;
            }
            mStartPositionMs = position;
            return this;
        }

        /**
         * Sets the end position in milliseconds at which the playback will end.
         * Any negative number is treated as maximum length of the media item.
         *
         * @param position the end position in milliseconds at which the playback will end
         * @return this instance for chaining
         */
        @NonNull
        public Builder setEndPosition(long position) {
            if (position < 0) {
                position = POSITION_UNKNOWN;
            }
            mEndPositionMs = position;
            return this;
        }

        /**
         * Sets the media source as a content Uri.
         *
         * @param uri the Content URI of the data you want to play
         * @return the same Builder instance.
         * @throws NullPointerException if context or uri is null.
         */
        @NonNull
        public Builder setMediaSource(@NonNull Uri uri) {
            if (uri == null) {
                throw new NullPointerException("uri cannot be null");
            }
            setSourceType(SOURCE_TYPE_URI);
            mUri = uri;
            return this;
        }

        /**
         * Sets the data source as a content Uri.
         *
         * To provide cookies for the subsequent HTTP requests, you can install your own default
         * cookie handler and use other variants of setMediaSource APIs instead. Alternatively, you
         * can use this API to pass the cookies as a list of HttpCookie. If the app has not
         * installed a CookieHandler already, The implementation of {@link SessionPlayer} would
         * create a CookieManager and populates its CookieStore with the provided cookies when this
         * data source is passed to {@link SessionPlayer}. If the app has installed its own handler
         * already, the handler is required to be of CookieManager type such that
         * {@link SessionPlayer} can update the manager’s CookieStore.
         *
         *  <p><strong>Note</strong> that the cross domain redirection is allowed by default,
         * but that can be changed with key/value pairs through the headers parameter with
         * "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value to
         * disallow or allow cross domain redirection.
         *
         * @param uri the Content URI of the data you want to play
         * @param headers the headers to be sent together with the request for the data
         *                The headers must not include cookies. Instead, use the cookies param.
         * @param cookies the cookies to be sent together with the request
         * @return the same Builder instance.
         * @throws NullPointerException if context or uri is null.
         * @throws IllegalArgumentException if the cookie handler is not of CookieManager type
         *                                  when cookies are provided.
         */
        @NonNull
        public Builder setMediaSource(@NonNull Uri uri, @Nullable Map<String, String> headers,
                @Nullable List<HttpCookie> cookies) {
            if (uri == null) {
                throw new NullPointerException("uri cannot be null");
            }
            if (cookies != null) {
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (cookieHandler != null && !(cookieHandler instanceof CookieManager)) {
                    throw new IllegalArgumentException(
                            "The cookie handler has to be of CookieManager type "
                                    + "when cookies are provided.");
                }
            }
            setSourceType(SOURCE_TYPE_URI);
            mUri = uri;
            if (headers != null) {
                mHeader = new HashMap<String, String>(headers);
            }
            if (cookies != null) {
                mCookies = new ArrayList<HttpCookie>(cookies);
            }
            return this;
        }

        /**
         * Sets the data source (ParcelFileDescriptor) to use. The ParcelFileDescriptor must be
         * seekable (N.B. a LocalSocket is not seekable). When the {@link MediaItem} created with
         * this method is passed to {@link SessionPlayer} via {@link SessionPlayer#setMediaItem}
         * or {@link SessionPlayer#setPlaylist}, the implementations of Sessionplayer would close
         * the ParcelFileDescriptor.
         *
         * @param pfd the ParcelFileDescriptor for the file to play
         * @return the same Builder instance.
         * @throws NullPointerException if pfd is null.
         */
        @NonNull
        public Builder setMediaSource(@NonNull ParcelFileDescriptor pfd) {
            if (pfd == null) {
                throw new NullPointerException("pfd cannot be null");
            }
            setSourceType(SOURCE_TYPE_FILE);
            mPFD = pfd;
            return this;
        }

        /**
         * Sets the data source (ParcelFileDescriptor) to use. The ParcelFileDescriptor must be
         * seekable (N.B. a LocalSocket is not seekable). When the {@link MediaItem} created with
         * this method is passed to {@link SessionPlayer} via {@link SessionPlayer#setMediaItem}
         * or {@link SessionPlayer#setPlaylist}, the implementations of Sessionplayer would close
         * the ParcelFileDescriptor.
         *
         * Any negative number for offset is treated as 0.
         * Any negative number for length is treated as maximum length of the data source.
         *
         * @param pfd the ParcelFileDescriptor for the file to play
         * @param offset the offset into the file where the data to be played starts, in bytes
         * @param length the length in bytes of the data to be played
         * @return the same Builder instance.
         * @throws NullPointerException if pfd is null.
         */
        @NonNull
        public Builder setMediaSource(
                @NonNull ParcelFileDescriptor pfd, long offset, long length) {
            if (pfd == null) {
                throw new NullPointerException("pfd cannot be null");
            }
            setSourceType(SOURCE_TYPE_FILE);
            if (offset < 0) {
                offset = 0;
            }
            if (length < 0) {
                length = FileMediaItem.FD_LENGTH_UNKNOWN;
            }
            mPFD = pfd;
            mOffset = offset;
            mLength = length;
            return this;
        }

        /**
         * Sets the data source (DataSourceCallback) to use.
         *
         * @param dscb the DataSourceCallback for the media to play
         * @return the same Builder instance.
         * @throws NullPointerException if dscb is null.
         */
        public @NonNull Builder setMediaSource(@NonNull DataSourceCallback dscb) {
            if (dscb == null) {
                throw new NullPointerException("dscb cannot be null");
            }
            setSourceType(SOURCE_TYPE_CALLBACK);
            mDataSourceCallback = dscb;
            return this;
        }

        private void setSourceType(int type) {
            if (mSourceType != SOURCE_TYPE_UNKNOWN) {
                throw new IllegalStateException("Source is already set. type=" + mSourceType);
            }
            mSourceType = type;
        }

        /**
         * Build {@link MediaItem}.
         *
         * @return a new {@link MediaItem}.
         */
        @NonNull
        public MediaItem build() {
            if (mStartPositionMs > mEndPositionMs) {
                throw new IllegalStateException("Illegal start/end position: "
                        + mStartPositionMs + " : " + mEndPositionMs);
            }

            switch (mSourceType) {
                case SOURCE_TYPE_CALLBACK:
                    return new CallbackMediaItem(this);
                case SOURCE_TYPE_FILE:
                    return new FileMediaItem(this);
                case SOURCE_TYPE_URI:
                    return new UriMediaItem(this);
                default:
                    return new MediaItem(this);
            }
        }
    }

    interface OnMetadataChangedListener {
        void onMetadataChanged(MediaItem item);
    }

    /**
     * @hide
     * @param isStream
     */
    @RestrictTo(LIBRARY)
    @Override
    public void onPreParceling(boolean isStream) {
        if (getClass() != MediaItem.class) {
            throw new RuntimeException("MediaItem's subclasses shouldn't be parcelized.");
        }
        super.onPreParceling(isStream);
    }
}
