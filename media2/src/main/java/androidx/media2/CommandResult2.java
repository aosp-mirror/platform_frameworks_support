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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Result of the asynchrnous APIs
 * <p>
 * This class doesn't define error code itself. Check the returned class for the individual error
 * codes.
 */
// Similar to 'status_t' in C/C++
@TargetApi(Build.VERSION_CODES.P)
@VersionedParcelize
public final class CommandResult2 implements VersionedParcelable {
    @ParcelField(1)
    int mResultCode;
    @ParcelField(2)
    long mCompletionTime;
    @ParcelField(3)
    MediaItem2 mItem;

    /**
     * Used for VersionedParcelable
     */
    CommandResult2() {
    }

    public CommandResult2(int resultCode, MediaItem2 item) {
        this(resultCode, item, SystemClock.elapsedRealtime());
    }

    public CommandResult2(int resultCode, MediaItem2 item, long completionTime) {
        mResultCode = resultCode;
        mCompletionTime = completionTime;
        mItem = item;
    }

    /**
     * Gets the result code.
     * <p>
     * Check the class documentation that has returned this {@link CommandResult2} to understand the
     * meaning of the code value.
     *
     * @return result code
     */
    public final int getResultCode() {
        return mResultCode;
    }

    /**
     * Gets the completion time of the command.
     *
     * @return completion time
     */
    public final long getCompletionTime() {
        return mCompletionTime;
    }

    /**
     * Gets the {@link MediaItem2} for which the command was executed. In other words, this is the
     * current media item when the command is completed.
     *
     * @return media item
     */
    public @Nullable MediaItem2 getMediaItem() {
        return mItem;
    }
}
