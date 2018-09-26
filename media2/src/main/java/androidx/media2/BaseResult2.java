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

import androidx.annotation.Nullable;

/**
 * Base class for all result classes in {@link MediaSession2}, {@link MediaController2},
 * and {@link SessionPlayer2}, for defining result codes in one place with documentation.
 **/
abstract class BaseResult2 {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Common codes for {@link MediaSession2}, {@link MediaController2}, and {@link SessionPlayer2}.
    // Ranges
    //     - Error codes: Integer.MIN_VALUE, (-1000, 0)
    //     - Success code: 0
    //     - Info code: (0, 1000]
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Result code represents that call is ended with an unknown error.
     */
    public static final int RESULT_CODE_UNKNOWN_ERROR = Integer.MIN_VALUE;

    /**
     * Result code represents that call is completed without an error.
     */
    public static final int RESULT_CODE_SUCCESS = 0;

    /**
     * Result code represents that the command cannot be completed because of the valid state for
     * the command.
     */
    public static final int RESULT_CODE_INVALID_STATE = -1;

    /**
     * Result code represents that the argument is illegal.
     */
    public static final int RESULT_CODE_BAD_VALUE = -2;

    /**
     * Result code represents that the command is not allowed.
     */
    public static final int RESULT_CODE_PERMISSION_DENIED = -3;

    /**
     * Result code represents a file or network related command error.
     */
    public static final int RESULT_CODE_IO_ERROR = -4;

    /**
     * Result code represents that the command is not supported nor implemented.
     */
    public static final int RESULT_CODE_NOT_SUPPORTED = -5;

    /**
     * Result code represents that the command is skipped or canceled. For an example, a seek
     * command can be skipped if it is followed by another seek command.
     */
    public static final int RESULT_CODE_SKIPPED = 1;

    abstract int getResultCode();

    /**
     * Gets the completion time of the command. Being more specific, it's the same as
     * {@link android.os.SystemClock#elapsedRealtime()} when the command is completed.
     *
     * @return completion time of the command
     */
    public abstract long getCompletionTime();

    public abstract @Nullable MediaItem2 getMediaItem();
}
