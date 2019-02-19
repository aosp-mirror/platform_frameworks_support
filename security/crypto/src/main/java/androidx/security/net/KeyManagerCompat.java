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
import androidx.security.SecureConfig;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;

import javax.net.ssl.X509KeyManager;

import java.security.cert.X509Certificate;

public class KeyManagerCompat implements X509KeyManager, KeyChainAliasCallback {
    private static final String TAG = "KeyManagerCompat";

    private final String alias;
    private X509Certificate[] certChain;
    private PrivateKey privateKey;
    private static Activity ACTIVITY;
    private SecureConfig secureConfig;

    public static void setContext(Activity activity) {
        ACTIVITY = activity;
    }

    public enum CertType {
        X509(0),
        PKCS12(1),
        NOT_SUPPORTED(1000);

        private final int type;

        CertType(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }

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


    public static KeyManagerCompat getDefault(String alias) {
        return getDefault(alias, SecureConfig.getStrongConfig());
    }

    public static KeyManagerCompat getDefault(String alias, SecureConfig secureConfig) {
        KeyManagerCompat keyManager = new KeyManagerCompat(alias, secureConfig);
        try {
            KeyChain.choosePrivateKeyAlias(ACTIVITY, keyManager,
                    secureConfig.getClientCertAlgorithms(), null, null, -1, alias);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return keyManager;
    }

    public static KeyManagerCompat installCertManually(CertType certType, byte[] certData, String keyAlias, SecureConfig secureConfig) {
        KeyManagerCompat keyManager = new KeyManagerCompat(keyAlias, secureConfig);
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
                throw new SecurityException("Cert type not supported.");
        }
        ACTIVITY.startActivity(intent);
        return keyManager;
    }

    public KeyManagerCompat(String alias, SecureConfig secureConfig) {
        this.alias = alias;
        this.secureConfig = secureConfig;
    }

    @Override
    public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
        return alias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        if (this.alias.equals(alias)) return certChain;
        return null;
    }

    public void setCertChain(X509Certificate[] certChain) {
        this.certChain = certChain;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        if (this.alias.equals(alias)) return privateKey;
        return null;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public final String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String[] getClientAliases(String keyType, Principal[] issuers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String[] getServerAliases(String keyType, Principal[] issuers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void alias(@NonNull String alias) {
        try {
            certChain = KeyChain.getCertificateChain(ACTIVITY.getApplicationContext(), alias);
            privateKey = KeyChain.getPrivateKey(ACTIVITY.getApplicationContext(), alias);
            if (certChain == null || privateKey == null) {
                throw new SecurityException("Could not retrieve the cert chain and private key from client cert.");
            }
            this.setCertChain(certChain);
            this.setPrivateKey(privateKey);
        } catch (KeyChainException ex) {
            throw new SecurityException("Could not retrieve the cert chain and private key from client cert.");
        } catch (InterruptedException ex) {
            throw new SecurityException("Could not retrieve the cert chain and private key from client cert.");
        }
    }
}
