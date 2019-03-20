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
import androidx.security.SecureConfig;
import androidx.security.content.SecureSharedPreferencesCompat;
import androidx.security.crypto.FileCipher;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class that provides access to an encrypted file input and output streams, as well as encrypted
 * SharedPreferences.
 */
public class SecureContextCompat {

    private static final String TAG = "SecureContextCompat";

    private Context mContext;
    private SecureConfig mSecureConfig;

    public SecureContextCompat(@NonNull Context context) {
        this(context, SecureConfig.getDefault());
    }

    public SecureContextCompat(@NonNull Context context, @NonNull SecureConfig secureConfig) {
        mContext = context;
        mSecureConfig = secureConfig;
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
    public FileInputStream openEncryptedFileInput(@NonNull String name) throws IOException {
        return new FileCipher(name, mContext.openFileInput(name), mSecureConfig, mContext)
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
            throws IOException {
        FileCipher fileCipher = new FileCipher(name, mContext.openFileOutput(name, mode),
                mSecureConfig, mContext);
        return fileCipher.getFileOutputStream();
    }

    /**
     * Gets a SharedPreferences object that internally handles encryption/decryption.
     *
     * @param name The name of the preferences file
     * @param mode Operating mode, should use MODE_PRIVATE
     * @return A shared preferences object that handles encryption.
     */
    @NonNull
    public SharedPreferences getSharedPreferences(@NonNull String name, int mode,
            @NonNull String keyAlias) {
        return new SecureSharedPreferencesCompat(keyAlias,
                mContext.getSharedPreferences(name, mode),
                mSecureConfig, mContext);
    }

}
