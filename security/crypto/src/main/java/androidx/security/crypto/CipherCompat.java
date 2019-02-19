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


import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;
import androidx.security.biometric.BiometricSupport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class CipherCompat {

    private static final String TAG = "CipherCompat";

    private SecureConfig secureConfig;

    public static int MODE_ENCRYPT = 1;
    public static int MODE_DECRYPT = 2;

    public interface SecureCallback {
    }

    public interface SecureAuthCallback extends SecureCallback {
        void authComplete(BiometricSupport.BiometricStatus status);
    }

    public interface SecureSymmetricEncryptionCallback extends SecureCallback {
        void encryptionComplete(byte[] cipherText, byte[] iv);
    }

    public interface SecureAsymmetricEncryptionCallback extends SecureCallback {
        void encryptionComplete(byte[] cipherText);
    }

    public interface SecureDecryptionCallback extends SecureCallback {
        void decryptionComplete(byte[] clearText);
    }


    public static CipherCompat getDefault() {
        return new CipherCompat(SecureConfig.getStrongConfig());
    }

    public static CipherCompat getDefault(BiometricSupport biometricSupport) {
        return new CipherCompat(SecureConfig.getStrongConfig(biometricSupport));
    }

    public static CipherCompat getInstance(SecureConfig secureConfig) {
        return new CipherCompat(secureConfig);
    }

    public CipherCompat(SecureConfig secureConfig) {
        this.secureConfig = secureConfig;
    }

    public enum SecureFileEncodingType {
        SYMMETRIC(0),
        ASYMMETRIC(1),
        EPHEMERAL(2),
        NOT_ENCRYPTED(1000);

        private final int type;

        SecureFileEncodingType(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }

        public static SecureFileEncodingType fromId(int id) {
            switch (id) {
                case 0:
                    return SYMMETRIC;
                case 1:
                    return ASYMMETRIC;
                case 2:
                    return EPHEMERAL;
            }
            return NOT_ENCRYPTED;
        }

    }


    /**
     * Encrypts data with an existing key alias from the AndroidKeyStore.
     *
     * @param keyAlias  The name of the existing SecretKey to retrieve from the AndroidKeyStore.
     * @param clearData The unencrypted data to encrypt
     * @return A Pair of byte[]'s, first is the encrypted data, second is the IV (initialization vector)
     * used to encrypt which is required for decryption
     */
    public void encryptSensitiveData(String keyAlias, final byte[] clearData, final SecureSymmetricEncryptionCallback callback) {
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(keyAlias, null);
            final Cipher cipher = Cipher.getInstance(secureConfig.getSymmetricCipherTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            if (secureConfig.isSymmetricRequireUserAuthEnabled()) {
                secureConfig.getBiometricSupport().authenticate(cipher, new CipherCompat.SecureAuthCallback() {
                    public void authComplete(BiometricSupport.BiometricStatus status) {

                        switch (status) {
                            case SUCCESS:
                                try {
                                    callback.encryptionComplete(cipher.doFinal(clearData), cipher.getIV());
                                } catch (GeneralSecurityException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                Log.i(TAG, "Failure");
                                callback.encryptionComplete(null, null);
                        }
                    }
                });
            } else {
                callback.encryptionComplete(cipher.doFinal(clearData), cipher.getIV());
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Encrypts data with a public key from the cert in the AndroidKeyStore.
     *
     * @param keyAlias  The name of the existing KeyPair to retrieve the PublicKey from the AndroidKeyStore.
     * @param clearData The unencrypted data to encrypt
     * @return A Pair of byte[]'s, first is the encrypted data, second is the IV (initialization vector)
     * used to encrypt which is required for decryption
     */
    public void encryptSensitiveDataAsymmetric(String keyAlias, byte[] clearData, SecureAsymmetricEncryptionCallback callback) {
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            Cipher cipher = Cipher.getInstance(secureConfig.getAsymmetricCipherTransformation());
            if (secureConfig.getAsymmetricPaddings().equals(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)) {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey, new OAEPParameterSpec("SHA-256",
                        "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT));
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            }
            byte[] clearText = cipher.doFinal(clearData);
            callback.encryptionComplete(clearText);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Encrypts data using an Ephemeral key, destroying any trace of the key from the Cipher used.
     *
     * @param ephemeralSecretKey The generated Ephemeral key
     * @param clearData          The unencrypted data to encrypt
     * @return A Pair of byte[]'s, first is the encrypted data, second is the IV (initialization vector)
     * used to encrypt which is required for decryption
     */
    public Pair<byte[], byte[]> encryptEphemeralData(EphemeralSecretKey
                                                             ephemeralSecretKey, byte[] clearData) {
        try {
            SecureRandom secureRandom = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                secureRandom = SecureRandom.getInstanceStrong();
            } else {
                secureRandom = new SecureRandom();
            }
            byte[] iv = new byte[SecureConfig.AES_IV_SIZE_BYTES];
            secureRandom.nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(secureConfig.getSymmetricGcmTagLength(), iv);
            final Cipher cipher = Cipher.getInstance(secureConfig.getSymmetricCipherTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, ephemeralSecretKey, parameterSpec);
            byte[] encryptedData = cipher.doFinal(clearData);
            ephemeralSecretKey.destroyCipherKey(cipher, Cipher.ENCRYPT_MODE);
            return new Pair<>(encryptedData, iv);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Decrypts a previously encrypted byte[]
     * <p>
     * Destroys all traces of the key data in the Cipher.
     *
     * @param ephemeralSecretKey   The generated Ephemeral key
     * @param encryptedData        The byte[] of encrypted data
     * @param initializationVector The IV of which the encrypted data was encrypted with
     * @return The byte[] of data that has been decrypted
     */
    public byte[] decryptEphemeralData(EphemeralSecretKey ephemeralSecretKey,
                                       byte[] encryptedData, byte[] initializationVector) {
        try {
            final Cipher cipher = Cipher.getInstance(secureConfig.getSymmetricCipherTransformation());
            cipher.init(Cipher.DECRYPT_MODE, ephemeralSecretKey, new GCMParameterSpec(secureConfig.getSymmetricGcmTagLength(), initializationVector));
            byte[] decryptedData = cipher.doFinal(encryptedData);
            ephemeralSecretKey.destroyCipherKey(cipher, Cipher.DECRYPT_MODE);
            return decryptedData;
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Decrypts a previously encrypted byte[]
     *
     * @param keyAlias             The name of the existing SecretKey to retrieve from the AndroidKeyStore.
     * @param encryptedData        The byte[] of encrypted data
     * @param initializationVector The IV of which the encrypted data was encrypted with
     * @return The byte[] of data that has been decrypted
     */
    public void decryptSensitiveData(String keyAlias, final byte[] encryptedData,
                                     byte[] initializationVector, final SecureDecryptionCallback callback) {
        byte[] decryptedData = new byte[0];
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            Key key = keyStore.getKey(keyAlias, null);
            final Cipher cipher = Cipher.getInstance(secureConfig.getSymmetricCipherTransformation());
            GCMParameterSpec spec = new GCMParameterSpec(secureConfig.getSymmetricGcmTagLength(), initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (secureConfig.isSymmetricRequireUserAuthEnabled()) {
                secureConfig.getBiometricSupport().authenticate(cipher, new CipherCompat.SecureAuthCallback() {
                    public void authComplete(BiometricSupport.BiometricStatus status) {
                        switch (status) {
                            case SUCCESS:
                                try {
                                    callback.decryptionComplete(cipher.doFinal(encryptedData));
                                } catch (GeneralSecurityException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                Log.i(TAG, "Failure");
                                callback.decryptionComplete(null);
                        }
                    }
                });
            } else {
                callback.decryptionComplete(cipher.doFinal(encryptedData));
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Decrypts a previously encrypted byte[] with the PrivateKey
     *
     * @param keyAlias      The name of the existing KeyPair to retrieve from the AndroidKeyStore.
     * @param encryptedData The byte[] of encrypted data
     * @return The byte[] of data that has been decrypted
     */
    public void decryptSensitiveDataAsymmetric(String keyAlias, final byte[] encryptedData, final SecureDecryptionCallback callback) {
        byte[] decryptedData = new byte[0];
        try {
            KeyStore keyStore = KeyStore.getInstance(secureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, null);
            final Cipher cipher = Cipher.getInstance(secureConfig.getAsymmetricCipherTransformation());
            if (secureConfig.getAsymmetricPaddings().equals(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)) {
                cipher.init(Cipher.DECRYPT_MODE, key, new OAEPParameterSpec("SHA-256",
                        "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            if (secureConfig.isAsymmetricRequireUserAuthEnabled()) {
                secureConfig.getBiometricSupport().authenticate(cipher, new CipherCompat.SecureAuthCallback() {
                    public void authComplete(BiometricSupport.BiometricStatus status) {
                        Log.i(TAG, "Finished success auth!");
                        switch (status) {
                            case SUCCESS:
                                try {
                                    byte[] clearData = cipher.doFinal(encryptedData);
                                    Log.i(TAG, "Decrypted " + new String(clearData));
                                    callback.decryptionComplete(clearData);

                                } catch (GeneralSecurityException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                Log.i(TAG, "Failure");
                                callback.decryptionComplete(null);
                        }
                    }
                });
            } else {
                decryptedData = cipher.doFinal(encryptedData);
                callback.decryptionComplete(decryptedData);
            }
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
            //Log.e(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    public byte[] encodeEphemeralData(byte[] keyPairAlias, byte[] encryptedKey,
                                      byte[] cipherText, byte[] iv) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(((Integer.SIZE / 8) * 4) + iv.length +
                keyPairAlias.length + encryptedKey.length + cipherText.length);
        byteBuffer.putInt(SecureFileEncodingType.EPHEMERAL.getType());
        byteBuffer.putInt(encryptedKey.length);
        byteBuffer.put(encryptedKey);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.putInt(keyPairAlias.length);
        byteBuffer.put(keyPairAlias);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    public byte[] encodeSymmetricData(byte[] keyAlias, byte[] cipherText, byte[] iv) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(((Integer.SIZE / 8) * 3) + iv.length +
                keyAlias.length + cipherText.length);
        byteBuffer.putInt(SecureFileEncodingType.SYMMETRIC.getType());
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.putInt(keyAlias.length);
        byteBuffer.put(keyAlias);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    public byte[] encodeAsymmetricData(byte[] keyPairAlias, byte[] cipherText) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(((Integer.SIZE / 8) * 2) +
                keyPairAlias.length + cipherText.length);
        byteBuffer.putInt(SecureFileEncodingType.ASYMMETRIC.getType());
        byteBuffer.putInt(keyPairAlias.length);
        byteBuffer.put(keyPairAlias);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    public void decryptEncodedData(byte[] encodedCipherText, final SecureDecryptionCallback callback) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(encodedCipherText);
        int encodingTypeVal = byteBuffer.getInt();
        SecureFileEncodingType encodingType = SecureFileEncodingType.fromId(encodingTypeVal);
        byte[] encodedEphKey = null;
        byte[] iv = null;
        String keyAlias = null;
        byte[] cipherText = null;

        switch (encodingType) {
            case EPHEMERAL:
                int encodedEphKeyLength = byteBuffer.getInt();
                encodedEphKey = new byte[encodedEphKeyLength];
                byteBuffer.get(encodedEphKey);
            case SYMMETRIC:
                int ivLength = byteBuffer.getInt();
                iv = new byte[ivLength];
                byteBuffer.get(iv);
            case ASYMMETRIC:
                int keyAliasLength = byteBuffer.getInt();
                byte[] keyAliasBytes = new byte[keyAliasLength];
                byteBuffer.get(keyAliasBytes);
                keyAlias = new String(keyAliasBytes);
                cipherText = new byte[byteBuffer.remaining()];
                byteBuffer.get(cipherText);
                break;
            case NOT_ENCRYPTED:
                throw new SecurityException("Cannot determine file type.");
        }
        switch (encodingType) {
            case EPHEMERAL:
                final byte[] ephemeralCipherText = cipherText;
                final byte[] ephemeralIv = iv;
                decryptSensitiveDataAsymmetric(keyAlias, encodedEphKey, new SecureDecryptionCallback() {
                            @java.lang.Override
                            public void decryptionComplete(byte[] clearText) {
                                EphemeralSecretKey ephemeralSecretKey = new EphemeralSecretKey(clearText);
                                byte[] decrypted = decryptEphemeralData(
                                        ephemeralSecretKey,
                                        ephemeralCipherText, ephemeralIv);
                                callback.decryptionComplete(decrypted);
                                ephemeralSecretKey.destroy();
                            }
                });

                break;
            case SYMMETRIC:
                decryptSensitiveData(
                        keyAlias,
                        cipherText, iv, callback);
                break;
            case ASYMMETRIC:
                decryptSensitiveDataAsymmetric(
                        keyAlias,
                        cipherText, callback);
                break;
            case NOT_ENCRYPTED:
                throw new SecurityException("File not encrypted.");
        }
    }

    public static class Builder {

        private boolean requireUserAuth;
        private String mAlgorithm;


        public Builder(@NonNull String algorithm) {

        }

        public Builder setAlgorithm(String algorithm) {
            mAlgorithm = algorithm;
            return this;
        }

        public CipherCompat build() {
            return new CipherCompat(null);
        }
    }

}
