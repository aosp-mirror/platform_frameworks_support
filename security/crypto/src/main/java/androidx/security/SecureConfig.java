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
import androidx.security.biometric.BiometricKeyAuthCallback;
import androidx.security.config.TrustAnchorOptions;

public class SecureConfig {

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final String ANDROID_CA_STORE = "AndroidCAStore";
    public static final int AES_IV_SIZE_BYTES = 16;
    public static final String SSL_TLS = "TLS";

    String androidKeyStore = SecureConfig.ANDROID_KEYSTORE;
    String androidCAStore = SecureConfig.ANDROID_CA_STORE;
    String keystoreType = "PKCS12";

    // Asymmetric Encryption Constants
    String asymmetricKeyPairAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
    int asymmetricKeySize = 4096;
    //private String asymmetricCipherTransformation = "RSA/ECB/PKCS1Padding";
    String asymmetricCipherTransformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    String asymmetricBlockModes = KeyProperties.BLOCK_MODE_ECB;
    //private String asymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
    String asymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
    int asymmetricKeyPurposes = KeyProperties.PURPOSE_DECRYPT;
    // Sets KeyGenBuilder#setUnlockedDeviceRequired to true, requires Android 9 Pie.
    boolean asymmetricSensitiveDataProtection = true;
    boolean asymmetricRequireUserAuth = true;
    int asymmetricRequireUserValiditySeconds = -1;
    private String asymmetricDigests = KeyProperties.DIGEST_SHA256;

    // Symmetric Encryption Constants
    String symmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
    String symmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
    String symmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
    int symmetricKeySize = 256;
    int symmetricGcmTagLength = 128;
    int symmetricKeyPurposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
    String symmetricCipherTransformation = "AES/GCM/NoPadding";
    // Sets KeyGenBuilder#setUnlockedDeviceRequired to true, requires Android 9 Pie.
    boolean symmetricSensitiveDataProtection = true;
    boolean symmetricRequireUserAuth = true;
    int symmetricRequireUserValiditySeconds = -1;
    private String symmetricDigests = null;

    // Certificate Constants
    String certPath = "X.509";
    String certPathValidator = "PKIX";
    boolean useStrongSSLCiphers = true;
    String[] strongSSLCiphers = new String[]{
            "TLS_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_GCM_SHA384"
    };
    String[] clientCertAlgorithms = new String[]{"RSA"};
    TrustAnchorOptions trustAnchorOptions = TrustAnchorOptions.USER_SYSTEM;

    BiometricKeyAuthCallback mBiometricKeyAuthCallback = null;

    SecureConfig() {
    }

    public static class Builder {

        public Builder() {
        }

        // Keystore Constants
        String androidKeyStore;
        String androidCAStore;
        String keystoreType;

        @NonNull
        public Builder forKeyStoreType(@NonNull String keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }

        // Asymmetric Encryption Constants
        String asymmetricKeyPairAlgorithm;
        int asymmetricKeySize;
        String asymmetricCipherTransformation;
        int asymmetricKeyPurposes;
        String asymmetricBlockModes;
        String asymmetricPaddings;
        boolean asymmetricSensitiveDataProtection;
        boolean asymmetricRequireUserAuth = true;
        int asymmetricRequireUserValiditySeconds = -1;

        @NonNull
        public Builder setAsymmetricKeyPairAlgorithm(@NonNull String keyPairAlgorithm) {
            this.asymmetricKeyPairAlgorithm = keyPairAlgorithm;
            return this;
        }

        @NonNull
        public Builder setAsymmetricKeySize(int keySize) {
            this.asymmetricKeySize = keySize;
            return this;
        }

        @NonNull
        public Builder setAsymmetricCipherTransformation(@NonNull String cipherTransformation) {
            this.asymmetricCipherTransformation = cipherTransformation;
            return this;
        }

        @NonNull
        public Builder setAsymmetricKeyPurposes(int purposes) {
            this.asymmetricKeyPurposes = purposes;
            return this;
        }

        @NonNull
        public Builder setAsymmetricBlockModes(@NonNull String blockModes) {
            this.asymmetricBlockModes = blockModes;
            return this;
        }

        @NonNull
        public Builder setAsymmetricPaddings(@NonNull String paddings) {
            this.asymmetricPaddings = paddings;
            return this;
        }

        @NonNull
        public Builder setAsymmetricSensitiveDataProtection(boolean dataProtection) {
            this.asymmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        @NonNull
        public Builder setAsymmetricRequireUserAuth(boolean userAuth) {
            this.asymmetricRequireUserAuth = userAuth;
            return this;
        }

        @NonNull
        public Builder setAsymmetricRequireUserValiditySeconds(int authValiditySeconds) {
            this.asymmetricRequireUserValiditySeconds = authValiditySeconds;
            return this;
        }

        // Symmetric Encryption Constants
        String symmetricKeyAlgorithm;
        String symmetricBlockModes;
        String symmetricPaddings;
        int symmetricKeySize;
        int symmetricGcmTagLength;
        int symmetricKeyPurposes;
        String symmetricCipherTransformation;
        boolean symmetricSensitiveDataProtection;
        boolean symmetricRequireUserAuth = true;
        int symmetricRequireUserValiditySeconds = -1;

        @NonNull
        public Builder setSymmetricKeyAlgorithm(@NonNull String keyAlgorithm) {
            this.symmetricKeyAlgorithm = keyAlgorithm;
            return this;
        }

        @NonNull
        public Builder setSymmetricKeySize(int keySize) {
            this.symmetricKeySize = keySize;
            return this;
        }

        @NonNull
        public Builder setSymmetricCipherTransformation(@NonNull String cipherTransformation) {
            this.symmetricCipherTransformation = cipherTransformation;
            return this;
        }

        @NonNull
        public Builder setSymmetricKeyPurposes(int purposes) {
            this.symmetricKeyPurposes = purposes;
            return this;
        }

        @NonNull
        public Builder setSymmetricGcmTagLength(int gcmTagLength) {
            this.symmetricGcmTagLength = gcmTagLength;
            return this;
        }

        @NonNull
        public Builder setSymmetricBlockModes(@NonNull String blockModes) {
            this.symmetricBlockModes = blockModes;
            return this;
        }

        @NonNull
        public Builder setSymmetricPaddings(@NonNull String paddings) {
            this.symmetricPaddings = paddings;
            return this;
        }

        @NonNull
        public Builder setSymmetricSensitiveDataProtection(boolean dataProtection) {
            this.symmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        @NonNull
        public Builder setSymmetricRequireUserAuth(boolean userAuth) {
            this.symmetricRequireUserAuth = userAuth;
            return this;
        }

        @NonNull
        public Builder setSymmetricRequireUserValiditySeconds(int authValiditySeconds) {
            this.symmetricRequireUserValiditySeconds = authValiditySeconds;
            return this;
        }

        // Certificate Constants
        String certPath;
        String certPathValidator;
        boolean useStrongSSLCiphers;
        String[] strongSSLCiphers;
        String[] clientCertAlgorithms;
        TrustAnchorOptions trustAnchorOptions;
        BiometricKeyAuthCallback mBiometricKeyAuthCallback;

        @NonNull
        public Builder setCertPath(@NonNull String certPath) {
            this.certPath = certPath;
            return this;
        }

        @NonNull
        public Builder setCertPathValidator(@NonNull String certPathValidator) {
            this.certPathValidator = certPathValidator;
            return this;
        }

        @NonNull
        public Builder setUseStrongSSLCiphers(boolean strongSSLCiphers) {
            this.useStrongSSLCiphers = strongSSLCiphers;
            return this;
        }

        @NonNull
        public Builder setStrongSSLCiphers(@NonNull String[] strongSSLCiphers) {
            this.strongSSLCiphers = strongSSLCiphers;
            return this;
        }

        @NonNull
        public Builder setClientCertAlgorithms(@NonNull String[] clientCertAlgorithms) {
            this.clientCertAlgorithms = clientCertAlgorithms;
            return this;
        }

        @NonNull
        public Builder setTrustAnchorOptions(@NonNull TrustAnchorOptions trustAnchorOptions) {
            this.trustAnchorOptions = trustAnchorOptions;
            return this;
        }

        @NonNull
        public Builder setBiometricKeyAuthCallback(@NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
            this.mBiometricKeyAuthCallback = biometricKeyAuthCallback;
            return this;
        }

        @NonNull
        public SecureConfig build() {
            SecureConfig secureConfig = new SecureConfig();
            secureConfig.androidKeyStore = this.androidKeyStore;
            secureConfig.androidCAStore = this.androidCAStore;
            secureConfig.keystoreType = this.keystoreType;

            secureConfig.asymmetricKeyPairAlgorithm = this.asymmetricKeyPairAlgorithm;
            secureConfig.asymmetricKeySize = this.asymmetricKeySize;
            secureConfig.asymmetricCipherTransformation = this.asymmetricCipherTransformation;
            secureConfig.asymmetricKeyPurposes = this.asymmetricKeyPurposes;
            secureConfig.asymmetricBlockModes = this.asymmetricBlockModes;
            secureConfig.asymmetricPaddings = this.asymmetricPaddings;
            secureConfig.asymmetricSensitiveDataProtection = this.asymmetricSensitiveDataProtection;
            secureConfig.asymmetricRequireUserAuth = this.asymmetricRequireUserAuth;
            secureConfig.asymmetricRequireUserValiditySeconds = this.asymmetricRequireUserValiditySeconds;

            secureConfig.symmetricKeyAlgorithm = this.symmetricKeyAlgorithm;
            secureConfig.symmetricBlockModes = this.symmetricBlockModes;
            secureConfig.symmetricPaddings = this.symmetricPaddings;
            secureConfig.symmetricKeySize = this.symmetricKeySize;
            secureConfig.symmetricGcmTagLength = this.symmetricGcmTagLength;
            secureConfig.symmetricKeyPurposes = this.symmetricKeyPurposes;
            secureConfig.symmetricCipherTransformation = this.symmetricCipherTransformation;
            secureConfig.symmetricSensitiveDataProtection = this.symmetricSensitiveDataProtection;
            secureConfig.symmetricRequireUserAuth = this.symmetricRequireUserAuth;
            secureConfig.symmetricRequireUserValiditySeconds = this.symmetricRequireUserValiditySeconds;

            secureConfig.certPath = this.certPath;
            secureConfig.certPathValidator = this.certPathValidator;
            secureConfig.useStrongSSLCiphers = this.useStrongSSLCiphers;
            secureConfig.strongSSLCiphers = this.strongSSLCiphers;
            secureConfig.clientCertAlgorithms = this.clientCertAlgorithms;
            secureConfig.trustAnchorOptions = this.trustAnchorOptions;
            secureConfig.mBiometricKeyAuthCallback = this.mBiometricKeyAuthCallback;

            return secureConfig;
        }
    }

    @NonNull
    public static SecureConfig getDefault() {
        return getStrongConfig(null);
    }

    @NonNull
    public static SecureConfig getDefault(@NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
        return getStrongConfig(biometricKeyAuthCallback);
    }

    @NonNull
    public static SecureConfig getStrongConfig() {
        return getStrongConfig(null);
    }

    @NonNull
    public static SecureConfig getStrongConfig(@NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
        SecureConfig.Builder builder = new SecureConfig.Builder();
        builder.androidKeyStore = SecureConfig.ANDROID_KEYSTORE;
        builder.androidCAStore = SecureConfig.ANDROID_CA_STORE;
        builder.keystoreType = "PKCS12";

        builder.asymmetricKeyPairAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
        builder.asymmetricKeySize = 4096;
        //builder.asymmetricCipherTransformation = "RSA/ECB/PKCS1Padding";
        builder.asymmetricCipherTransformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
        builder.asymmetricBlockModes = KeyProperties.BLOCK_MODE_ECB;
        //builder.asymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
        builder.asymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
        builder.asymmetricKeyPurposes = KeyProperties.PURPOSE_DECRYPT;
        builder.asymmetricSensitiveDataProtection = true;
        builder.asymmetricRequireUserAuth = true;
        builder.asymmetricRequireUserValiditySeconds = -1;

        builder.symmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
        builder.symmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
        builder.symmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
        builder.symmetricKeySize = 256;
        builder.symmetricGcmTagLength = 128;
        builder.symmetricKeyPurposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        builder.symmetricCipherTransformation = "AES/GCM/NoPadding";
        builder.symmetricSensitiveDataProtection = true;
        builder.symmetricRequireUserAuth = true;
        builder.symmetricRequireUserValiditySeconds = -1;

        builder.certPath = "X.509";
        builder.certPathValidator = "PKIX";
        builder.useStrongSSLCiphers = false;
        builder.strongSSLCiphers = new String[]{
                "TLS_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384"
        };
        builder.clientCertAlgorithms = new String[]{"RSA"};
        builder.trustAnchorOptions = TrustAnchorOptions.USER_SYSTEM;
        builder.mBiometricKeyAuthCallback = biometricKeyAuthCallback;

        return builder.build();
    }

    @NonNull
    public String getAndroidKeyStore() {
        return androidKeyStore;
    }

    public void setAndroidKeyStore(@NonNull String androidKeyStore) {
        this.androidKeyStore = androidKeyStore;
    }

    @NonNull
    public String getAndroidCAStore() {
        return androidCAStore;
    }

    public void setAndroidCAStore(@NonNull String androidCAStore) {
        this.androidCAStore = androidCAStore;
    }

    @NonNull
    public String getKeystoreType() {
        return keystoreType;
    }

    @NonNull
    public void setKeystoreType(@NonNull String keystoreType) {
        this.keystoreType = keystoreType;
    }

    @NonNull
    public String getAsymmetricKeyPairAlgorithm() {
        return asymmetricKeyPairAlgorithm;
    }

    public void setAsymmetricKeyPairAlgorithm(@NonNull String asymmetricKeyPairAlgorithm) {
        this.asymmetricKeyPairAlgorithm = asymmetricKeyPairAlgorithm;
    }

    public int getAsymmetricKeySize() {
        return asymmetricKeySize;
    }

    public void setAsymmetricKeySize(int asymmetricKeySize) {
        this.asymmetricKeySize = asymmetricKeySize;
    }

    @NonNull
    public String getAsymmetricCipherTransformation() {
        return asymmetricCipherTransformation;
    }

    public void setAsymmetricCipherTransformation(@NonNull String asymmetricCipherTransformation) {
        this.asymmetricCipherTransformation = asymmetricCipherTransformation;
    }

    @NonNull
    public String getAsymmetricBlockModes() {
        return asymmetricBlockModes;
    }

    public void setAsymmetricBlockModes(@NonNull String asymmetricBlockModes) {
        this.asymmetricBlockModes = asymmetricBlockModes;
    }

    @NonNull
    public String getAsymmetricPaddings() {
        return asymmetricPaddings;
    }

    public void setAsymmetricPaddings(@NonNull String asymmetricPaddings) {
        this.asymmetricPaddings = asymmetricPaddings;
    }

    public int getAsymmetricKeyPurposes() {
        return asymmetricKeyPurposes;
    }

    public void setAsymmetricKeyPurposes(int asymmetricKeyPurposes) {
        this.asymmetricKeyPurposes = asymmetricKeyPurposes;
    }

    public boolean isAsymmetricSensitiveDataProtectionEnabled() {
        return asymmetricSensitiveDataProtection;
    }

    public void setAsymmetricSensitiveDataProtection(boolean asymmetricSensitiveDataProtection) {
        this.asymmetricSensitiveDataProtection = asymmetricSensitiveDataProtection;
    }

    public boolean isAsymmetricRequireUserAuthEnabled() {
        return asymmetricRequireUserAuth;
    }

    public void setAsymmetricRequireUserAuth(boolean requireUserAuth) {
        this.asymmetricRequireUserAuth = requireUserAuth;
    }

    public int getAsymmetricRequireUserValiditySeconds() {
        return this.asymmetricRequireUserValiditySeconds;
    }

    public void setAsymmetricRequireUserValiditySeconds(int userValiditySeconds) {
        this.asymmetricRequireUserValiditySeconds = userValiditySeconds;
    }

    @NonNull
    public String getSymmetricKeyAlgorithm() {
        return symmetricKeyAlgorithm;
    }

    public void setSymmetricKeyAlgorithm(@NonNull String symmetricKeyAlgorithm) {
        this.symmetricKeyAlgorithm = symmetricKeyAlgorithm;
    }

    @NonNull
    public String getSymmetricBlockModes() {
        return symmetricBlockModes;
    }

    public void setSymmetricBlockModes(@NonNull String symmetricBlockModes) {
        this.symmetricBlockModes = symmetricBlockModes;
    }

    @NonNull
    public String getSymmetricPaddings() {
        return symmetricPaddings;
    }

    public void setSymmetricPaddings(@NonNull String symmetricPaddings) {
        this.symmetricPaddings = symmetricPaddings;
    }

    public int getSymmetricKeySize() {
        return symmetricKeySize;
    }

    public void setSymmetricKeySize(int symmetricKeySize) {
        this.symmetricKeySize = symmetricKeySize;
    }

    public int getSymmetricGcmTagLength() {
        return symmetricGcmTagLength;
    }

    public void setSymmetricGcmTagLength(int symmetricGcmTagLength) {
        this.symmetricGcmTagLength = symmetricGcmTagLength;
    }

    public int getSymmetricKeyPurposes() {
        return symmetricKeyPurposes;
    }

    public void setSymmetricKeyPurposes(int symmetricKeyPurposes) {
        this.symmetricKeyPurposes = symmetricKeyPurposes;
    }

    @NonNull
    public String getSymmetricCipherTransformation() {
        return symmetricCipherTransformation;
    }

    public void setSymmetricCipherTransformation(@NonNull String symmetricCipherTransformation) {
        this.symmetricCipherTransformation = symmetricCipherTransformation;
    }

    public boolean isSymmetricSensitiveDataProtectionEnabled() {
        return symmetricSensitiveDataProtection;
    }

    public void setSymmetricSensitiveDataProtection(boolean symmetricSensitiveDataProtection) {
        this.symmetricSensitiveDataProtection = symmetricSensitiveDataProtection;
    }

    public boolean isSymmetricRequireUserAuthEnabled() {
        return symmetricRequireUserAuth;
    }

    public void setSymmetricRequireUserAuth(boolean requireUserAuth) {
        this.symmetricRequireUserAuth = requireUserAuth;
    }

    public int getSymmetricRequireUserValiditySeconds() {
        return this.symmetricRequireUserValiditySeconds;
    }

    public void setSymmetricRequireUserValiditySeconds(int userValiditySeconds) {
        this.symmetricRequireUserValiditySeconds = userValiditySeconds;
    }

    @NonNull
    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(@NonNull String certPath) {
        this.certPath = certPath;
    }

    @NonNull
    public String getCertPathValidator() {
        return certPathValidator;
    }

    public void setCertPathValidator(@NonNull String certPathValidator) {
        this.certPathValidator = certPathValidator;
    }

    public boolean isUseStrongSSLCiphersEnabled() {
        return useStrongSSLCiphers;
    }

    public void setUseStrongSSLCiphers(boolean useStrongSSLCiphers) {
        this.useStrongSSLCiphers = useStrongSSLCiphers;
    }

    public boolean isUseStrongSSLCiphers() {
        return useStrongSSLCiphers;
    }

    @NonNull
    public String[] getStrongSSLCiphers() {
        return strongSSLCiphers;
    }

    public void setStrongSSLCiphers(@NonNull String[] strongSSLCiphers) {
        this.strongSSLCiphers = strongSSLCiphers;
    }

    @NonNull
    public String[] getClientCertAlgorithms() {
        return clientCertAlgorithms;
    }

    public void setClientCertAlgorithms(@NonNull String[] clientCertAlgorithms) {
        this.clientCertAlgorithms = clientCertAlgorithms;
    }

    @NonNull
    public TrustAnchorOptions getTrustAnchorOptions() {
        return trustAnchorOptions;
    }

    public void setTrustAnchorOptions(@NonNull TrustAnchorOptions trustAnchorOptions) {
        this.trustAnchorOptions = trustAnchorOptions;
    }

    @NonNull
    public BiometricKeyAuthCallback getBiometricKeyAuthCallback() {
        return mBiometricKeyAuthCallback;
    }

    public void setBiometricKeyAuthCallback(@NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
        this.mBiometricKeyAuthCallback = biometricKeyAuthCallback;
    }
}
