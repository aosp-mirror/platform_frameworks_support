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

import static androidx.security.content.EncryptedSharedPreferencesCompat.KEY_FILE_NAME;
import static androidx.security.content.EncryptedSharedPreferencesCompat.KEY_KEYSET_ALIAS;
import static androidx.security.content.EncryptedSharedPreferencesCompat.VALUE_FILE_NAME;
import static androidx.security.content.EncryptedSharedPreferencesCompat.VALUE_KEYSET_ALIAS;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.security.content.EncryptedSharedPreferencesCompat;

/**
 * Defines the encryption schemes and meta data for encrypting SharedPreferences.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EncryptedSharedPreferencesKeysets {

    String mKeyFileName;
    String mValueFileName;
    String mKeyKeysetAlias;
    String mValueKeysetAlias;
    EncryptedSharedPreferencesCompat.KeyEncryptionScheme mKeyEncryptionScheme;
    EncryptedSharedPreferencesCompat.ValueEncryptionScheme mValueEncryptionScheme;
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
    public EncryptedSharedPreferencesCompat.KeyEncryptionScheme getKeyEncryptionScheme() {
        return mKeyEncryptionScheme;
    }

    /**
     * @return The value encryption scheme
     */
    @NonNull
    public EncryptedSharedPreferencesCompat.ValueEncryptionScheme getValueEncryptionScheme() {
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
    public static EncryptedSharedPreferencesKeysets getOrCreate(
            @NonNull Context context,
            @NonNull MasterKey masterKey,
            @NonNull EncryptedSharedPreferencesCompat.KeyEncryptionScheme keyEncryptionScheme,
            @NonNull EncryptedSharedPreferencesCompat.ValueEncryptionScheme valueEncryptionScheme) {
        Builder builder = new Builder(context, masterKey,
                keyEncryptionScheme, valueEncryptionScheme);
        return builder.build();
    }

    /**
     * Builder class to configure EncryptedSharedPreferencesKeysets
     */
    public static class Builder {

        public Builder(@NonNull Context context,
                @NonNull MasterKey masterKey,
                @NonNull EncryptedSharedPreferencesCompat.KeyEncryptionScheme keyEncryptionScheme,
                @NonNull EncryptedSharedPreferencesCompat.ValueEncryptionScheme
                        valueEncryptionScheme) {
            mKeyFileName = KEY_FILE_NAME;
            mValueFileName = VALUE_FILE_NAME;
            mKeyKeysetAlias  = KEY_KEYSET_ALIAS;
            mValueKeysetAlias = VALUE_KEYSET_ALIAS;
            mKeyEncryptionScheme = keyEncryptionScheme;
            mValueEncryptionScheme = valueEncryptionScheme;
            mContext = context;
            mMasterKey = masterKey;
        }

        String mKeyFileName;
        String mValueFileName;
        String mKeyKeysetAlias;
        String mValueKeysetAlias;
        EncryptedSharedPreferencesCompat.KeyEncryptionScheme mKeyEncryptionScheme;
        EncryptedSharedPreferencesCompat.ValueEncryptionScheme mValueEncryptionScheme;
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
        public Builder setKeyEncryptionScheme(@NonNull EncryptedSharedPreferencesCompat
                .KeyEncryptionScheme keyEncryptionScheme) {
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
                @NonNull EncryptedSharedPreferencesCompat
                        .ValueEncryptionScheme valueEncryptionScheme) {
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
