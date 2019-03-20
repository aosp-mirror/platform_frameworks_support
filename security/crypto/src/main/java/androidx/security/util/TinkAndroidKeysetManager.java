/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.security.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.KeysetManager;
import com.google.crypto.tink.KeysetReader;
import com.google.crypto.tink.KeysetWriter;
import com.google.crypto.tink.integration.android.SharedPrefKeysetReader;
import com.google.crypto.tink.integration.android.SharedPrefKeysetWriter;
import com.google.crypto.tink.proto.KeyTemplate;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 *  A wrapper of {@link com.google.crypto.tink.integration.android.AndroidKeysetManager}
 *  that supports reading/writing {@link
 *  * com.google.crypto.tink.proto.Keyset} to/from private shared preferences on Android.
 *
 *  This also supports inject a custom master key which has been created from the library
 *  externally to add support for BiometricPrompt and sensitive data protection
 *  AndroidKeyStore flags.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TinkAndroidKeysetManager {
    private static final String TAG = TinkAndroidKeysetManager.class.getName();
    private final KeysetReader mReader;
    private final KeysetWriter mWriter;
    private final boolean mUseKeystore;
    private final TinkUtil.AsyncAead mMasterKey;
    private final KeyTemplate mKeyTemplate;
    private boolean mEmpty;

    @GuardedBy("this")
    private KeysetManager mKeysetManager;

    TinkAndroidKeysetManager(Builder builder) throws GeneralSecurityException, IOException {
        mReader = builder.mReader;
        if (mReader == null) {
            throw new IllegalArgumentException(
                    "need to specify where to read the keyset from with Builder#withSharedPref");
        }

        mWriter = builder.mWriter;
        if (mWriter == null) {
            throw new IllegalArgumentException(
                    "need to specify where to write the keyset to with Builder#withSharedPref");
        }

        mUseKeystore = builder.mUseKeystore;
        if (mUseKeystore && builder.mMasterKey == null) {
            throw new IllegalArgumentException(
                    "need a master key URI, please set it with Builder#masterKeyUri");
        }
        if (shouldUseKeystore()) {
            mMasterKey = builder.mMasterKey;
        } else {
            mMasterKey = null;
        }

        mKeyTemplate = builder.mKeyTemplate;
        mKeysetManager = readOrGenerateNewKeyset();
    }

    /** A builder for {@link TinkAndroidKeysetManager}. */
    public static final class Builder {
        KeysetReader mReader = null;
        KeysetWriter mWriter = null;
        TinkUtil.AsyncAead mMasterKey = null;
        boolean mUseKeystore = true;
        KeyTemplate mKeyTemplate = null;

        public Builder() { }

        /** Reads and writes the keyset from shared preferences. */
        public Builder withSharedPref(Context context, String keysetName, String prefFileName)
                throws IOException {
            if (context == null) {
                throw new IllegalArgumentException("need an Android context");
            }
            if (keysetName == null) {
                throw new IllegalArgumentException("need a keyset name");
            }
            mReader = new SharedPrefKeysetReader(context, keysetName, prefFileName);
            mWriter = new SharedPrefKeysetWriter(context, keysetName, prefFileName);
            return this;
        }

        /**
         * Sets the master key URI.
         *
         * <p>Only master keys stored in Android Keystore is supported. The URI must start with
         * {@code android-keystore://}.
         */
        public Builder withMasterKey(TinkUtil.AsyncAead val) {
            mMasterKey = val;
            return this;
        }

        /** If the keyset is not found or valid, generates a new one using {@code val}. */
        public Builder withKeyTemplate(KeyTemplate val) {
            mKeyTemplate = val;
            return this;
        }

        /**
         * Does not use Android Keystore which might not work well in some phones.
         *
         * <p><b>Warning:</b> When Android Keystore is disabled, keys are stored in cleartext. This
         * should be safe because they are stored in private preferences.
         */
        public Builder doNotUseKeystore() {
            mUseKeystore = false;
            return this;
        }

        /** @return a {@link KeysetHandle} with the specified options. */
        public TinkAndroidKeysetManager build() throws GeneralSecurityException, IOException {
            return new TinkAndroidKeysetManager(this);
        }
    }

    /** @return a {@link KeysetHandle} of the managed keyset */
    @GuardedBy("this")
    public synchronized KeysetHandle getKeysetHandle() throws GeneralSecurityException {
        return mKeysetManager.getKeysetHandle();
    }

    /**
     * Generates and adds a fresh key generated using {@code mKeyTemplate}, and sets the new
     * key as the primary key.
     *
     * @throws GeneralSecurityException if cannot find any that can handle {@code
     *     mKeyTemplate}
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager rotate(KeyTemplate keyTemplate)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.rotate(keyTemplate);
        write(mKeysetManager);
        return this;
    }

    /**
     * Generates and adds a fresh key generated using {@code mKeyTemplate}.
     *
     * @throws GeneralSecurityException if cannot find any that can handle {@code
     *     mKeyTemplate}
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager add(KeyTemplate keyTemplate)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.add(keyTemplate);
        write(mKeysetManager);
        return this;
    }

    /**
     * Sets the key with {@code keyId} as primary.
     *
     * @throws GeneralSecurityException if the key is not found or not enabled
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager setPrimary(int keyId)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.setPrimary(keyId);
        write(mKeysetManager);
        return this;
    }

    /**
     * Sets the key with {@code keyId} as primary.
     *
     * @throws GeneralSecurityException if the key is not found or not enabled
     * @deprecated use
     */
    @GuardedBy("this")
    @Deprecated
    public synchronized TinkAndroidKeysetManager promote(int keyId)
            throws GeneralSecurityException {
        return setPrimary(keyId);
    }

    /**
     * Enables the key with {@code keyId}.
     *
     * @throws GeneralSecurityException if the key is not found
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager enable(int keyId)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.enable(keyId);
        write(mKeysetManager);
        return this;
    }

    /**
     * Disables the key with {@code keyId}.
     *
     * @throws GeneralSecurityException if the key is not found or it is the primary key
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager disable(int keyId)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.disable(keyId);
        write(mKeysetManager);
        return this;
    }

    /**
     * Deletes the key with {@code keyId}.
     *
     * @throws GeneralSecurityException if the key is not found or it is the primary key
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager delete(int keyId)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.delete(keyId);
        write(mKeysetManager);
        return this;
    }

    /**
     * Checks to see if the keyset is empty
     *
     * @return true if the Keyset is empty, false otherwise
     */
    public boolean isEmpty() {
        return mEmpty;
    }

    /**
     * Destroys the key material associated with the {@code keyId}.
     *
     * @throws GeneralSecurityException if the key is not found or it is the primary key
     */
    @GuardedBy("this")
    public synchronized TinkAndroidKeysetManager destroy(int keyId)
            throws GeneralSecurityException {
        mKeysetManager = mKeysetManager.destroy(keyId);
        write(mKeysetManager);
        return this;
    }

    private KeysetManager readOrGenerateNewKeyset() throws GeneralSecurityException, IOException {
        mEmpty = false;
        try {
            return read();
        } catch (IOException e) {
            // Not found, handle below.
            Log.i(TAG, "cannot read keyset: " + e.toString());
        }
        mEmpty = true;
        // Not found.
        if (mKeyTemplate != null) {
            KeysetManager manager = KeysetManager.withEmptyKeyset().rotate(mKeyTemplate);
            write(manager);
            return manager;
        }
        throw new GeneralSecurityException("cannot obtain keyset handle");
    }

    private KeysetManager read() throws GeneralSecurityException, IOException {
        if (shouldUseKeystore()) {
            try {
                return KeysetManager.withKeysetHandle(KeysetHandle.read(mReader, mMasterKey));
            } catch (InvalidProtocolBufferException | GeneralSecurityException e) {
                // This edge case happens when
                //   - the keyset was generated on a pre M phone which is then
                //   upgraded to M or newer, or
                //   - the keyset was generated with Keystore being disabled, then
                //   Keystore is enabled.

                Log.i(TAG, "cannot decrypt keyset: " + e.toString());
            }
        }
        KeysetHandle handle = CleartextKeysetHandle.read(mReader);
        if (shouldUseKeystore()) {
            // Opportunistically encrypt the keyset to avoid further fallback to cleartext.
            handle.write(mWriter, mMasterKey);
        }
        return KeysetManager.withKeysetHandle(handle);
    }

    private void write(KeysetManager manager) throws GeneralSecurityException {
        try {
            if (shouldUseKeystore()) {
                manager.getKeysetHandle().write(mWriter, mMasterKey);
            } else {
                CleartextKeysetHandle.write(manager.getKeysetHandle(), mWriter);
            }
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
    }

    private boolean shouldUseKeystore() {
        return (mUseKeystore && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }
}
