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
public final class MasterKey {

    private static final int KEY_SIZE = 256;

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    static final String KEYSTORE_PATH_URI = "android-keystore://";
    static final String MASTER_KEY_ALIAS = "_androidx_security_master_key_";

    @NonNull
    public static final KeyGenParameterSpec AES256_GCM_SPEC =
            createAES256GCMKeyGenParameterSpec(MASTER_KEY_ALIAS);

    /**
     * Provides a safe and easy to use KenGenParameterSpec with the settings.
     * Algorithm: AES
     * Block Mode: GCM
     * Padding: No Padding
     * Key Size: 256
     *
     * @param keyAlias The alias for the master key
     * @return The spec for the master key with the specified keyAlias
     */
    @NonNull
    private static KeyGenParameterSpec createAES256GCMKeyGenParameterSpec(
            @NonNull String keyAlias) {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE);
        return builder.build();
    }

    /**
     * Provides a safe and easy to use KenGenParameterSpec with the settings with a default
     * key alias.
     *
     * Algorithm: AES
     * Block Mode: GCM
     * Padding: No Padding
     * Key Size: 256
     *
     * @return The spec for the master key with the default key alias
     */
    @NonNull
    private static KeyGenParameterSpec createAES256GCMKeyGenParameterSpec() {
        return createAES256GCMKeyGenParameterSpec(MASTER_KEY_ALIAS);
    }

    /**
     * Creates or gets the master key provided
     *
     * The encryption scheme is required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param keyGenParameterSpec The key encryption scheme
     * @return The key alias for the master key
     */
    @NonNull
    public static String getOrCreate(
            @NonNull KeyGenParameterSpec keyGenParameterSpec)
            throws GeneralSecurityException, IOException {
        if (!MasterKey.keyExists(keyGenParameterSpec.getKeystoreAlias())) {
            generateKey(keyGenParameterSpec);
        }
        return keyGenParameterSpec.getKeystoreAlias();
    }

    private static void generateKey(@NonNull KeyGenParameterSpec keyGenParameterSpec)
            throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE);
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    private static boolean keyExists(@NonNull String keyAlias)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore.containsAlias(keyAlias);
    }

}
