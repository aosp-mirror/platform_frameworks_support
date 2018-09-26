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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base class for result classes in {@link MediaSession2}, {@link MediaController2},
 * and {@link SessionPlayer2}, for defining result codes in one place to avoid potential
 * duplications.
 **/
abstract class Result2 {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Common codes for {@link MediaSession2}, {@link MediaController2}, and {@link SessionPlayer2}.
    // Range
    //     - Error codes: Integer.MIN_VALUE, [-1000-0)
    //     - Success code: 0
    //     - Info code:
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Result code represents that call is ended with an unknown error.
     */
    static final int RESULT_CODE_UNKNOWN_ERROR = Integer.MIN_VALUE;

    /**
     * Result code represents that call is completed without an error.
     */
    static final int RESULT_CODE_SUCCESS = 0;

    /**
     * Result code represents that the player is not in valid state for the operation.
     */
    static final int RESULT_CODE_INVALID_STATE = -1;

    /**
     * Result code represents that the argument is illegal.
     */
    static final int RESULT_CODE_BAD_VALUE = -2;

    /**
     * Result code represents that the operation is not allowed.
     */
    static final int RESULT_CODE_PERMISSION_DENIED = -3;

    /**
     * Result code represents a file or network related operation error.
     */
    static final int RESULT_CODE_IO_ERROR = -4;

    /**
     * Result code represents that the player skipped the call. For example, a seek request may be
     * skipped if it is followed by another seek request.
     */
    static final int RESULT_CODE_SKIPPED = 1;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Common codes for {@link MediaSession2} and {@link MediaController2}
    // Range: [10000-Integer.MAX_VALUE)
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Result code represents that the session and controller were disconnected.
     */
    static final int RESULT_CODE_DISCONNECTED = 10000;

    /**
     * Result code represents that the request is not supported by the application.
     */
    static final int RESULT_CODE_NOT_SUPPORTED = 10001;

    /**
     * Result code represents that the authentication has expired.
     */
    static final int RESULT_CODE_AUTHENTICATION_EXPIRED = 10002;

    /**
     * Result code represents that a premium account is required.
     */
    static final int RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED = 10003;

    /**
     * Result code represents that too many concurrent streams are detected.
     */
    static final int RESULT_CODE_CONCURRENT_STREAM_LIMIT = 10004;

    /**
     * Result code represents that the content is blocked due to parental controls.
     */
    static final int RESULT_CODE_PARENTAL_CONTROL_RESTRICTED = 10005;

    /**
     * Result code represents that the content is blocked due to being regionally unavailable.
     */
    static final int RESULT_CODE_NOT_AVAILABLE_IN_REGION = 10006;

    /**
     * Result code represents that the application cannot skip any more songs because skip limit is
     * reached.
     */
    static final int RESULT_CODE_SKIP_LIMIT_REACHED = 10007;

    /**
     * Result code represents that the session needs user's manual intervention.
     */
    static final int RESULT_CODE_SETUP_REQUIRED = 10008;

    /**
     * Stub annotation to check code duplication by compiler.
     *
     * @hide
     */
    @IntDef(flag = false, /*prefix = "RESULT_CODE",*/ value = {
            RESULT_CODE_SUCCESS,
            RESULT_CODE_UNKNOWN_ERROR,
            RESULT_CODE_INVALID_STATE,
            RESULT_CODE_BAD_VALUE,
            RESULT_CODE_PERMISSION_DENIED,
            RESULT_CODE_IO_ERROR,
            RESULT_CODE_SKIPPED,
            RESULT_CODE_DISCONNECTED,
            RESULT_CODE_NOT_SUPPORTED,
            RESULT_CODE_AUTHENTICATION_EXPIRED,
            RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED,
            RESULT_CODE_CONCURRENT_STREAM_LIMIT,
            RESULT_CODE_PARENTAL_CONTROL_RESTRICTED,
            RESULT_CODE_NOT_AVAILABLE_IN_REGION,
            RESULT_CODE_SKIP_LIMIT_REACHED,
            RESULT_CODE_SETUP_REQUIRED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    @interface ResultCode {}

    abstract int getResultCode();

    abstract long getCompletionTime();
}
