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

import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
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
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Common codes for {@link MediaSession2} and {@link MediaController2}
    // Ranges
    //     - Error codes: (Integer.MIN_VALUE, -10000)
    //     - Info code: (10000, Integer.MAX_VALUE)
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Result code represents that the session and controller were disconnected.
     */
    public static final int RESULT_CODE_DISCONNECTED = -10000;

    /**
     * Result code represents that the authentication has expired.
     */
    public static final int RESULT_CODE_AUTHENTICATION_EXPIRED = -10002;

    /**
     * Result code represents that a premium account is required.
     */
    public static final int RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED = -10003;

    /**
     * Result code represents that too many concurrent streams are detected.
     */
    public static final int RESULT_CODE_CONCURRENT_STREAM_LIMIT = -10004;

    /**
     * Result code represents that the content is blocked due to parental controls.
     */
    public static final int RESULT_CODE_PARENTAL_CONTROL_RESTRICTED = -10005;

    /**
     * Result code represents that the content is blocked due to being regionally unavailable.
     */
    public static final int RESULT_CODE_NOT_AVAILABLE_IN_REGION = -10006;

    /**
     * Result code represents that the application cannot skip any more songs because skip limit is
     * reached.
     */
    public static final int RESULT_CODE_SKIP_LIMIT_REACHED = -10007;

    /**
     * Result code represents that the session needs user's manual intervention.
     */
    public static final int RESULT_CODE_SETUP_REQUIRED = -10008;

    @ParcelField(1)
    int mResultCode;
    @ParcelField(2)
    long mCompletionTime;
    @ParcelField(3)
    MediaItem2 mItem;
    @ParcelField(4)
    Bundle mExtra;

    /**
     * Used for VersionedParcelable
     */
    RemoteResult2() {
    }

    RemoteResult2(int resultCode, @Nullable MediaItem2 item, @Nullable Bundle extra) {
        init(resultCode, item, extra, SystemClock.elapsedRealtime());
    }

    RemoteResult2(@NonNull RemoteResult2 result) {
        if (result == null) {
            init(RESULT_CODE_NOT_SUPPORTED, null, null, SystemClock.elapsedRealtime());
        } else {
            init(result.getResultCode(), result.getMediaItem(), result.getExtra(),
                    result.getCompletionTime());
        }
    }

    RemoteResult2(SessionPlayer2.PlayerResult playerResult) {
        if (playerResult == null) {
            init(RESULT_CODE_NOT_SUPPORTED, null, null, SystemClock.elapsedRealtime());
        } else {
            // TODO: Convert undefined result code in playerResult to UNKNOWN_ERROR.
            init(playerResult.getResultCode(), playerResult.getMediaItem(), null,
                    playerResult.getCompletionTime());
        }
    }

    private void init(int resultCode, @Nullable MediaItem2 item, @Nullable Bundle extra,
            long completionTime) {
        mResultCode = resultCode;
        mItem = item;
        mExtra = extra;
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
     * <li>Error happened
     * <li>The current media item is {@code null}
     * <li>Not a player related API that a media item isn't involved.</li>
     * </ul>
     *
     * @return media item when the command is completed. Can be {@code null} for an error, or the
     *         current media item is {@code null}.
     */
    @Override
    public @Nullable MediaItem2 getMediaItem() {
        return mItem;
    }

    public @Nullable Bundle getExtra() {
        return mExtra;
    }
}
