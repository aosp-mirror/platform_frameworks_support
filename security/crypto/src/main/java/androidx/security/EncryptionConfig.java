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


package androidx.security;

import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.security.biometric.BiometricKeyAuth;

/**
 * Class that defines constants used by the library. Includes predefined configurations for:
 *
 * Default:
 * EncryptionConfig.getAES256GCMConfig() provides a good basic security configuration for
 * encrypting data both in transit and at rest.
 *
 * Biometric: Coming soon!
 * For more sensitive data when you want to ensure that a user is present and the key has been
 * unlocked by the known user.
 *
 *
 *
 */
public class EncryptionConfig {

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final String ANDROID_CA_STORE = "AndroidCAStore";
    public static final String MASTER_KEY = "master_key";

    String mAndroidKeyStore;
    String mAndroidCAStore;
    String mKeystoreType;

    // Symmetric Encryption Constants
    String mSymmetricKeyAlgorithm;
    String mSymmetricBlockModes;
    String mSymmetricPaddings;
    int mSymmetricKeySize;
    int mSymmetricGcmTagLength;
    int mSymmetricKeyPurposes;
    String mSymmetricCipherTransformation;

    boolean mSymmetricSensitiveDataProtection;

    EncryptionConfig() {
    }

    /**
     * EncryptionConfig.Builder configures EncryptionConfig.
     */
    public static class Builder {

        public Builder() {
        }

        // Keystore Constants
        String mAndroidKeyStore;
        String mAndroidCAStore;
        String mKeystoreType;

        /**
         * Sets the keystore type
         *
         * @param keystoreType the KeystoreType to set
         * @return
         */
        @NonNull
        public Builder forKeyStoreType(@NonNull String keystoreType) {
            this.mKeystoreType = keystoreType;
            return this;
        }


        // Symmetric Encryption Constants
        String mSymmetricKeyAlgorithm;
        String mSymmetricBlockModes;
        String mSymmetricPaddings;
        int mSymmetricKeySize;
        int mSymmetricGcmTagLength;
        int mSymmetricKeyPurposes;
        String mSymmetricCipherTransformation;
        boolean mSymmetricSensitiveDataProtection;

        /**
         * @param keyAlgorithm
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricKeyAlgorithm(@NonNull String keyAlgorithm) {
            this.mSymmetricKeyAlgorithm = keyAlgorithm;
            return this;
        }

        /**
         * @param keySize
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricKeySize(int keySize) {
            this.mSymmetricKeySize = keySize;
            return this;
        }

        /**
         * @param cipherTransformation
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricCipherTransformation(@NonNull String cipherTransformation) {
            this.mSymmetricCipherTransformation = cipherTransformation;
            return this;
        }

        /**
         * @param purposes
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricKeyPurposes(int purposes) {
            this.mSymmetricKeyPurposes = purposes;
            return this;
        }

        /**
         * @param gcmTagLength
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricGcmTagLength(int gcmTagLength) {
            this.mSymmetricGcmTagLength = gcmTagLength;
            return this;
        }

        /**
         * @param blockModes
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricBlockModes(@NonNull String blockModes) {
            this.mSymmetricBlockModes = blockModes;
            return this;
        }

        /**
         * @param paddings
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricPaddings(@NonNull String paddings) {
            this.mSymmetricPaddings = paddings;
            return this;
        }

        /**
         * @param dataProtection
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricSensitiveDataProtection(boolean dataProtection) {
            this.mSymmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        /**
         * @return The configured builder
         */
        @NonNull
        public EncryptionConfig build() {
            EncryptionConfig encryptionConfig = new EncryptionConfig();
            encryptionConfig.mAndroidKeyStore = this.mAndroidKeyStore;
            encryptionConfig.mAndroidCAStore = this.mAndroidCAStore;
            encryptionConfig.mKeystoreType = this.mKeystoreType;

            encryptionConfig.mSymmetricKeyAlgorithm = this.mSymmetricKeyAlgorithm;
            encryptionConfig.mSymmetricBlockModes = this.mSymmetricBlockModes;
            encryptionConfig.mSymmetricPaddings = this.mSymmetricPaddings;
            encryptionConfig.mSymmetricKeySize = this.mSymmetricKeySize;
            encryptionConfig.mSymmetricGcmTagLength = this.mSymmetricGcmTagLength;
            encryptionConfig.mSymmetricKeyPurposes = this.mSymmetricKeyPurposes;
            encryptionConfig.mSymmetricCipherTransformation = this.mSymmetricCipherTransformation;
            encryptionConfig.mSymmetricSensitiveDataProtection =
                    this.mSymmetricSensitiveDataProtection;

            return encryptionConfig;
        }
    }

    /**
     * @return A default configuration with for consumer applications.
     */
    @NonNull
    public static EncryptionConfig getAES256GCMConfig() {
        EncryptionConfig.Builder builder = new EncryptionConfig.Builder();
        builder.mAndroidKeyStore = EncryptionConfig.ANDROID_KEYSTORE;
        builder.mAndroidCAStore = EncryptionConfig.ANDROID_CA_STORE;
        builder.mKeystoreType = "PKCS12";

        builder.mSymmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
        builder.mSymmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
        builder.mSymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
        builder.mSymmetricKeySize = 256;
        builder.mSymmetricGcmTagLength = 128;
        builder.mSymmetricKeyPurposes =
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        builder.mSymmetricCipherTransformation = "AES/GCM/NoPadding";
        builder.mSymmetricSensitiveDataProtection = true;

        return builder.build();
    }

    /**
     * Create a Biometric configuration with the strongest encryption settings
     *
     *
     * @param biometricKeyAuth
     * @return The configuration
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static EncryptionConfig getBiometricConfig(
            @NonNull BiometricKeyAuth biometricKeyAuth) {
        EncryptionConfig.Builder builder = new EncryptionConfig.Builder();
        builder.mAndroidKeyStore = EncryptionConfig.ANDROID_KEYSTORE;
        builder.mAndroidCAStore = EncryptionConfig.ANDROID_CA_STORE;
        builder.mKeystoreType = "PKCS12";

        builder.mSymmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
        builder.mSymmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
        builder.mSymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
        builder.mSymmetricKeySize = 256;
        builder.mSymmetricGcmTagLength = 128;
        builder.mSymmetricKeyPurposes =
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        builder.mSymmetricCipherTransformation = "AES/GCM/NoPadding";
        builder.mSymmetricSensitiveDataProtection = true;

        return builder.build();
    }

    /**
     * @return
     */
    @NonNull
    public String getAndroidKeyStore() {
        return mAndroidKeyStore;
    }

    public void setAndroidKeyStore(@NonNull String androidKeyStore) {
        this.mAndroidKeyStore = androidKeyStore;
    }

    /**
     * @return
     */
    @NonNull
    public String getAndroidCAStore() {
        return mAndroidCAStore;
    }

    public void setAndroidCAStore(@NonNull String androidCAStore) {
        this.mAndroidCAStore = androidCAStore;
    }

    /**
     * @return
     */
    @NonNull
    public String getKeystoreType() {
        return mKeystoreType;
    }

    /**
     * @param keystoreType
     */
    @NonNull
    public void setKeystoreType(@NonNull String keystoreType) {
        this.mKeystoreType = keystoreType;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricKeyAlgorithm() {
        return mSymmetricKeyAlgorithm;
    }

    /**
     * @param symmetricKeyAlgorithm
     */
    public void setSymmetricKeyAlgorithm(@NonNull String symmetricKeyAlgorithm) {
        this.mSymmetricKeyAlgorithm = symmetricKeyAlgorithm;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricBlockModes() {
        return mSymmetricBlockModes;
    }

    /**
     * @param symmetricBlockModes
     */
    public void setSymmetricBlockModes(@NonNull String symmetricBlockModes) {
        this.mSymmetricBlockModes = symmetricBlockModes;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricPaddings() {
        return mSymmetricPaddings;
    }

    /**
     * @param symmetricPaddings
     */
    public void setSymmetricPaddings(@NonNull String symmetricPaddings) {
        this.mSymmetricPaddings = symmetricPaddings;
    }

    /**
     * @return
     */
    public int getSymmetricKeySize() {
        return mSymmetricKeySize;
    }

    /**
     * @param symmetricKeySize
     */
    public void setSymmetricKeySize(int symmetricKeySize) {
        this.mSymmetricKeySize = symmetricKeySize;
    }

    /**
     * @return
     */
    public int getSymmetricGcmTagLength() {
        return mSymmetricGcmTagLength;
    }

    /**
     * @param symmetricGcmTagLength
     */
    public void setSymmetricGcmTagLength(int symmetricGcmTagLength) {
        this.mSymmetricGcmTagLength = symmetricGcmTagLength;
    }

    /**
     * @return
     */
    public int getSymmetricKeyPurposes() {
        return mSymmetricKeyPurposes;
    }

    /**
     * @param symmetricKeyPurposes
     */
    public void setSymmetricKeyPurposes(int symmetricKeyPurposes) {
        this.mSymmetricKeyPurposes = symmetricKeyPurposes;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricCipherTransformation() {
        return mSymmetricCipherTransformation;
    }

    /**
     * @param symmetricCipherTransformation
     */
    public void setSymmetricCipherTransformation(@NonNull String symmetricCipherTransformation) {
        this.mSymmetricCipherTransformation = symmetricCipherTransformation;
    }

    /**
     * @return
     */
    public boolean getSymmetricSensitiveDataProtectionEnabled() {
        return mSymmetricSensitiveDataProtection;
    }

    /**
     * @param symmetricSensitiveDataProtection
     */
    public void setSymmetricSensitiveDataProtection(boolean symmetricSensitiveDataProtection) {
        this.mSymmetricSensitiveDataProtection = symmetricSensitiveDataProtection;
    }

}
