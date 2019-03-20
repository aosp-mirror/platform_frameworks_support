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

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;

/**
 * Defines the key used to encrypt all Keysets for encrypting Files and SharedPreferences.
 *
 * The master key is stored in the AndroidKeyStore and supports advanced access control features
 * including that the device be unlocked for access and biometric / pin / pattern auth.
 */
public class MasterKey {

    private static final int KEY_SIZE = 256;

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final String MASTER_KEY_ALIAS = "_master_key_";

    String mKeyAlias;
    MasterKeyEncryptionScheme mMasterKeyEncryptionScheme;
    int mUserAuthRequiredSeconds;
    boolean mDeviceUnlockedRequired;

    /**
     * The encryption scheme to encrypt files.
     */
    public enum MasterKeyEncryptionScheme {
        /**
         * Uses AES 256 GCM, No Padding, for encryption and decryption of File and
         * SharedPreferences Keysets.
         *
         */
        AES256_GCM
    }

    /**
     * Gets the default configuration which automatically sets the values for the MasterKey alias
     * and provides basic less complex settings for the key.
     *
     * The encryption scheme is required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param masterKeyEncryptionScheme The key encryption scheme
     * @return The configured Keyset used when encrypting files
     */
    @NonNull
    public static MasterKey getOrCreate(
            @NonNull MasterKeyEncryptionScheme masterKeyEncryptionScheme) {
        Builder builder = new Builder(masterKeyEncryptionScheme);
        return builder.build();
    }

    /**
     * Builder class to configure the Masterkey
     */
    public static class Builder {

        public Builder(@NonNull MasterKeyEncryptionScheme masterKeyEncryptionScheme) {
            mMasterKeyEncryptionScheme = masterKeyEncryptionScheme;
            mKeyAlias = MASTER_KEY_ALIAS;
            mDeviceUnlockedRequired = true;
            mUserAuthRequiredSeconds = -1;
        }

        String mKeyAlias;
        MasterKeyEncryptionScheme mMasterKeyEncryptionScheme;
        int mUserAuthRequiredSeconds;
        boolean mDeviceUnlockedRequired;

        /**
         * @param keyAlias The name of
         * @return This Builder
         */
        @NonNull
        public Builder setKeyAlias(@NonNull String keyAlias) {
            mKeyAlias = keyAlias;
            return this;
        }

        /**
         * @param masterKeyEncryptionScheme The encryption scheme used to generate the MasterKey.
         *
         * @return This Builder
         */
        @NonNull
        public Builder setMasterKeyEncryptionScheme(
                @NonNull MasterKeyEncryptionScheme masterKeyEncryptionScheme) {
            masterKeyEncryptionScheme = masterKeyEncryptionScheme;
            return this;
        }

        /**
         * @param userAuthRequiredSeconds The amount of time that the MasterKey will be authorized
         *                                for.
         * @return This Builder
         */
        @NonNull
        public Builder setUserAuthRequiredSeconds(
                int userAuthRequiredSeconds) {
            mUserAuthRequiredSeconds = userAuthRequiredSeconds;
            return this;
        }

        /**
         * @param deviceUnlockedRequired If enabled, the MasterKey will only be available when the
         *                               device has been unlocked.
         * @return This Builder
         */
        @NonNull
        public Builder setUserAuthRequiredSeconds(
                boolean deviceUnlockedRequired) {
            mDeviceUnlockedRequired = deviceUnlockedRequired;
            return this;
        }

        /**
         * @return The configured Key information
         */
        @NonNull
        public MasterKey build() {
            MasterKey masterKey = new MasterKey();
            masterKey.mKeyAlias = mKeyAlias;
            masterKey.mMasterKeyEncryptionScheme = mMasterKeyEncryptionScheme;
            masterKey.mUserAuthRequiredSeconds = mUserAuthRequiredSeconds;
            return masterKey;
        }
    }

    /**
     * @return The master key alias
     */
    @NonNull
    public String getKeyAlias() {
        return mKeyAlias;
    }

    /**
     * @return The master key encryption scheme
     */
    @NonNull
    public MasterKeyEncryptionScheme getEncryptionScheme() {
        return mMasterKeyEncryptionScheme;
    }

    /**
     * @return The time in seconds that the master key will be available for. If the value is less
     * than 1, this feature is disabled.
     */
    public int getUserAuthRequiredSeconds() {
        return mUserAuthRequiredSeconds;
    }

    /**
     * @return true if the device must be unlocked to access the MasterKey.
     */
    public boolean getDeviceUnlockedRequired() {
        return mDeviceUnlockedRequired;
    }

    /**
     * Creates the underlying MasterKey in the AndroidKeyStore if it does not exist.
     *
     * @param masterKey The configuration
     * @throws GeneralSecurityException
     */
    public static void ensureExistence(@NonNull MasterKey masterKey)
            throws GeneralSecurityException, IOException {
        if (!keyExists(masterKey.getKeyAlias())) {
            generateKey(masterKey);
        }
    }

    /**
     * <p>
     * Generates a sensitive data key and adds the SecretKey to the AndroidKeyStore.
     * Utilizes UnlockedDeviceProtection to ensure that the device must be unlocked in order to
     * use the generated key.
     * </p>
     *
     * @param masterKey The MasterKey configuration
     */
    private static void generateKey(@NonNull MasterKey masterKey)
            throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE);
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                masterKey.mKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE);
        // Only enable if value is at least 1, for longer running more complex operations this
        // value should be higher.
        // Until operation level support BiometricPrompt, the key will need to be unlocked for
        // duration of the operation.
        if (masterKey.getUserAuthRequiredSeconds() > 0) {
            builder = builder.setUserAuthenticationRequired(true);
            builder = builder.setUserAuthenticationValidityDurationSeconds(
                    masterKey.mUserAuthRequiredSeconds);
        } else {
            builder = builder.setUserAuthenticationRequired(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder = builder.setUnlockedDeviceRequired(masterKey.mDeviceUnlockedRequired);
        }
        keyGenerator.init(builder.build());
        keyGenerator.generateKey();
    }

    /**
     * Checks to see if the specified key exists in the AndroidKeyStore
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    private static boolean keyExists(@NonNull String keyAlias)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore.containsAlias(keyAlias);
    }

}
