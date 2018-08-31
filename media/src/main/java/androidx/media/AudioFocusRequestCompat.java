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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Compatibility version of an {@link AudioFocusRequest}. */
public class AudioFocusRequestCompat {

    // default attributes for the request when not specified
    private static final AudioAttributesCompat FOCUS_DEFAULT_ATTR =
            new AudioAttributesCompat.Builder().setUsage(AudioAttributesCompat.USAGE_MEDIA).build();

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(SOURCE)
    @IntDef({
        AudioManager.AUDIOFOCUS_GAIN,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
    })
    public @interface FocusGain {}

    private final int mFocusGain;
    private final OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private final Handler mFocusChangeHandler;
    private final AudioAttributesCompat mAudioAttributesCompat;
    private final boolean mPauseOnDuck;
    private final boolean mAcceptsDelayedFocusGain;

    private final Object mFrameworkAudioFocusRequest;

    private AudioFocusRequestCompat(
            int focusGain,
            OnAudioFocusChangeListener onAudioFocusChangeListener,
            Handler focusChangeHandler,
            AudioAttributesCompat audioFocusRequestCompat,
            boolean pauseOnDuck,
            boolean acceptsDelayedFocusGain) {
        mFocusGain = focusGain;
        mFocusChangeHandler = focusChangeHandler;
        mAudioAttributesCompat = audioFocusRequestCompat;
        mPauseOnDuck = pauseOnDuck;
        mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                && mFocusChangeHandler.getLooper() != Looper.getMainLooper()) {
            mOnAudioFocusChangeListener =
                    new OnAudioFocusChangeListenerHandlerCompat(
                            onAudioFocusChangeListener, focusChangeHandler);
        } else {
            mOnAudioFocusChangeListener = onAudioFocusChangeListener;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFrameworkAudioFocusRequest =
                    new AudioFocusRequest.Builder(mFocusGain)
                            .setAudioAttributes(getAudioAttributes())
                            .setAcceptsDelayedFocusGain(mAcceptsDelayedFocusGain)
                            .setWillPauseWhenDucked(mPauseOnDuck)
                            .setOnAudioFocusChangeListener(
                                    mOnAudioFocusChangeListener, mFocusChangeHandler)
                            .build();
        } else {
            mFrameworkAudioFocusRequest = null;
        }
    }

    /**
     * Gets the type of audio focus request configured for this {@code AudioFocusRequestCompat}.
     *
     * @return one of {@link AudioManager#AUDIOFOCUS_GAIN}, {@link
     *     AudioManager#AUDIOFOCUS_GAIN_TRANSIENT}, {@link
     *     AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}, and {@link
     *     AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}.
     */
    public int getFocusGain() {
        return mFocusGain;
    }

    /**
     * Gets the {@link AudioAttributesCompat} set for this {@code AudioFocusRequestCompat}, or
     * the default attributes if none were set.
     *
     * @return non-null {@link AudioAttributesCompat}.
     */
    @NonNull
    public AudioAttributesCompat getAudioAttributesCompat() {
        return mAudioAttributesCompat;
    }

    /**
     * Gets whether the application that would use this {@code AudioFocusRequestCompat} would
     * pause when it is requested to duck. This value is only applicable on {@link
     * android.os.Build.VERSION_CODES#O} and later.
     *
     * @return the duck/pause behavior.
     */
    public boolean willPauseWhenDucked() {
        return mPauseOnDuck;
    }

    /**
     * Gets whether the application that would use this {@code AudioFocusRequest} supports a
     * focus gain granted after a temporary request failure. This value is only applicable on {@link
     * android.os.Build.VERSION_CODES#O} and later.
     *
     * @return whether delayed focus gain is supported.
     */
    public boolean acceptsDelayedFocusGain() {
        return mAcceptsDelayedFocusGain;
    }

    /* package */ OnAudioFocusChangeListener getOnAudioFocusChangeListener() {
        return mOnAudioFocusChangeListener;
    }

    /* package */ Handler getFocusChangeHandler() {
        return mFocusChangeHandler;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    /* package */ AudioAttributes getAudioAttributes() {
        return (mAudioAttributesCompat != null)
                ? (AudioAttributes) (mAudioAttributesCompat.unwrap())
                : null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    /* package */ AudioFocusRequest getAudioFocusRequest() {
        return (AudioFocusRequest) mFrameworkAudioFocusRequest;
    }

    /**
     * Builder class for {@link AudioFocusRequestCompat} objects.
     *
     * <p>See {@link AudioFocusRequestCompat} for an example of building an instance with this
     * builder. <br>
     * The default values for the instance to be built are:
     *
     * <table>
     * <tr><td>focus listener and handler</td><td>none</td></tr>
     * <tr><td>{@link AudioAttributesCompat}</td><td>attributes with usage set to
     * {@link AudioAttributesCompat#USAGE_MEDIA}</td></tr>
     * <tr><td>pauses on duck</td><td>false</td></tr>
     * <tr><td>supports delayed focus grant</td><td>false</td></tr>
     * </table>
     *
     * <p>In cotrast to a {@link AudioFocusRequest}, attempting to {@link #build()} an {@link
     * AudioFocusRequestCompat} without an {@link AudioManager.OnAudioFocusChangeListener} will
     * throw an {@link IllegalArgumentException}, because the listener is required for all API
     * levels up to API 26.
     */
    public static final class Builder {
        private int mFocusGain;
        private OnAudioFocusChangeListener mOnAudioFocusChangeListener;
        private Handler mFocusChangeHandler;
        private AudioAttributesCompat mAudioAttributesCompat = FOCUS_DEFAULT_ATTR;

        // Flags
        private boolean mPauseOnDuck;
        private boolean mAcceptsDelayedFocusGain;

        /**
         * Constructs a new {@code Builder}, and specifies how audio focus will be requested. Valid
         * values for focus requests in all API levels are {@link AudioManager#AUDIOFOCUS_GAIN},
         * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT}, and {@link
         * AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}. {@link
         * AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE} is valid on API levels 19 and higher.
         *
         * <p>By default there is no focus change listener, delayed focus is not supported, ducking
         * is suitable for the application, and the <code>AudioAttributesCompat</code> have a usage
         * of {@link AudioAttributes#USAGE_MEDIA}.
         *
         * @param focusGain the type of audio focus gain that will be requested
         * @throws IllegalArgumentException thrown when an invalid focus gain type is used
         */
        public Builder(@FocusGain int focusGain) {
            setFocusGain(focusGain);
        }

        /**
         * Constructs a new {@code Builder} with all the properties of the {@code
         * AudioFocusRequestCompat} passed as parameter. Use this method when you want a new request
         * to differ only by some properties.
         *
         * @param requestToCopy the non-null {@code AudioFocusRequestCompat} to duplicate.
         * @throws IllegalArgumentException thrown when a null {@code AudioFocusRequestCompat} is
         *     used.
         */
        public Builder(@NonNull AudioFocusRequestCompat requestToCopy) {
            if (requestToCopy == null) {
                throw new IllegalArgumentException(
                        "AudioFocusRequestCompat to copy must not be null");
            }
            mFocusGain = requestToCopy.mFocusGain;
            mOnAudioFocusChangeListener = requestToCopy.mOnAudioFocusChangeListener;
            mFocusChangeHandler = requestToCopy.mFocusChangeHandler;
            mAudioAttributesCompat = requestToCopy.mAudioAttributesCompat;
            mPauseOnDuck = requestToCopy.mPauseOnDuck;
            mAcceptsDelayedFocusGain = requestToCopy.mAcceptsDelayedFocusGain;
        }

        /**
         * Sets the type of focus gain that will be requested. Use this method to replace the focus
         * gain when building a request by modifying an existing {@code AudioFocusRequestCompat}
         * instance.
         *
         * @param focusGain the type of audio focus gain that will be requested.
         * @return this {@code Builder} instance
         * @throws IllegalArgumentException thrown when an invalid focus gain type is used
         */
        @NonNull
        public Builder setFocusGain(@FocusGain int focusGain) {
            if (!isValidFocusGain(focusGain)) {
                throw new IllegalArgumentException("Illegal audio focus gain type " + focusGain);
            }
            mFocusGain = focusGain;
            return this;
        }

        /**
         * Sets the listener called when audio focus changes after being requested with {@link
         * AudioManagerCompat#requestAudioFocus(AudioManager, AudioFocusRequestCompat)}, and until
         * being abandoned with {@link AudioManagerCompat#abandonAudioFocusRequest(AudioManager,
         * AudioFocusRequestCompat)}. Note that only focus changes (gains and losses) affecting the
         * focus owner are reported, not gains and losses of other focus requesters in the system.
         * <br>
         * Notifications are delivered on the main thread.
         *
         * @param listener the listener receiving the focus change notifications.
         * @return this {@code Builder} instance.
         * @throws NullPointerException thrown when a null focus listener is used.
         */
        @NonNull
        public Builder setOnAudioFocusChangeListener(@NonNull OnAudioFocusChangeListener listener) {
            return setOnAudioFocusChangeListener(listener, new Handler(Looper.getMainLooper()));
        }

        /**
         * Sets the listener called when audio focus changes after being requested with {@link
         * AudioManagerCompat#requestAudioFocus(AudioManager, AudioFocusRequestCompat)}, and until
         * being abandoned with {@link AudioManagerCompat#abandonAudioFocusRequest(AudioManager,
         * AudioFocusRequestCompat)}. Note that only focus changes (gains and losses) affecting the
         * focus owner are reported, not gains and losses of other focus requesters in the system.
         *
         * @param listener the listener receiving the focus change notifications.
         * @param handler the {@link Handler} for the thread on which to execute the notifications.
         * @return this {@code Builder} instance.
         * @throws NullPointerException thrown when a null focus listener or handler is used.
         */
        @NonNull
        public Builder setOnAudioFocusChangeListener(
                @NonNull OnAudioFocusChangeListener listener, @NonNull Handler handler) {
            if (listener == null) {
                throw new IllegalArgumentException(
                        "OnAudioFocusChangeListener must not be null");
            }
            if (handler == null) {
                throw new IllegalArgumentException(
                        "Handler must not be null");
            }

            mOnAudioFocusChangeListener = listener;
            mFocusChangeHandler = handler;
            return this;
        }

        /**
         * Sets the {@link AudioAttributesCompat} to be associated with the focus request, and which
         * describe the use case for which focus is requested. As the focus requests typically
         * precede audio playback, this information is used on certain platforms to declare the
         * subsequent playback use case. It is therefore good practice to use in this method the
         * same {@code AudioAttributesCompat} as used for playback, see for example {@link
         * MediaPlayer#setAudioAttributes(AudioAttributes)} in {@code MediaPlayer} or {@link
         * AudioTrack.Builder#setAudioAttributes(AudioAttributes)} in {@code AudioTrack}.
         *
         * @param attributes the {@link AudioAttributesCompat} for the focus request.
         * @return this {@code Builder} instance.
         * @throws NullPointerException thrown when using null for the attributes.
         */
        @NonNull
        public Builder setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
            if (attributes == null) {
                throw new NullPointerException("Illegal null AudioAttributes");
            }
            mAudioAttributesCompat = attributes;
            return this;
        }

        /**
         * Declare the intended behavior of the application with regards to audio ducking. See more
         * details in the {@link AudioFocusRequest} class documentation. This method will only work
         * on {@link android.os.Build.VERSION_CODES#O} and later.
         *
         * @param pauseOnDuck use {@code true} if the application intends to pause audio playback
         *     when losing focus with {@link AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK}.
         * @return this {@code Builder} instance.
         */
        @NonNull
        public Builder setWillPauseWhenDucked(boolean pauseOnDuck) {
            mPauseOnDuck = pauseOnDuck;
            return this;
        }

        /**
         * Marks this focus request as compatible with delayed focus. See more details about delayed
         * focus in the {@link AudioFocusRequest} class documentation. This method will only work on
         * {@link android.os.Build.VERSION_CODES#O} and later.
         *
         * @param acceptsDelayedFocusGain use {@code true} if the application supports delayed
         *     focus.
         * @return this {@code Builder} instance
         */
        @NonNull
        public Builder setAcceptsDelayedFocusGain(boolean acceptsDelayedFocusGain) {
            mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
            return this;
        }

        /**
         * Builds a new {@code AudioFocusRequestCompat} instance combining all the information
         * gathered by this {@code Builder}'s configuration methods.
         *
         * @return the {@code AudioFocusRequestCompat} instance qualified by all the properties set
         *     on this {@code Builder}.
         * @throws IllegalStateException thrown when attempting to build a focus request without a
         *     focus change listener set.
         */
        public AudioFocusRequestCompat build() {
            if (mOnAudioFocusChangeListener == null) {
                throw new IllegalStateException(
                        "Can't build an AudioFocusRequestCompat instance without a listener");
            }

            return new AudioFocusRequestCompat(
                    mFocusGain,
                    mOnAudioFocusChangeListener,
                    mFocusChangeHandler,
                    mAudioAttributesCompat,
                    mPauseOnDuck,
                    mAcceptsDelayedFocusGain);
        }

        /**
         * @param focusGain value to check
         * @return true if focusGain is a valid value for an audio focus request.
         * @hide Checks whether a focus gain constant is a valid value for an audio focus request.
         */
        @RestrictTo(LIBRARY_GROUP)
        static boolean isValidFocusGain(int focusGain) {
            switch (focusGain) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    return true;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
                default:
                    return false;
            }
        }
    }

    /**
     * Class to allow {@link OnAudioFocusChangeListener#onAudioFocusChange(int)} calls on a specific
     * thread prior to {@link Build.VERSION_CODES#O}.
     */
    private static class OnAudioFocusChangeListenerHandlerCompat
            implements Handler.Callback, OnAudioFocusChangeListener {

        private static final int FOCUS_CHANGE = 0x002a74b2;

        private final Handler mHandler;
        private final OnAudioFocusChangeListener mListener;

        private OnAudioFocusChangeListenerHandlerCompat(
                @NonNull OnAudioFocusChangeListener listener, @NonNull Handler handler) {

            mListener = listener;
            mHandler = new Handler(handler.getLooper(), this);
        }

        @Override
        public void onAudioFocusChange(final int focusChange) {
            mHandler.sendMessage(Message.obtain(mHandler, FOCUS_CHANGE, focusChange, 0));
        }

        @Override
        public boolean handleMessage(Message message) {
            if (message.what == FOCUS_CHANGE) {
                mListener.onAudioFocusChange(message.arg1);
                return true;
            }
            return false;
        }
    }
}
