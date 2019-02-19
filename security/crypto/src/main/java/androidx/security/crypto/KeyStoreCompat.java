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

import android.security.keystore.KeyInfo;

import androidx.security.SecureConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

public class KeyStoreCompat {

    private static final String TAG = "KeyStoreCompat";

    private SecureConfig secureConfig;

    public static KeyStoreCompat getDefault() {
        return new KeyStoreCompat(SecureConfig.getStrongConfig());
    }

    public static KeyStoreCompat getInstance(SecureConfig secureConfig) {
        return new KeyStoreCompat(secureConfig);
    }

    private KeyStoreCompat(SecureConfig secureConfig) {
        this.secureConfig = secureConfig;
    }

    //@TargetApi(Build.VERSION_CODES.P)
    public boolean keyExists(String keyAlias) {
        boolean exists = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            exists = keyStore.getCertificate(keyAlias).getPublicKey() != null;
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
        return exists;
    }

    /**
     * Checks to see if the specified key is stored in secure hardware
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    public boolean checkKeyInsideSecureHardware(String keyAlias) {
        boolean inHardware = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(keyAlias, null);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(key.getAlgorithm(), secureConfig.getAndroidKeyStore());
            KeyInfo keyInfo;
            keyInfo = (KeyInfo) factory.getKeySpec(key, KeyInfo.class);
            inHardware = keyInfo.isInsideSecureHardware();
            return inHardware;
        } catch (GeneralSecurityException e) {
            return inHardware;
        } catch (IOException e) {
            return inHardware;
        }
    }

    /**
     * Checks to see if the specified private key is stored in secure hardware
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    public boolean checkKeyInsideSecureHardwareAsymmetric(String keyAlias) {
        boolean inHardware = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, null);

            KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(), secureConfig.getAndroidKeyStore());
            KeyInfo keyInfo;

            keyInfo = factory.getKeySpec(privateKey, KeyInfo.class);
            inHardware = keyInfo.isInsideSecureHardware();
            return inHardware;

        } catch (GeneralSecurityException e) {
            return inHardware;
        } catch (IOException e) {
            return inHardware;
        }
    }

}
