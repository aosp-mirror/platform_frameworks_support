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

import static androidx.security.EncryptionConfig.ENCRYPTED_FILE_KEYSET;
import static androidx.security.EncryptionConfig.SHARED_PREF_KEY_KEYSET;
import static androidx.security.EncryptionConfig.SHARED_PREF_VALUE_KEYSET;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.security.EncryptionConfig;
import androidx.security.biometric.BiometricKeyAuthCallback;
import androidx.security.util.TinkAndroidKeysetManager;
import androidx.security.util.TinkMasterKeyGenerator;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.daead.DeterministicAeadConfig;
import com.google.crypto.tink.daead.DeterministicAeadFactory;
import com.google.crypto.tink.daead.DeterministicAeadKeyTemplates;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import com.google.crypto.tink.streamingaead.StreamingAeadFactory;
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

/**
 * Class that helps encrypt and decrypt data utilizing Tink.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AeadCipher {

    private static final String TAG = "AeadCipher";

    private EncryptionConfig mEncryptionConfig;
    private Context mContext;
    private TinkAndroidKeysetManager mAeadKeysetManager;
    private TinkAndroidKeysetManager mDaeadKeysetManager;
    private TinkAndroidKeysetManager mStreamingAaeadKeysetManager;

    /**
     * Listener for encryption that requires biometric prompt
     */
    public interface SecureAuthListener {
        /**
         * @param status the status of auth
         */
        void authComplete(@NonNull BiometricKeyAuthCallback.BiometricStatus status);
    }

    /**
     * @param context The context
     * @return The secure cipher
     */
    @NonNull
    public static AeadCipher getDefault(@NonNull Context context)
            throws GeneralSecurityException, IOException {
        return getInstance(EncryptionConfig.getAES256GCMConfig(), context);
    }

    /**
     * Gets an instance using the specified configuration
     *
     * @param encryptionConfig the config
     * @param context The context
     * @return the secure cipher
     */
    @NonNull
    public static AeadCipher getInstance(@NonNull EncryptionConfig encryptionConfig,
                                         @NonNull Context context)
            throws GeneralSecurityException, IOException {
        return new AeadCipher(encryptionConfig, context);
    }

    public AeadCipher(@NonNull EncryptionConfig encryptionConfig, @NonNull Context context)
            throws GeneralSecurityException {
        this.mEncryptionConfig = encryptionConfig;
        this.mContext = context;
    }

    private synchronized void setupSharedPrefEncryptionKeys(Context context)
            throws GeneralSecurityException, IOException {
        if (mAeadKeysetManager == null) {
            AeadConfig.register();
            DeterministicAeadConfig.register();
            TinkMasterKeyGenerator.AsyncAead masterKey = TinkMasterKeyGenerator
                    .getOrCreateMasterKey(mEncryptionConfig);
            mAeadKeysetManager = new TinkAndroidKeysetManager.Builder()
                    .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                    .withSharedPref(context, SHARED_PREF_KEY_KEYSET,
                            SHARED_PREF_KEY_KEYSET)
                    .withMasterKey(masterKey)
                    .build();
            mDaeadKeysetManager = new TinkAndroidKeysetManager.Builder()
                    .withKeyTemplate(DeterministicAeadKeyTemplates.AES256_SIV)
                    .withSharedPref(context, SHARED_PREF_VALUE_KEYSET,
                            SHARED_PREF_VALUE_KEYSET)
                    .withMasterKey(masterKey)
                    .build();
        }
    }

    private synchronized void setupFileEncryptionKeys(Context context)
            throws GeneralSecurityException, IOException {
        if (mStreamingAaeadKeysetManager == null) {
            StreamingAeadConfig.register();
            TinkMasterKeyGenerator.AsyncAead masterKey = TinkMasterKeyGenerator
                    .getOrCreateMasterKey(mEncryptionConfig);
            mStreamingAaeadKeysetManager = new TinkAndroidKeysetManager.Builder()
                    .withKeyTemplate(StreamingAeadKeyTemplates.AES256_GCM_HKDF_4KB)
                    .withSharedPref(context, ENCRYPTED_FILE_KEYSET,
                            ENCRYPTED_FILE_KEYSET)
                    .withMasterKey(masterKey)
                    .build();
        }
    }

    /**
     * Rotates all Tink keys.
     *
     * @param context
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public synchronized void rotateKeys(Context context)
            throws GeneralSecurityException, IOException {
        TinkMasterKeyGenerator.AsyncAead masterKey = TinkMasterKeyGenerator
                .getOrCreateMasterKey(mEncryptionConfig);
        setupFileEncryptionKeys(context);
        setupSharedPrefEncryptionKeys(context);
        mAeadKeysetManager.rotate(AeadKeyTemplates.AES256_GCM);
        mDaeadKeysetManager.rotate(DeterministicAeadKeyTemplates.AES256_SIV);
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
        // Implement biometric prompt to unlock master key
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

    /**
     * Opens an encrypted OutputStream
     *
     * @param fileOutputStream
     * @param fileName
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public OutputStream createEncryptedStream(FileOutputStream fileOutputStream, String fileName)
            throws GeneralSecurityException, IOException {
        OutputStream stream = null;
        setupFileEncryptionKeys(mContext);
        if (fileOutputStream != null) {
            KeysetHandle keysetHandle = mStreamingAaeadKeysetManager.getKeysetHandle();
            StreamingAead streamingAead = StreamingAeadFactory.getPrimitive(keysetHandle);
            stream = streamingAead.newEncryptingStream(fileOutputStream,
                    fileName.getBytes(TinkMasterKeyGenerator.UTF_8));
        }
        return stream;
    }

    /**
     * Open an encrypted InputStream
     *
     * @param fileInputStream
     * @param fileName
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public InputStream createDecryptionStream(FileInputStream fileInputStream, String fileName)
            throws GeneralSecurityException, IOException {
        InputStream stream = null;
        setupFileEncryptionKeys(mContext);
        if (fileInputStream != null) {
            KeysetHandle keysetHandle = mStreamingAaeadKeysetManager.getKeysetHandle();
            StreamingAead streamingAead = StreamingAeadFactory.getPrimitive(keysetHandle);
            stream = streamingAead.newDecryptingStream(fileInputStream,
                    fileName.getBytes(TinkMasterKeyGenerator.UTF_8));
        }
        return stream;
    }

}
