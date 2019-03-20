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

package androidx.security.crypto;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.daead.DeterministicAeadFactory;
import com.google.crypto.tink.daead.DeterministicAeadKeyTemplates;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.streamingaead.StreamingAeadFactory;
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

/**
 * Class that helps encrypt and decrypt data utilizing Tink.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TinkCipher {

    private static final String TAG = "TinkCipher";
    private static final String KEYSTORE_PATH_URI = "android-keystore://";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private MasterKey mMasterKey;
    private Context mContext;
    private AndroidKeysetManager mAeadKeysetManager;
    private AndroidKeysetManager mDaeadKeysetManager;
    private AndroidKeysetManager mStreamingAaeadKeysetManager;

    private EncryptedSharedPreferencesKeysets mEncryptedSharedPreferencesKeysets;
    private EncryptedFileKeyset mEncryptedFileKeyset;

    /**
     * Gets an instance of to handle SharedPreferences crypto operations.
     *
     * @param masterKey the config
     * @param context The context
     * @param keysets The Keysets
     * @return the secure cipher
     */
    @NonNull
    public static TinkCipher getSharedPreferencesCipher(
            @NonNull MasterKey masterKey,
            @NonNull Context context,
            @NonNull EncryptedSharedPreferencesKeysets keysets)
            throws GeneralSecurityException, IOException {
        return new TinkCipher(masterKey, keysets, null, context);
    }

    /**
     * Gets an instance of to handle file based crypto operations.
     *
     * @param masterKey the config
     * @param context The context
     * @param keyset The Keysets
     * @return the secure cipher
     */
    @NonNull
    public static TinkCipher getFileCipher(
            @NonNull MasterKey masterKey,
            @NonNull Context context,
            @NonNull EncryptedFileKeyset keyset)
            throws GeneralSecurityException, IOException {
        return new TinkCipher(masterKey, null, keyset, context);
    }

    private TinkCipher(MasterKey masterKey,
            @Nullable EncryptedSharedPreferencesKeysets sharedPreferencesKeysets,
            @Nullable EncryptedFileKeyset fileKeyset,
            @NonNull Context context)
            throws GeneralSecurityException {
        mMasterKey = masterKey;
        mContext = context;
        mEncryptedSharedPreferencesKeysets = sharedPreferencesKeysets;
        mEncryptedFileKeyset = fileKeyset;
        TinkConfig.register();
    }

    private synchronized void setupSharedPrefEncryptionKeys(Context context)
            throws GeneralSecurityException, IOException {
        if (mEncryptedSharedPreferencesKeysets == null) {
            throw new GeneralSecurityException("Missing keyset information.");
        }
        if (mAeadKeysetManager == null) {
            MasterKey.ensureExistence(mMasterKey);
            mAeadKeysetManager = new AndroidKeysetManager.Builder()
                    .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                    .withSharedPref(context, mEncryptedSharedPreferencesKeysets.getKeyKeysetAlias(),
                            mEncryptedSharedPreferencesKeysets.getKeyFileName())
                    .withMasterKeyUri(getKeyStorePathUri(mMasterKey))
                    .build();
            mDaeadKeysetManager = new AndroidKeysetManager.Builder()
                    .withKeyTemplate(DeterministicAeadKeyTemplates.AES256_SIV)
                    .withSharedPref(context,
                            mEncryptedSharedPreferencesKeysets.getValueKeysetAlias(),
                            mEncryptedSharedPreferencesKeysets.getValueFileName())
                    .withMasterKeyUri(getKeyStorePathUri(mMasterKey))
                    .build();
        }
    }

    private synchronized void setupFileEncryptionKeys(Context context)
            throws GeneralSecurityException, IOException {
        if (mEncryptedFileKeyset == null) {
            throw new GeneralSecurityException("Missing keyset information.");
        }
        if (mStreamingAaeadKeysetManager == null) {
            MasterKey.ensureExistence(mMasterKey);
            mStreamingAaeadKeysetManager = new AndroidKeysetManager.Builder()
                    .withKeyTemplate(StreamingAeadKeyTemplates.AES256_GCM_HKDF_4KB)
                    .withSharedPref(context, mEncryptedFileKeyset.getFileKeysetAlias(),
                            mEncryptedFileKeyset.getFileName())
                    .withMasterKeyUri(getKeyStorePathUri(mMasterKey))
                    .build();
        }
    }

    /**
     * @param masterKey The master key
     * @return The full path to the AndroidKeyStore master key
     */
    private String getKeyStorePathUri(MasterKey masterKey) {
        return KEYSTORE_PATH_URI + masterKey.getKeyAlias();
    }

    /**
     * Rotates key and value SharedPreferences keysets
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public synchronized void rotateSharedPreferencesKeysets()
            throws GeneralSecurityException, IOException {
        setupSharedPrefEncryptionKeys(mContext);
        mAeadKeysetManager.rotate(AeadKeyTemplates.AES256_GCM);
        mDaeadKeysetManager.rotate(DeterministicAeadKeyTemplates.AES256_SIV);
    }

    /**
     * Rotates the file encryption keyset
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public synchronized void rotateFileKeyset()
            throws GeneralSecurityException, IOException {
        setupFileEncryptionKeys(mContext);
        mStreamingAaeadKeysetManager.rotate(
                StreamingAeadKeyTemplates.AES256_GCM_HKDF_4KB);
    }


    /**
     * Encrypts Aead data with an existing key alias from the AndroidKeyStore.
     *
     * @param clearData The unencrypted data to encrypt
     * @param aad Associated Data for the encrypted data
     * @param deterministic true to use deterministic encryption
     * @return the encrypted data
     * @throws GeneralSecurityException
     * @throws IOException
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public byte[] encryptAead(
            @NonNull final Context context,
            @NonNull final byte[] clearData,
            @NonNull final byte[] aad,
            boolean deterministic) throws GeneralSecurityException, IOException {
        setupSharedPrefEncryptionKeys(context);
        KeysetHandle keysetHandle = deterministic
                ? mDaeadKeysetManager.getKeysetHandle()
                : mAeadKeysetManager.getKeysetHandle();

        byte[] encrypted = new byte[0];
        if (!deterministic) {
            Aead aead = AeadFactory.getPrimitive(keysetHandle);
            encrypted = aead.encrypt(clearData, aad);
        } else {
            DeterministicAead daead = DeterministicAeadFactory.getPrimitive(keysetHandle);
            encrypted = daead.encryptDeterministically(clearData, aad);
        }
        return encrypted;
    }

    /**
     * Decrypts Aead data with an existing key alias from the AndroidKeyStore.
     *
     * @param cipherText The encrypted data
     * @param aad Associated Data for the encrypted data
     * @param deterministic true to use deterministic encryption
     * @return the decrypted data
     * @throws GeneralSecurityException
     * @throws IOException
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public byte[] decryptAead(
            @NonNull final Context context,
            @NonNull final byte[] cipherText,
            @NonNull final byte[] aad,
            boolean deterministic) throws GeneralSecurityException, IOException {
        setupSharedPrefEncryptionKeys(context);
        KeysetHandle keysetHandle = deterministic
                ? mDaeadKeysetManager.getKeysetHandle()
                : mAeadKeysetManager.getKeysetHandle();
        byte[] decrypted = new byte[0];
        if (!deterministic) {
            Aead aead = AeadFactory.getPrimitive(keysetHandle);
            decrypted = aead.decrypt(cipherText, aad);
        } else {
            DeterministicAead daead = DeterministicAeadFactory.getPrimitive(keysetHandle);
            decrypted = daead.decryptDeterministically(cipherText, aad);
        }
        return decrypted;
    }

    OutputStream createEncryptedStream(FileOutputStream fileOutputStream, String fileName)
            throws GeneralSecurityException, IOException {
        OutputStream stream = null;
        setupFileEncryptionKeys(mContext);
        if (fileOutputStream != null) {
            KeysetHandle keysetHandle = mStreamingAaeadKeysetManager.getKeysetHandle();
            StreamingAead streamingAead = StreamingAeadFactory.getPrimitive(keysetHandle);
            stream = streamingAead.newEncryptingStream(fileOutputStream,
                    fileName.getBytes(UTF_8));
        }
        return stream;
    }

    InputStream createDecryptionStream(FileInputStream fileInputStream, String fileName)
            throws GeneralSecurityException, IOException {
        InputStream stream = null;
        setupFileEncryptionKeys(mContext);
        if (fileInputStream != null) {
            KeysetHandle keysetHandle = mStreamingAaeadKeysetManager.getKeysetHandle();
            StreamingAead streamingAead = StreamingAeadFactory.getPrimitive(keysetHandle);
            stream = streamingAead.newDecryptingStream(fileInputStream,
                    fileName.getBytes(UTF_8));
        }
        return stream;
    }

}
