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
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;

import androidx.security.SecureConfig;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;

public class KeyGeneratorCompat {

    private SecureConfig secureConfig;

    public static class KeyBuilder {

        private String mAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
        private int mKeySize = 256;
        private int mGcmTagLength = 128;
        private String mBlockModes = KeyProperties.BLOCK_MODE_GCM;
        private String mPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
        private int mKeyPurposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        private String mCipherTransformation = "AES/GCM/NoPadding";
        private boolean mSensitiveDataProtection = true;
        private boolean mRequireUserAuth = false;
        private int mRequireUserValditySeconds = -1;

        public KeyBuilder(@NonNull String algorithm) {
            setAlgorithm(algorithm);
        }

        public KeyGeneratorCompat getDefaultEncryptDecrypt() {
            KeyBuilder builder = new KeyBuilder(KeyProperties.KEY_ALGORITHM_AES);
            return builder.build();
        }

        public KeyBuilder setAlgorithm(@NonNull String algorithm) {
            mAlgorithm = algorithm;
            return this;
        }

        public KeyGeneratorCompat build() {
            return new KeyGeneratorCompat(mAlgorithm);
        }
    }

    public static class KeyPairBuilder {

        private String mAlgorithm;
        private int mKeySize;

        public KeyPairBuilder(@NonNull String algorithm) {
            setAlgorithm(algorithm);
        }

        public KeyPairBuilder setAlgorithm(String algorithm) {
            mAlgorithm = algorithm;
            return this;
        }

    }

    public KeyGeneratorCompat(@NonNull String algorithm) {

    }

    public static KeyGeneratorCompat getDefault() {
        return new KeyGeneratorCompat(SecureConfig.getStrongConfig());
    }

    public static KeyGeneratorCompat getInstance(SecureConfig secureConfig) {
        return new KeyGeneratorCompat(secureConfig);
    }

    private KeyGeneratorCompat(SecureConfig secureConfig) {
        this.secureConfig = secureConfig;
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
    //@TargetApi(Build.VERSION_CODES.P)
    public boolean generateKey(String keyAlias, KeyBuilder keyBuilder) {
        boolean created = false;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(secureConfig.getSymmetricKeyAlgorithm(), secureConfig.getAndroidKeyStore());
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    keyAlias, secureConfig.getSymmetricKeyPurposes()).
                    setBlockModes(secureConfig.getSymmetricBlockModes()).
                    setEncryptionPaddings(secureConfig.getSymmetricPaddings()).
                    setKeySize(secureConfig.getSymmetricKeySize());
            builder = builder.setUserAuthenticationRequired(secureConfig.isSymmetricRequireUserAuthEnabled());
            builder = builder.setUserAuthenticationValidityDurationSeconds(secureConfig.getSymmetricRequireUserValiditySeconds());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder = builder.setUnlockedDeviceRequired(secureConfig.isSymmetricSensitiveDataProtectionEnabled());
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
     * <p>
     * Generates a sensitive data public/private key pair and adds the KeyPair to the AndroidKeyStore.
     * Utilizes UnlockedDeviceProtection to ensure that the device must be unlocked in order to
     * use the generated key.
     * </p>
     * <p>
     * ANDROID P ONLY (API LEVEL 28>)
     * </p>
     *
     * @param keyPairAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key was generated, false otherwise
     */
    public boolean generateAsymmetricKeyPair(String keyPairAlias) {
        boolean created = false;
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(secureConfig.getAsymmetricKeyPairAlgorithm(), secureConfig.getAndroidKeyStore());
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    keyPairAlias, secureConfig.getAsymmetricKeyPurposes())
                    .setEncryptionPaddings(secureConfig.getAsymmetricPaddings())
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setBlockModes(secureConfig.getAsymmetricBlockModes())
                    .setKeySize(secureConfig.getAsymmetricKeySize());
            builder = builder.setUserAuthenticationRequired(secureConfig.isAsymmetricRequireUserAuthEnabled());
            builder = builder.setUserAuthenticationValidityDurationSeconds(secureConfig.getAsymmetricRequireUserValiditySeconds());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder = builder.setUnlockedDeviceRequired(secureConfig.isAsymmetricSensitiveDataProtectionEnabled());
            }
            keyGenerator.initialize(builder.build());
            keyGenerator.generateKeyPair();
            created = true;
        } catch (NoSuchProviderException | InvalidAlgorithmParameterException | NoSuchAlgorithmException ex) {
            throw new SecurityException(ex);
        }
        return created;
    }

    /**
     * <p>
     * Generates an Ephemeral symmetric key that can be fully destroyed and removed from memory.
     * </p>
     *
     * @return The EphemeralSecretKey generated
     */
    public EphemeralSecretKey generateEphemeralDataKey() {
        try {
            SecureRandom secureRandom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                secureRandom = SecureRandom.getInstanceStrong();
            } else {
                // Not best practices, TODO update this as per this SO thread
                // https://stackoverflow.com/questions/36813098/securerandom-provider-crypto-unavailable-in-android-n-for-deterministially-gen
                secureRandom = new SecureRandom();
            }
            byte[] key = new byte[secureConfig.getSymmetricKeySize() / 8];
            secureRandom.nextBytes(key);
            return new EphemeralSecretKey(key, secureConfig.getSymmetricKeyAlgorithm());
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        }
    }

}
