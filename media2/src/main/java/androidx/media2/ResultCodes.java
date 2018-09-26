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

/**
 * Defines the result codes for {@link MediaSession2}, {@link MediaController2},
 * and {@link SessionPlayer2} in one place, to avoid potential duplications.
 **/
class ResultCodes {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Common codes for {@link MediaSession2}, {@link MediaController2}, and {@link SessionPlayer2}.
    // Range: Integer.MIN_VALUE, [0-1000)
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Result code represents that call is ended with an unknown error.
     */
    public static final int RESULT_CODE_ERROR_UNKNOWN = Integer.MIN_VALUE;

    /**
     * Result code represents that call is completed without an error.
     */
    public static final int RESULT_CODE_NO_ERROR = 0;

    /**
     * Result code represents that the player is not in valid state for the operation.
     */
    public static final int RESULT_CODE_INVALID_OPERATION = 1;

    /**
     * Result code represents that the argument is illegal.
     */
    public static final int RESULT_CODE_BAD_VALUE = 2;

    /**
     * Result code represents that the operation is not allowed.
     */
    public static final int RESULT_CODE_PERMISSION_DENIED = 3;

    /**
     * Result code represents a file or network related operation error.
     */
    public static final int RESULT_CODE_ERROR_IO = 4;

    /**
     * Result code represents that the player skipped the call. For example, a seek request may be
     * skipped if it is followed by another seek request.
     */
    public static final int RESULT_CODE_SKIPPED = 5;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Common codes for {@link MediaSession2} and {@link MediaController2}
    // Range: [10000-Integer.MAX_VALUE)
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Result code represents that the session and controller were disconnected.
     */
    public static final int RESULT_CODE_DISCONNECTED = 10000;

    /**
     * Result code represents that the request is not supported by the application.
     */
    public static final int RESULT_CODE_NOT_SUPPORTED = 10001;

    /**
     * Result code represents that the authentication has expired.
     */
    public static final int RESULT_CODE_AUTHENTICATION_EXPIRED = 10002;

    /**
     * Result code represents that a premium account is required.
     */
    public static final int RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED = 10003;

    /**
     * Result code represents that too many concurrent streams are detected.
     */
    public static final int RESULT_CODE_CONCURRENT_STREAM_LIMIT = 10004;

    /**
     * Result code represents that the content is blocked due to parental controls.
     */
    public static final int RESULT_CODE_PARENTAL_CONTROL_RESTRICTED = 10005;

    /**
     * Result code represents that the content is blocked due to being regionally unavailable.
     */
    public static final int RESULT_CODE_NOT_AVAILABLE_IN_REGION = 10006;

    /**
     * Result code represents that the requested content is already playing.
     */
    public static final int RESULT_CODE_CONTENT_ALREADY_PLAYING = 10007;

    /**
     * Result code represents that the application cannot skip any more songs because skip limit is
     * reached.
     */
    public static final int RESULT_CODE_SKIP_LIMIT_REACHED = 10008;

    /**
     * Result code represents that the session needs user's manual intervention.
     */
    public static final int RESULT_CODE_SETUP_REQUIRED = 10009;

    private ResultCodes() {
        // Prevents from instantiation.
    }
}
