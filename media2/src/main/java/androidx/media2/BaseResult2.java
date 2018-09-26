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
 * <p>
 * Error code: Negative integer
 * Success code: 0
 * Info code: Positive integer
 * <p>
 *    0 <  |code| <  100: Session player specific code.
 *  100 <= |code| < 1000: Session/Controller/Browser/Library session specific code.
 * 1000 <= |code|       : Custom session player result code.
 */
abstract class BaseResult2 {
    /**
     * Result code representing that the command is successfully completed.
     */
    public static final int RESULT_CODE_SUCCESS = 0;

    /**
     * Result code represents that call is ended with an unknown error.
     */
    public static final int RESULT_CODE_UNKNOWN_ERROR = -1;

    /**
     * Result code representing that the command cannot be completed because the current state is
     * not valid for the command.
     */
    public static final int RESULT_CODE_INVALID_STATE = -2;

    /**
     * Result code representing that an argument is illegal.
     */
    public static final int RESULT_CODE_BAD_VALUE = -3;

    /**
     * Result code representing that the command is not allowed.
     */
    public static final int RESULT_CODE_PERMISSION_DENIED = -4;

    /**
     * Result code representing a file or network related command error.
     */
    public static final int RESULT_CODE_IO_ERROR = -5;

    /**
     * Result code representing that the command is not supported nor implemented.
     */
    public static final int RESULT_CODE_NOT_SUPPORTED = -6;

    /**
     * Result code representing that the command is skipped or canceled. For an example, a seek
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
