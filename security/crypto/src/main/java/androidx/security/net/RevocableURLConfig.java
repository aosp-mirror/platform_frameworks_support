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


package androidx.security.net;

import androidx.annotation.NonNull;

/**
 * Configuration that defines constants used by the data-in-transit.
 *
 * Default:
 * RevocableURLConfig.getDefault() provides basic Certificate revocation checking via OCSP.
 *
 * Enterprise:
 * RevocableURLConfig.getDefault(), only allows for secure SSL ciphers which use encryption and
 * a SHA of 128 and 256 or higher.
 *
 */
public class RevocableURLConfig {

    public static final String SSL_TLS = "TLS";
    public static final String KEYSTORE_TYPE = "PKCS12";
    public static final String ANDROID_CA_STORE = "AndroidCAStore";

    // Certificate Constants
    String mCertPath;
    String mCertPathValidator;
    boolean mUseStrongSSLCiphers;
    String[] mStrongSSLCiphers;
    String[] mClientCertAlgorithms;
    TrustAnchorOptions mTrustAnchorOptions;

    RevocableURLConfig() {
    }

    /**
     * RevocableURLConfig.Builder configures EncryptionConfig.
     */
    public static class Builder {

        public Builder() {
        }

        // Certificate Constants
        String mCertPath;
        String mCertPathValidator;
        boolean mUseStrongSSLCiphers;
        String[] mStrongSSLCiphers;
        String[] mClientCertAlgorithms;
        TrustAnchorOptions mAnchorOptions;

        /**
         * @param certPath
         * @return The configured builder
         */
        @NonNull
        public Builder setCertPath(@NonNull String certPath) {
            this.mCertPath = certPath;
            return this;
        }

        /**
         * @param certPathValidator
         * @return The configured builder
         */
        @NonNull
        public Builder setCertPathValidator(@NonNull String certPathValidator) {
            this.mCertPathValidator = certPathValidator;
            return this;
        }

        /**
         * @param strongSSLCiphers
         * @return The configured builder
         */
        @NonNull
        public Builder setUseStrongSSLCiphers(boolean strongSSLCiphers) {
            this.mUseStrongSSLCiphers = strongSSLCiphers;
            return this;
        }

        /**
         * @param strongSSLCiphers
         * @return The configured builder
         */
        @NonNull
        public Builder setStrongSSLCiphers(@NonNull String[] strongSSLCiphers) {
            this.mStrongSSLCiphers = strongSSLCiphers;
            return this;
        }

        /**
         * @param clientCertAlgorithms
         * @return The configured builder
         */
        @NonNull
        public Builder setClientCertAlgorithms(@NonNull String[] clientCertAlgorithms) {
            this.mClientCertAlgorithms = clientCertAlgorithms;
            return this;
        }

        /**
         * @param trustAnchorOptions
         * @return The configured builder
         */
        @NonNull
        public Builder setTrustAnchorOptions(@NonNull TrustAnchorOptions trustAnchorOptions) {
            this.mAnchorOptions = trustAnchorOptions;
            return this;
        }


        /**
         * @return The configured builder
         */
        @NonNull
        public RevocableURLConfig build() {
            RevocableURLConfig revocableURLConfig = new RevocableURLConfig();

            revocableURLConfig.mCertPath = this.mCertPath;
            revocableURLConfig.mCertPathValidator = this.mCertPathValidator;
            revocableURLConfig.mUseStrongSSLCiphers = this.mUseStrongSSLCiphers;
            revocableURLConfig.mStrongSSLCiphers = this.mStrongSSLCiphers;
            revocableURLConfig.mClientCertAlgorithms = this.mClientCertAlgorithms;
            revocableURLConfig.mTrustAnchorOptions = this.mAnchorOptions;
            return revocableURLConfig;
        }
    }

    /**
     * @return A default configuration with for consumer applications.
     */
    @NonNull
    public static RevocableURLConfig getDefault() {
        Builder builder = new RevocableURLConfig.Builder();

        builder.mCertPath = "X.509";
        builder.mCertPathValidator = "PKIX";
        builder.mUseStrongSSLCiphers = false;
        builder.mStrongSSLCiphers = null;
        builder.mClientCertAlgorithms = new String[]{"RSA"};
        builder.mAnchorOptions = TrustAnchorOptions.USER_SYSTEM;

        return builder.build();
    }

    /**
     * A configuration with built-in strong SSL ciphers only. For enterprise use.
     *
     * @return The configuration
     */
    @NonNull
    public static RevocableURLConfig getEnterpriseConfig() {
        Builder builder = new RevocableURLConfig.Builder();

        builder.mCertPath = "X.509";
        builder.mCertPathValidator = "PKIX";
        builder.mUseStrongSSLCiphers = false;
        builder.mStrongSSLCiphers = new String[]{
                "TLS_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"
        };
        builder.mClientCertAlgorithms = new String[]{"RSA"};
        builder.mAnchorOptions = TrustAnchorOptions.USER_SYSTEM;

        return builder.build();
    }

    /**
     * @return
     */
    @NonNull
    public String getCertPath() {
        return mCertPath;
    }

    public void setCertPath(@NonNull String certPath) {
        this.mCertPath = certPath;
    }

    /**
     * @return
     */
    @NonNull
    public String getCertPathValidator() {
        return mCertPathValidator;
    }

    /**
     * @param certPathValidator
     */
    public void setCertPathValidator(@NonNull String certPathValidator) {
        this.mCertPathValidator = certPathValidator;
    }

    /**
     * @return
     */
    public boolean getUseStrongSSLCiphersEnabled() {
        return mUseStrongSSLCiphers;
    }

    /**
     * @param useStrongSSLCiphers
     */
    public void setUseStrongSSLCiphers(boolean useStrongSSLCiphers) {
        this.mUseStrongSSLCiphers = useStrongSSLCiphers;
    }

    public boolean getUseStrongSSLCiphers() {
        return mUseStrongSSLCiphers;
    }

    /**
     * @return
     */
    @NonNull
    public String[] getStrongSSLCiphers() {
        return mStrongSSLCiphers;
    }

    /**
     * @param strongSSLCiphers
     */
    public void setStrongSSLCiphers(@NonNull String[] strongSSLCiphers) {
        this.mStrongSSLCiphers = strongSSLCiphers;
    }

    /**
     * @return
     */
    @NonNull
    public String[] getClientCertAlgorithms() {
        return mClientCertAlgorithms;
    }

    public void setClientCertAlgorithms(@NonNull String[] clientCertAlgorithms) {
        this.mClientCertAlgorithms = clientCertAlgorithms;
    }

    /**
     * @return
     */
    @NonNull
    public TrustAnchorOptions getTrustAnchorOptions() {
        return mTrustAnchorOptions;
    }

    /**
     * @param trustAnchorOptions
     */
    public void setTrustAnchorOptions(@NonNull TrustAnchorOptions trustAnchorOptions) {
        this.mTrustAnchorOptions = trustAnchorOptions;
    }
}
