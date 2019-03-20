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
 * Defines the encryption schemes and meta data for encrypting files.
 */
public class EncryptedFileKeyset {

    public static final String FILE_NAME = "_encrypted_file_keyset_";
    public static final String KEYSET_ALIAS = "_file_keyset_";

    String mFileName;
    String mFileKeysetAlias;
    FileEncryptionScheme mFileEncryptionScheme;
    Context mContext;
    MasterKey mMasterKey;

    /**
     * @return The file name
     */
    @NonNull
    public String getFileName() {
        return mFileName;
    }

    /**
     * @return The file Keyset alias
     */
    @NonNull
    public String getFileKeysetAlias() {
        return mFileKeysetAlias;
    }

    /**
     * @return The key encryption scheme
     */
    @NonNull
    public FileEncryptionScheme getFileEncryptionScheme() {
        return mFileEncryptionScheme;
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
     * The encryption scheme to encrypt files.
     */
    public enum FileEncryptionScheme {
        /**
         * Uses Streaming Aead encryption to ensure that the value is encrypted. The AAD provided
         * is the file name.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates
         */
        AES256_GCM_HKDF_4KB
    }

    /**
     * Gets the default configuration which automatically sets the SharedPreferences Keyset file
     * names and keys used. The file Keyset is also stored in their own SharedPreferences
     * file.
     *
     * The file encryption scheme is required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param fileEncryptionScheme The key encryption scheme
     * @return The configured Keyset used when encrypting files
     */
    @NonNull
    public static EncryptedFileKeyset getOrCreate(
            @NonNull Context context,
            @NonNull MasterKey masterKey,
            @NonNull FileEncryptionScheme fileEncryptionScheme) {
        Builder builder = new Builder(context, masterKey, fileEncryptionScheme);
        return builder.build();
    }

    /**
     * Rotate Keyset
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void rotateKeys() throws GeneralSecurityException, IOException {
        TinkCipher tinkCipher = TinkCipher.getFileCipher(
                mMasterKey, mContext, this);
        tinkCipher.rotateFileKeyset();
    }

    /**
     * Builder class to configure EncryptedFileKeyset
     */
    public static class Builder {

        public Builder(@NonNull Context context,
                @NonNull MasterKey masterKey,
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mFileName = FILE_NAME;
            mFileKeysetAlias = KEYSET_ALIAS;
            mFileEncryptionScheme = fileEncryptionScheme;
            mContext = context;
            mMasterKey = masterKey;
        }

        String mFileName;
        String mFileKeysetAlias;
        FileEncryptionScheme mFileEncryptionScheme;
        Context mContext;
        MasterKey mMasterKey;

        /**
         * @param fileName The SharedPreferences file to store the file Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeyFileName(@NonNull String fileName) {
            mFileName = fileName;
            return this;
        }

        /**
         * @param fileKeysetAlias The alias to store the Keyset in the file SharedPreferences
         * @return This Builder
         */
        @NonNull
        public Builder setFileKeysetAlias(@NonNull String fileKeysetAlias) {
            mFileKeysetAlias = fileKeysetAlias;
            return this;
        }

        /**
         * @param fileEncryptionScheme The encryption scheme to use for the files. Currently there
         *                              is only one option, but this is provided to ensure that it's
         *                              clear which algorithm is used.
         * @return This Builder
         */
        @NonNull
        public Builder setFileEncryptionScheme(
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mFileEncryptionScheme = fileEncryptionScheme;
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
        public EncryptedFileKeyset build() {
            EncryptedFileKeyset keyset = new EncryptedFileKeyset();
            keyset.mFileName = mFileName;
            keyset.mFileKeysetAlias = mFileKeysetAlias;
            keyset.mFileEncryptionScheme = mFileEncryptionScheme;
            keyset.mContext = mContext;
            keyset.mMasterKey = mMasterKey;
            return keyset;
        }
    }


}
