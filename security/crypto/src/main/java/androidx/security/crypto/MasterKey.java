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

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;

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
    static final String MASTER_KEY_ALIAS = "_androidx_security_master_key_";

    final KeyGenParameterSpec mKeyGenParameterSpec;

    MasterKey(@NonNull KeyGenParameterSpec parameterSpec) {
        mKeyGenParameterSpec = parameterSpec;
    }

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
    public static KeyGenParameterSpec createAES256GCMKeyGenParameterSpec(
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
    public static KeyGenParameterSpec createAES256GCMKeyGenParameterSpec() {
        return createAES256GCMKeyGenParameterSpec(MASTER_KEY_ALIAS);
    }

    /**
     * Creates or gets the master key provided
     *
     * The encryption scheme is required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param keyGenParameterSpec The key encryption scheme
     * @return The configured Keyset used when encrypting files
     */
    @NonNull
    public static MasterKey getOrCreate(
            @NonNull KeyGenParameterSpec keyGenParameterSpec) {
        Builder builder = new Builder(keyGenParameterSpec);
        return builder.build();
    }

    /**
     * Before each key use, promptForUnlock must be called if userAuthRequiredSeconds was set
     * on key creation.
     *
     * To ensure that crypto operations only occur after a successful auth, it is recommended to
     * override onActivityResult in the activity provided. To ensure the auth was successful check
     * to make sure the resultCode is equal to RESULT_OK.
     *
     * @param activity the activity
     * @param requestCode A code that can be checked, should be > 0 for onActivityResult to fire.
     *                    This code should be checked.
     * @param title the title of the confirm credential screen
     * @param description the description shown to the user for confirm credential
     */
    public void promptForUnlock(@NonNull FragmentActivity activity,
            int requestCode,
            @NonNull CharSequence title,
            @NonNull CharSequence description) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(
                Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(title, description);
        activity.startActivityForResult(intent, requestCode);
    }

    String getKeyAlias() {
        return mKeyGenParameterSpec.getKeystoreAlias();
    }

    /**
     * Builder class to configure the Masterkey
     */
    public static final class Builder {

        public Builder(@NonNull KeyGenParameterSpec keyGenParameterSpec) {
            mKeyGenParameterSpec = keyGenParameterSpec;
        }

        KeyGenParameterSpec mKeyGenParameterSpec;

        /**
         * @return The configured Key information
         */
        @NonNull
        public MasterKey build() {
            MasterKey masterKey = new MasterKey(mKeyGenParameterSpec);
            return masterKey;
        }
    }

    /**
     * Creates the underlying MasterKey in the AndroidKeyStore if it does not exist.
     *
     * @param masterKey The configuration
     * @throws GeneralSecurityException when bad key info has been supplied
     */
    @WorkerThread
    public static void ensureExistence(@NonNull MasterKey masterKey)
            throws GeneralSecurityException, IOException {
        if (!keyExists(masterKey.mKeyGenParameterSpec.getKeystoreAlias())) {
            generateKey(masterKey.mKeyGenParameterSpec);
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MasterKey masterKey = (MasterKey) o;
        return mKeyGenParameterSpec.getKeystoreAlias().equals(
                masterKey.mKeyGenParameterSpec.getKeystoreAlias());
    }

    @Override
    public int hashCode() {
        return mKeyGenParameterSpec.getKeystoreAlias().hashCode();
    }

    @Override
    public String toString() {
        return "MasterKey{" + "KeyAlias='" + mKeyGenParameterSpec.getKeystoreAlias() + '\'' + '}';
    }
}
