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


package androidx.security.context;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.security.EncryptionConfig;
import androidx.security.content.EncryptedSharedPreferencesCompat;
import androidx.security.crypto.AeadCipher;
import androidx.security.crypto.EncryptedFileCipher;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Class that provides access to an encrypted file input and output streams, as well as encrypted
 * SharedPreferences.
 */
public class EncryptedContextCompat {

    private static final String TAG = "EncryptedContextCompat";

    private Context mContext;
    private EncryptionConfig mEncryptionConfig;

    public EncryptedContextCompat(@NonNull Context context) {
        this(context, EncryptionConfig.getAES256GCMConfig());
    }

    public EncryptedContextCompat(@NonNull Context context,
                                  @NonNull EncryptionConfig encryptionConfig) {
        mContext = context;
        mEncryptionConfig = encryptionConfig;
    }

    /**
     * Open an encrypted private file associated with this Context's application
     * package for reading.
     *
     * @param name The name of the file to open; can not contain path separators.
     * @return the encrypted file input stream
     * @throws IOException
     */
    @NonNull
    public FileInputStream openEncryptedFileInput(@NonNull String name)
            throws GeneralSecurityException, IOException {
        return new EncryptedFileCipher(name, mContext.openFileInput(name),
                mEncryptionConfig, mContext)
                .getFileInputStream();
    }

    /**
     * Open a private encrypted file associated with this Context's application package for
     * writing. Creates the file if it doesn't already exist.
     * <p>
     * The written file will be encrypted with the specified keyPairAlias.
     *
     * @param name         The name of the file to open; can not contain path separators.
     * @param mode         Operating mode.
     * @return The resulting {@link FileOutputStream}.
     * @throws IOException
     */
    @NonNull
    public FileOutputStream openEncryptedFileOutput(@NonNull String name, int mode)
            throws GeneralSecurityException, IOException {
        EncryptedFileCipher encryptedFileCipher = new EncryptedFileCipher(name,
                mContext.openFileOutput(name, mode),
                mEncryptionConfig, mContext);
        return encryptedFileCipher.getFileOutputStream();
    }

    /**
     * Gets a SharedPreferences object that internally handles encryption/decryption.
     *
     * @param name The name of the preferences file
     * @param mode Operating mode, should use MODE_PRIVATE
     * @return A shared preferences object that handles encryption.
     */
    @NonNull
    public SharedPreferences getSharedPreferences(@NonNull String name, int mode) {
        return new EncryptedSharedPreferencesCompat(
                mContext.getSharedPreferences(name, mode),
                mEncryptionConfig, mContext);
    }

    /**
     * Rotates encryption keys used to encrypt all files and shared preferences data.
     * Does nothing if the keys haven't be created yet, which happens on first use.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void rotateKeys() throws GeneralSecurityException, IOException {
        AeadCipher aeadCipher = AeadCipher.getInstance(EncryptionConfig.getAES256GCMConfig(),
                mContext);
        aeadCipher.rotateKeys(mContext);
    }

}
