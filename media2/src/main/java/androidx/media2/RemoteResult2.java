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

import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Base class for result classes in {@link MediaSession2} and {@link MediaController2} that may be
 * sent across the processes.
 **/
@VersionedParcelize
class RemoteResult2 extends BaseResult2 implements VersionedParcelable {
    /**
     * Result code represents that the session and controller were disconnected.
     */
    public static final int RESULT_CODE_DISCONNECTED = -100;

    /**
     * Result code represents that the authentication has expired.
     */
    public static final int RESULT_CODE_AUTHENTICATION_EXPIRED = -102;

    /**
     * Result code represents that a premium account is required.
     */
    public static final int RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED = -103;

    /**
     * Result code represents that too many concurrent streams are detected.
     */
    public static final int RESULT_CODE_CONCURRENT_STREAM_LIMIT = -104;

    /**
     * Result code represents that the content is blocked due to parental controls.
     */
    public static final int RESULT_CODE_PARENTAL_CONTROL_RESTRICTED = -105;

    /**
     * Result code represents that the content is blocked due to being regionally unavailable.
     */
    public static final int RESULT_CODE_NOT_AVAILABLE_IN_REGION = -106;

    /**
     * Result code represents that the application cannot skip any more songs because skip limit is
     * reached.
     */
    public static final int RESULT_CODE_SKIP_LIMIT_REACHED = -107;

    /**
     * Result code represents that the session needs user's manual intervention.
     */
    public static final int RESULT_CODE_SETUP_REQUIRED = -108;

    @ParcelField(1)
    int mResultCode;
    @ParcelField(2)
    long mCompletionTime;
    @ParcelField(3)
    MediaItem2 mItem;

    /**
     * Used for VersionedParcelable
     */
    RemoteResult2() {
    }

    RemoteResult2(int resultCode, @Nullable MediaItem2 item) {
        init(resultCode, item, SystemClock.elapsedRealtime());
    }

    RemoteResult2(@Nullable RemoteResult2 result) {
        if (result == null) {
            init(RESULT_CODE_NOT_SUPPORTED, null, SystemClock.elapsedRealtime());
        } else {
            init(result.getResultCode(), result.getMediaItem(), result.getCompletionTime());
        }
    }

    RemoteResult2(SessionPlayer2.PlayerResult playerResult) {
        if (playerResult == null) {
            init(RESULT_CODE_NOT_SUPPORTED, null, SystemClock.elapsedRealtime());
        } else {
            // TODO: Convert undefined result code in playerResult to UNKNOWN_ERROR.
            init(playerResult.getResultCode(), playerResult.getMediaItem(),
                    playerResult.getCompletionTime());
        }
    }

    private void init(int resultCode, @Nullable MediaItem2 item, long completionTime) {
        mResultCode = resultCode;
        mItem = item;
        mCompletionTime = completionTime;
    }

    @Override
    public int getResultCode() {
        return mResultCode;
    }

    @Override
    public long getCompletionTime() {
        return mCompletionTime;
    }

    /**
     * Gets the {@link MediaItem2} for which the command was executed. In other words, this is
     * the current media item when the command is completed.
     * <p>
     * Can be {@code null} for many reasons. For examples,
     * <ul>
     * <li>Error happened.
     * <li>The current media item is {@code null}.
     * <li>The called API is not expected to return a media item.
     * </ul>
     *
     * @return media item when the command is completed. Can be {@code null} for an error, or the
     *         current media item is {@code null}.
     */
    @Override
    public @Nullable MediaItem2 getMediaItem() {
        return mItem;
    }
}
