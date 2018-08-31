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

package androidx.media;

import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;

/** Compatibility library for {@link AudioManager} with fallbacks for older platforms. */
public final class AudioManagerCompat {

    private static final String TAG = "AudioManCompat";

    /**
     * Requests audio focus. See the {@link AudioFocusRequestCompat} for information about the
     * options available to configure your request, and notification of focus gain and loss.
     *
     * @param focusRequest an {@link AudioFocusRequestCompat} instance used to configure how focus
     *     is requested.
     * @return {@link AudioManager#AUDIOFOCUS_REQUEST_FAILED}, {@link
     *     AudioManager#AUDIOFOCUS_REQUEST_GRANTED} or {@link
     *     AudioManager#AUDIOFOCUS_REQUEST_DELAYED}. <br>
     *     Note that the return value is never {@link AudioManager#AUDIOFOCUS_REQUEST_DELAYED} on
     *     APIs lower than 26 or when focus is requested without building the {@link
     *     AudioFocusRequestCompat} with {@link
     *     AudioFocusRequestCompat.Builder#setAcceptsDelayedFocusGain(boolean)} set to {@code true}.
     * @throws NullPointerException if passed a null argument
     */
    public static int requestAudioFocus(
            @NonNull AudioManager audioManager, @NonNull AudioFocusRequestCompat focusRequest) {
        if (audioManager == null) {
            throw new IllegalArgumentException("AudioManager must not be null");
        }
        if (focusRequest == null) {
            throw new IllegalArgumentException("AudioFocusRequestCompat must not be null");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return audioManager.requestAudioFocus(focusRequest.getAudioFocusRequest());
        } else {
            return audioManager.requestAudioFocus(
                    focusRequest.getOnAudioFocusChangeListener(),
                    focusRequest.getAudioAttributesCompat().getLegacyStreamType(),
                    focusRequest.getFocusGain());
        }
    }

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive focus.
     *
     * @param focusRequest the {@link AudioFocusRequestCompat} that was used when requesting focus
     *     with {@link #requestAudioFocus(AudioManager, AudioFocusRequestCompat)}.
     * @return {@link AudioManager#AUDIOFOCUS_REQUEST_FAILED} or {@link
     *     AudioManager#AUDIOFOCUS_REQUEST_GRANTED}
     * @throws IllegalArgumentException if passed a null argument
     */
    public static int abandonAudioFocusRequest(
            @NonNull AudioManager audioManager, @NonNull AudioFocusRequestCompat focusRequest) {
        if (audioManager == null) {
            throw new IllegalArgumentException("AudioManager must not be null");
        }
        if (focusRequest == null) {
            throw new IllegalArgumentException("AudioFocusRequestCompat must not be null");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return audioManager.abandonAudioFocusRequest(focusRequest.getAudioFocusRequest());
        } else {
            return audioManager.abandonAudioFocus(focusRequest.getOnAudioFocusChangeListener());
        }
    }

    private AudioManagerCompat() {}
}
