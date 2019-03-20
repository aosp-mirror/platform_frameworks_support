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


import android.app.Activity;
import android.content.Intent;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;

import androidx.annotation.NonNull;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

/**
 * Class that helps generate and manage crypto keys
 */
public class RevocableX509KeyManager implements X509KeyManager, KeyChainAliasCallback {
    private static final String TAG = "RevocableX509KeyManager";

    private final String mAlias;
    private X509Certificate[] mCertChain;
    private PrivateKey mPrivateKey;
    private static Activity sActivity;
    private RevocableURLConfig mRevocableURLConfig;

    public static void setContext(@NonNull Activity activity) {
        sActivity = activity;
    }

    public enum CertType {
        X509(0),
        PKCS12(1),
        NOT_SUPPORTED(1000);

        private final int mType;

        CertType(int type) {
            this.mType = type;
        }

        /**
         * @return the type as an int
         */
        public int getType() {
            return this.mType;
        }

        /**
         * @param id the id of the cert type
         * @return the cert type
         */
        @NonNull
        public static CertType fromId(int id) {
            switch (id) {
                case 0:
                    return X509;
                case 1:
                    return PKCS12;
            }
            return NOT_SUPPORTED;
        }
    }


    /**
     * @param alias the key alias
     * @param revocableURLConfig the configuration
     * @return the key manager
     */
    @NonNull
    public static RevocableX509KeyManager getInstance(@NonNull String alias,
                                                      @NonNull RevocableURLConfig
                                                              revocableURLConfig) {
        RevocableX509KeyManager keyManager = new RevocableX509KeyManager(alias, revocableURLConfig);
        try {
            KeyChain.choosePrivateKeyAlias(sActivity, keyManager,
                    revocableURLConfig.getClientCertAlgorithms(),
                    null, null, -1, alias);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return keyManager;
    }

    /**
     * @param certType cert mType to install
     * @param certData the cert data in byte[] format
     * @param keyAlias the alias of they key to use
     * @param revocableURLConfig the crypto config
     * @return the secure key manager instance
     */
    @NonNull
    public static RevocableX509KeyManager installCertManually(@NonNull CertType certType,
                                                              @NonNull byte[] certData,
                                                              @NonNull String keyAlias,
                                                              @NonNull RevocableURLConfig
                                                                          revocableURLConfig) {
        RevocableX509KeyManager keyManager = new RevocableX509KeyManager(keyAlias,
                revocableURLConfig);
        Intent intent = KeyChain.createInstallIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (certType) {
            case X509:
                intent.putExtra(KeyChain.EXTRA_CERTIFICATE, certData);
                break;
            case PKCS12:
                intent.putExtra(KeyChain.EXTRA_PKCS12, certData);
                break;
            default:
                throw new SecurityException("Cert mType not supported.");
        }
        sActivity.startActivity(intent);
        return keyManager;
    }

    public RevocableX509KeyManager(@NonNull String alias,
                                   @NonNull RevocableURLConfig revocableURLConfig) {
        this.mAlias = alias;
        this.mRevocableURLConfig = revocableURLConfig;
    }

    @Override
    @NonNull
    public String chooseClientAlias(@NonNull String[] arg0,
            @NonNull Principal[] arg1, @NonNull Socket arg2) {
        return mAlias;
    }

    @Override
    @NonNull
    public X509Certificate[] getCertificateChain(@NonNull String alias) {
        if (this.mAlias.equals(alias)) return mCertChain;
        return null;
    }

    public void setCertChain(@NonNull X509Certificate[] certChain) {
        this.mCertChain = certChain;
    }

    @Override
    @NonNull
    public PrivateKey getPrivateKey(@NonNull String alias) {
        if (this.mAlias.equals(alias)) return mPrivateKey;
        return null;
    }

    public void setPrivateKey(@NonNull PrivateKey privateKey) {
        this.mPrivateKey = privateKey;
    }

    @Override
    @NonNull
    public final String chooseServerAlias(@NonNull String keyType,
            @NonNull Principal[] issuers, @NonNull Socket socket) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public final String[] getClientAliases(@NonNull String keyType, @NonNull Principal[] issuers) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public final String[] getServerAliases(@NonNull String keyType, @NonNull Principal[] issuers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void alias(@NonNull String alias) {
        try {
            mCertChain = KeyChain.getCertificateChain(sActivity.getApplicationContext(), alias);
            mPrivateKey = KeyChain.getPrivateKey(sActivity.getApplicationContext(), alias);
            if (mCertChain == null || mPrivateKey == null) {
                throw new SecurityException("Could not retrieve the cert chain and private key"
                        + " from client cert.");
            }
            this.setCertChain(mCertChain);
            this.setPrivateKey(mPrivateKey);
        } catch (KeyChainException ex) {
            throw new SecurityException("Could not retrieve the cert chain and private key from"
                    + " client cert.");
        } catch (InterruptedException ex) {
            throw new SecurityException("Could not retrieve the cert chain and private key from"
                    + " client cert.");
        }
    }
}
