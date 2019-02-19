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

import androidx.security.biometric.BiometricSupport;
import androidx.security.config.TrustAnchorOptions;

// TODO Move properties to their respective builder classes

public class SecureConfig {

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final String ANDROID_CA_STORE = "AndroidCAStore";
    public static final int AES_IV_SIZE_BYTES = 16;
    public static final String SSL_TLS = "TLS";

    private String androidKeyStore = SecureConfig.ANDROID_KEYSTORE;
    private String androidCAStore = SecureConfig.ANDROID_CA_STORE;
    private String keystoreType = "PKCS12";

    // Asymmetric Encryption Constants
    private String asymmetricKeyPairAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
    private int asymmetricKeySize = 4096;
    //private String asymmetricCipherTransformation = "RSA/ECB/PKCS1Padding";
    private String asymmetricCipherTransformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private String asymmetricBlockModes = KeyProperties.BLOCK_MODE_ECB;
    //private String asymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
    private String asymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
    private int asymmetricKeyPurposes = KeyProperties.PURPOSE_DECRYPT;
    // Sets KeyGenBuilder#setUnlockedDeviceRequired to true, requires Android 9 Pie.
    private boolean asymmetricSensitiveDataProtection = true;
    private boolean asymmetricRequireUserAuth = true;
    private int asymmetricRequireUserValiditySeconds = -1;
    private String asymmetricDigests = KeyProperties.DIGEST_SHA256;

    // Symmetric Encryption Constants
    private String symmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
    private String symmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
    private String symmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
    private int symmetricKeySize = 256;
    private int symmetricGcmTagLength = 128;
    private int symmetricKeyPurposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
    private String symmetricCipherTransformation = "AES/GCM/NoPadding";
    // Sets KeyGenBuilder#setUnlockedDeviceRequired to true, requires Android 9 Pie.
    private boolean symmetricSensitiveDataProtection = true;
    private boolean symmetricRequireUserAuth = true;
    private int symmetricRequireUserValiditySeconds = -1;
    private String symmetricDigests = null;

    // Certificate Constants
    private String certPath = "X.509";
    private String certPathValidator = "PKIX";
    private boolean useStrongSSLCiphers = true;
    private String[] strongSSLCiphers = new String[]{
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
    private String[] clientCertAlgorithms = new String[]{"RSA"};
    private TrustAnchorOptions trustAnchorOptions = TrustAnchorOptions.USER_SYSTEM;

    private BiometricSupport biometricSupport = null;

    private SecureConfig() {
    }

    public static class Builder {

        public Builder() {
        }

        // Keystore Constants
        private String androidKeyStore;
        private String androidCAStore;
        private String keystoreType;

        public Builder forKeyStoreType(String keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }

        // Asymmetric Encryption Constants
        private String asymmetricKeyPairAlgorithm;
        private int asymmetricKeySize;
        private String asymmetricCipherTransformation;
        private int asymmetricKeyPurposes;
        private String asymmetricBlockModes;
        private String asymmetricPaddings;
        private boolean asymmetricSensitiveDataProtection;
        private boolean asymmetricRequireUserAuth = true;
        private int asymmetricRequireUserValiditySeconds = -1;

        public Builder setAsymmetricKeyPairAlgorithm(String keyPairAlgorithm) {
            this.asymmetricKeyPairAlgorithm = keyPairAlgorithm;
            return this;
        }

        public Builder setAsymmetricKeySize(int keySize) {
            this.asymmetricKeySize = keySize;
            return this;
        }

        public Builder setAsymmetricCipherTransformation(String cipherTransformation) {
            this.asymmetricCipherTransformation = cipherTransformation;
            return this;
        }

        public Builder setAsymmetricKeyPurposes(int purposes) {
            this.asymmetricKeyPurposes = purposes;
            return this;
        }

        public Builder setAsymmetricBlockModes(String blockModes) {
            this.asymmetricBlockModes = blockModes;
            return this;
        }

        public Builder setAsymmetricPaddings(String paddings) {
            this.asymmetricPaddings = paddings;
            return this;
        }

        public Builder setAsymmetricSensitiveDataProtection(boolean dataProtection) {
            this.asymmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        public Builder setAsymmetricRequireUserAuth(boolean userAuth) {
            this.asymmetricRequireUserAuth = userAuth;
            return this;
        }

        public Builder setAsymmetricRequireUserValiditySeconds(int authValiditySeconds) {
            this.asymmetricRequireUserValiditySeconds = authValiditySeconds;
            return this;
        }

        // Symmetric Encryption Constants
        private String symmetricKeyAlgorithm;
        private String symmetricBlockModes;
        private String symmetricPaddings;
        private int symmetricKeySize;
        private int symmetricGcmTagLength;
        private int symmetricKeyPurposes;
        private String symmetricCipherTransformation;
        private boolean symmetricSensitiveDataProtection;
        private boolean symmetricRequireUserAuth = true;
        private int symmetricRequireUserValiditySeconds = -1;

        public Builder setSymmetricKeyAlgorithm(String keyAlgorithm) {
            this.symmetricKeyAlgorithm = keyAlgorithm;
            return this;
        }

        public Builder setSymmetricKeySize(int keySize) {
            this.symmetricKeySize = keySize;
            return this;
        }

        public Builder setSymmetricCipherTransformation(String cipherTransformation) {
            this.symmetricCipherTransformation = cipherTransformation;
            return this;
        }

        public Builder setSymmetricKeyPurposes(int purposes) {
            this.symmetricKeyPurposes = purposes;
            return this;
        }

        public Builder setSymmetricGcmTagLength(int gcmTagLength) {
            this.symmetricGcmTagLength = gcmTagLength;
            return this;
        }

        public Builder setSymmetricBlockModes(String blockModes) {
            this.symmetricBlockModes = blockModes;
            return this;
        }

        public Builder setSymmetricPaddings(String paddings) {
            this.symmetricPaddings = paddings;
            return this;
        }

        public Builder setSymmetricSensitiveDataProtection(boolean dataProtection) {
            this.symmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        public Builder setSymmetricRequireUserAuth(boolean userAuth) {
            this.symmetricRequireUserAuth = userAuth;
            return this;
        }

        public Builder setSymmetricRequireUserValiditySeconds(int authValiditySeconds) {
            this.symmetricRequireUserValiditySeconds = authValiditySeconds;
            return this;
        }

        // Certificate Constants
        private String certPath;
        private String certPathValidator;
        private boolean useStrongSSLCiphers;
        private String[] strongSSLCiphers;
        private String[] clientCertAlgorithms;
        private TrustAnchorOptions trustAnchorOptions;
        private BiometricSupport biometricSupport;

        public Builder setCertPath(String certPath) {
            this.certPath = certPath;
            return this;
        }

        public Builder setCertPathValidator(String certPathValidator) {
            this.certPathValidator = certPathValidator;
            return this;
        }

        public Builder setUseStrongSSLCiphers(boolean strongSSLCiphers) {
            this.useStrongSSLCiphers = strongSSLCiphers;
            return this;
        }

        public Builder setStrongSSLCiphers(String[] strongSSLCiphers) {
            this.strongSSLCiphers = strongSSLCiphers;
            return this;
        }

        public Builder setClientCertAlgorithms(String[] clientCertAlgorithms) {
            this.clientCertAlgorithms = clientCertAlgorithms;
            return this;
        }

        public Builder setTrustAnchorOptions(TrustAnchorOptions trustAnchorOptions) {
            this.trustAnchorOptions = trustAnchorOptions;
            return this;
        }

        public Builder setBiometricSupport(BiometricSupport biometricSupport) {
            this.biometricSupport = biometricSupport;
            return this;
        }

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
            secureConfig.biometricSupport = this.biometricSupport;

            return secureConfig;
        }
    }

    public static SecureConfig getDefault() {
        return getStrongConfig(null);
    }

    public static SecureConfig getDefault(BiometricSupport biometricSupport) {
        return getStrongConfig(biometricSupport);
    }

    public static SecureConfig getStrongConfig() {
        return getStrongConfig(null);
    }

    public static SecureConfig getStrongConfig(BiometricSupport biometricSupport) {
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
        builder.biometricSupport = biometricSupport;

        return builder.build();
    }

    public String getAndroidKeyStore() {
        return androidKeyStore;
    }

    public void setAndroidKeyStore(String androidKeyStore) {
        this.androidKeyStore = androidKeyStore;
    }

    public String getAndroidCAStore() {
        return androidCAStore;
    }

    public void setAndroidCAStore(String androidCAStore) {
        this.androidCAStore = androidCAStore;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getAsymmetricKeyPairAlgorithm() {
        return asymmetricKeyPairAlgorithm;
    }

    public void setAsymmetricKeyPairAlgorithm(String asymmetricKeyPairAlgorithm) {
        this.asymmetricKeyPairAlgorithm = asymmetricKeyPairAlgorithm;
    }

    public int getAsymmetricKeySize() {
        return asymmetricKeySize;
    }

    public void setAsymmetricKeySize(int asymmetricKeySize) {
        this.asymmetricKeySize = asymmetricKeySize;
    }

    public String getAsymmetricCipherTransformation() {
        return asymmetricCipherTransformation;
    }

    public void setAsymmetricCipherTransformation(String asymmetricCipherTransformation) {
        this.asymmetricCipherTransformation = asymmetricCipherTransformation;
    }

    public String getAsymmetricBlockModes() {
        return asymmetricBlockModes;
    }

    public void setAsymmetricBlockModes(String asymmetricBlockModes) {
        this.asymmetricBlockModes = asymmetricBlockModes;
    }

    public String getAsymmetricPaddings() {
        return asymmetricPaddings;
    }

    public void setAsymmetricPaddings(String asymmetricPaddings) {
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


    public String getSymmetricKeyAlgorithm() {
        return symmetricKeyAlgorithm;
    }

    public void setSymmetricKeyAlgorithm(String symmetricKeyAlgorithm) {
        this.symmetricKeyAlgorithm = symmetricKeyAlgorithm;
    }

    public String getSymmetricBlockModes() {
        return symmetricBlockModes;
    }

    public void setSymmetricBlockModes(String symmetricBlockModes) {
        this.symmetricBlockModes = symmetricBlockModes;
    }

    public String getSymmetricPaddings() {
        return symmetricPaddings;
    }

    public void setSymmetricPaddings(String symmetricPaddings) {
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

    public String getSymmetricCipherTransformation() {
        return symmetricCipherTransformation;
    }

    public void setSymmetricCipherTransformation(String symmetricCipherTransformation) {
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

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getCertPathValidator() {
        return certPathValidator;
    }

    public void setCertPathValidator(String certPathValidator) {
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

    public String[] getStrongSSLCiphers() {
        return strongSSLCiphers;
    }

    public void setStrongSSLCiphers(String[] strongSSLCiphers) {
        this.strongSSLCiphers = strongSSLCiphers;
    }

    public String[] getClientCertAlgorithms() {
        return clientCertAlgorithms;
    }

    public void setClientCertAlgorithms(String[] clientCertAlgorithms) {
        this.clientCertAlgorithms = clientCertAlgorithms;
    }

    public TrustAnchorOptions getTrustAnchorOptions() {
        return trustAnchorOptions;
    }

    public void setTrustAnchorOptions(TrustAnchorOptions trustAnchorOptions) {
        this.trustAnchorOptions = trustAnchorOptions;
    }

    public BiometricSupport getBiometricSupport() {
        return biometricSupport;
    }

    public void setBiometricSupport(BiometricSupport biometricSupport) {
        this.biometricSupport = biometricSupport;
    }
}
