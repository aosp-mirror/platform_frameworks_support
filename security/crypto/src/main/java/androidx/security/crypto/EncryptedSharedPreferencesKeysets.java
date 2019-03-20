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

package androidx.security.crypto;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Defines the encryption schemes and meta data for encrypting SharedPreferences.
 *
 */
public class EncryptedSharedPreferencesKeysets {

    public static final String KEY_FILE_NAME = "_encrypted_shared_prefs_key_keyset_";
    public static final String VALUE_FILE_NAME = "_encrypted_shared_prefs_value_keyset_";
    public static final String KEY_KEYSET_ALIAS = "_key_keyset_";
    public static final String VALUE_KEYSET_ALIAS = "_value_keyset_";

    String mKeyFileName;
    String mValueFileName;
    String mKeyKeysetAlias;
    String mValueKeysetAlias;
    KeyEncryptionScheme mKeyEncryptionScheme;
    ValueEncryptionScheme mValueEncryptionScheme;
    Context mContext;
    MasterKey mMasterKey;

    /**
     * @return The key file name
     */
    @NonNull
    public String getKeyFileName() {
        return mKeyFileName;
    }

    /**
     * @return The value file name
     */
    @NonNull
    public String getValueFileName() {
        return mValueFileName;
    }

    /**
     * @return The key Keyset alias
     */
    @NonNull
    public String getKeyKeysetAlias() {
        return mKeyKeysetAlias;
    }

    /**
     * @return The value Keyset alias
     */
    @NonNull
    public String getValueKeysetAlias() {
        return mValueKeysetAlias;
    }

    /**
     * @return The key encryption scheme
     */
    @NonNull
    public KeyEncryptionScheme getKeyEncryptionScheme() {
        return mKeyEncryptionScheme;
    }

    /**
     * @return The value encryption scheme
     */
    @NonNull
    public ValueEncryptionScheme getValueEncryptionScheme() {
        return mValueEncryptionScheme;
    }

    /**
     * @return The Context
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * @return The MasterKey
     */
    @NonNull
    public MasterKey getMasterKey() {
        return mMasterKey;
    }

    /**
     * The encryption scheme to encrypt keys.
     */
    public enum KeyEncryptionScheme {
        /**
         * Uses Deterministic Aead encryption to ensure that the key is encrypted the same way such
         * that the key can be used as a lookup mechanism.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.daead.DeterministicAeadKeyTemplates
         */
        AES256_SIV
    }

    /**
     * The encryption scheme to encrypt values.
     */
    public enum ValueEncryptionScheme {
        /**
         * Uses Aead encryption to ensure that the value is encrypted. The AAD provided is the key.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.aead.AeadKeyTemplates
         */
        AES256_GCM
    }

    /**
     * Gets the default configuration which automatically sets the SharedPreferences Keyset file
     * names and keys used. The key and value Keysets are also stored in their own SharedPreferences
     * files.
     *
     * The key and value encryption schemes are required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param context The context
     * @param masterKey The master key
     * @param keyEncryptionScheme The key encryption scheme
     * @param valueEncryptionScheme The value encryption scheme
     * @return The configured Keysets used when encrypting SharedPreferences
     */
    @NonNull
    public static EncryptedSharedPreferencesKeysets getDefault(
            @NonNull Context context,
            @NonNull MasterKey masterKey,
            @NonNull KeyEncryptionScheme keyEncryptionScheme,
            @NonNull ValueEncryptionScheme valueEncryptionScheme) {
        Builder builder = new Builder();
        builder.mKeyFileName = KEY_FILE_NAME;
        builder.mValueFileName = VALUE_FILE_NAME;
        builder.mKeyKeysetAlias  = KEY_KEYSET_ALIAS;
        builder.mValueKeysetAlias = VALUE_KEYSET_ALIAS;
        builder.mKeyEncryptionScheme = keyEncryptionScheme;
        builder.mValueEncryptionScheme = valueEncryptionScheme;
        builder.mContext = context;
        builder.mMasterKey = masterKey;
        return builder.build();
    }

    /**
     * Rotate Keysets for key and values.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void rotateKeys() throws GeneralSecurityException, IOException {
        TinkCipher tinkCipher = TinkCipher.getSharedPreferencesCipher(
                mMasterKey, mContext, this);
        tinkCipher.rotateSharedPreferencesKeysets();
    }

    /**
     * Builder class to configure EncryptedSharedPreferencesKeysets
     */
    public static class Builder {

        public Builder() {
        }

        String mKeyFileName;
        String mValueFileName;
        String mKeyKeysetAlias;
        String mValueKeysetAlias;
        KeyEncryptionScheme mKeyEncryptionScheme;
        ValueEncryptionScheme mValueEncryptionScheme;
        Context mContext;
        MasterKey mMasterKey;

        /**
         * @param keyFileName The SharedPreferences file to store the Key Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeyFileName(@NonNull String keyFileName) {
            mKeyFileName = keyFileName;
            return this;
        }

        /**
         * @param valueFileName The SharedPreferences file to store the Value Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setValueFileName(@NonNull String valueFileName) {
            mValueFileName = valueFileName;
            return this;
        }

        /**
         * @param keyKeysetAlias The alias to store the Keyset in the Key SharedPreferences
         * @return This Builder
         */
        @NonNull
        public Builder setKeyKeysetAlias(@NonNull String keyKeysetAlias) {
            mKeyKeysetAlias = keyKeysetAlias;
            return this;
        }

        /**
         * @param valueKeysetAlias The alias to store the Keyset in the Value SharedPreferences
         *                         Keyset.
         * @return
         */
        @NonNull
        public Builder setValueKeysetAlias(@NonNull String valueKeysetAlias) {
            mValueKeysetAlias = valueKeysetAlias;
            return this;
        }

        /**
         * @param keyEncryptionScheme The encryption scheme to use for the key. Currently there is
         *                            only one option, but this is provided to ensure that it's
         *                            clear which algorithm is used.
         * @return
         */
        @NonNull
        public Builder setKeyEncryptionScheme(@NonNull KeyEncryptionScheme keyEncryptionScheme) {
            mKeyEncryptionScheme = keyEncryptionScheme;
            return this;
        }

        /**
         * @param valueEncryptionScheme The encryption scheme to use for the value. Currently there
         *                              is only one option, but this is provided to ensure that it's
         *                              clear which algorithm is used.
         * @return This Builder
         */
        @NonNull
        public Builder setValueEncryptionScheme(
                @NonNull ValueEncryptionScheme valueEncryptionScheme) {
            mValueEncryptionScheme = valueEncryptionScheme;
            return this;
        }

        /**
         * @param context The context
         * @return This Builder
         */
        @NonNull
        public Builder setContext(@NonNull Context context) {
            mContext = context;
            return this;
        }

        /**
         * @param masterKey The master key
         * @return This Builder
         */
        @NonNull
        public Builder setMasterKey(@NonNull MasterKey masterKey) {
            mMasterKey = masterKey;
            return this;
        }

        /**
         * @return The configured Key information
         */
        @NonNull
        public EncryptedSharedPreferencesKeysets build() {
            EncryptedSharedPreferencesKeysets keyset = new EncryptedSharedPreferencesKeysets();
            keyset.mKeyFileName = mKeyFileName;
            keyset.mValueFileName = mValueFileName;
            keyset.mKeyKeysetAlias = mKeyKeysetAlias;
            keyset.mValueKeysetAlias = mValueKeysetAlias;
            keyset.mKeyEncryptionScheme = mKeyEncryptionScheme;
            keyset.mValueEncryptionScheme = mValueEncryptionScheme;
            keyset.mContext = mContext;
            keyset.mMasterKey = mMasterKey;
            return keyset;
        }
    }

}
