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

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.security.EncryptionConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.KeyGenerator;

/**
 * Class that provides easy and reliable key generation for both symmetric and asymmetric crypto.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AeadMasterKeyGenerator {

    private EncryptionConfig mEncryptionConfig;

    @NonNull
    public static AeadMasterKeyGenerator getDefault() {
        return new AeadMasterKeyGenerator(EncryptionConfig.getAES256GCMConfig());
    }

    /**
     * @param encryptionConfig The config
     * @return The key generator
     */
    @NonNull
    public static AeadMasterKeyGenerator getInstance(@NonNull EncryptionConfig encryptionConfig) {
        return new AeadMasterKeyGenerator(encryptionConfig);
    }

    private AeadMasterKeyGenerator(EncryptionConfig encryptionConfig) {
        this.mEncryptionConfig = encryptionConfig;
    }

    /**
     * <p>
     * Generates a sensitive data key and adds the SecretKey to the AndroidKeyStore.
     * Utilizes UnlockedDeviceProtection to ensure that the device must be unlocked in order to
     * use the generated key.
     * </p>
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key was generated, false otherwise
     */
    public boolean generateKey(@NonNull String keyAlias) {
        boolean created = false;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    mEncryptionConfig.getSymmetricKeyAlgorithm(),
                    mEncryptionConfig.getAndroidKeyStore());
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    keyAlias, mEncryptionConfig.getSymmetricKeyPurposes())
                    .setBlockModes(mEncryptionConfig.getSymmetricBlockModes())
                    .setEncryptionPaddings(mEncryptionConfig.getSymmetricPaddings())
                    .setKeySize(mEncryptionConfig.getSymmetricKeySize());
            /*builder = builder.setUserAuthenticationRequired(
                    mEncryptionConfig.getSymmetricRequireUserAuthEnabled());
            builder = builder.setUserAuthenticationValidityDurationSeconds(
                    mEncryptionConfig.getSymmetricRequireUserValiditySeconds());*/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder = builder.setUnlockedDeviceRequired(
                        mEncryptionConfig.getSymmetricSensitiveDataProtectionEnabled());
            }
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
            created = true;
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex);
        } catch (InvalidAlgorithmParameterException ex) {
            throw new SecurityException(ex);
        } catch (NoSuchProviderException ex) {
            throw new SecurityException(ex);
        }
        return created;
    }

    /**
     * Checks to see if the specified key exists in the AndroidKeyStore
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    public boolean keyExists(@NonNull String keyAlias) {
        boolean exists = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(mEncryptionConfig.getAndroidKeyStore());
            keyStore.load(null);
            exists = keyStore.containsAlias(keyAlias);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
        return exists;
    }

    /**
     * Delete a key from the specified keystore.
     *
     * @param keyAlias The key to delete from the KeyStore
     */
    public void deleteKey(@NonNull String keyAlias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(mEncryptionConfig.getAndroidKeyStore());
            keyStore.load(null);
            keyStore.deleteEntry(keyAlias);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

}
