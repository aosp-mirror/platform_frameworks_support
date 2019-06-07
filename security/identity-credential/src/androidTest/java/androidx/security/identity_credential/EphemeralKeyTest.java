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

package androidx.security.identity_credential;

import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.security.keystore.KeyProperties;

import androidx.test.InstrumentationRegistry;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Collection;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// TODO: For better coverage, use different ECDH and HKDF implementations in test code.
public class EphemeralKeyTest {
    private static final String TAG = "EphemeralKeyTest";

    @Test
    public void createEphemeralKey() throws IdentityCredentialException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        String credentialName = "ephemeralKeyTest";

        store.deleteCredentialByName(credentialName);
        Collection<X509Certificate> certChain = ProvisioningTest.createCredential(store,
                credentialName);
        IdentityCredential credential = store.getCredentialByName(credentialName);
        assertNotNull(credential);

        // Check we can get both the public and private keys.
        KeyPair ephemeralKeyPair = credential.createEphemeralKeyPair();
        assertNotNull(ephemeralKeyPair);
        assertTrue(ephemeralKeyPair.getPublic().getEncoded().length > 0);
        assertTrue(ephemeralKeyPair.getPrivate().getEncoded().length > 0);

        TestReader reader = new TestReader(
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256,
                ephemeralKeyPair.getPublic());

        credential.setReaderEphemeralPublicKey(reader.getEphemeralPublicKey());

        // Exchange a couple of messages... this is to test that the nonce/counter
        // state works as expected.
        for (int n = 0; n < 5; n++) {
            // First send a message from the Reader to the Holder...
            byte[] messageToHolder = ("Hello Holder! (serial=" + n + ")").getBytes();
            byte[] encryptedMessageToHolder = reader.encryptMessageToHolder(messageToHolder);
            assertNotEquals(messageToHolder, encryptedMessageToHolder);
            byte[] decryptedMessageToHolder = credential.decryptMessageFromReader(
                    encryptedMessageToHolder);
            assertArrayEquals(messageToHolder, decryptedMessageToHolder);

            // Then from the Holder to the Reader...
            byte[] messageToReader = ("Hello Reader! (serial=" + n + ")").getBytes();
            byte[] encryptedMessageToReader = credential.encryptMessageToReader(messageToReader);
            assertNotEquals(messageToReader, encryptedMessageToReader);
            byte[] decryptedMessageToReader = reader.decryptMessageFromHolder(
                    encryptedMessageToReader);
            assertArrayEquals(messageToReader, decryptedMessageToReader);
        }
    }

    class TestReader {

        @IdentityCredentialStore.Ciphersuite
        private int mCipherSuite;

        private PublicKey mHolderEphemeralPublicKey;
        private KeyPair mEphemeralKeyPair;
        private SecretKey mSecretKey;
        private int mCounter;

        private SecureRandom mSecureRandom;

        private boolean mRemoteIsReaderDevice;

        // This is basically the reader-side of what needs to happen for encryption/decryption
        // of messages.. could easily be re-used in an mDL reader application.
        TestReader(@IdentityCredentialStore.Ciphersuite int cipherSuite,
                PublicKey holderEphemeralPublicKey) throws IdentityCredentialException {
            mCipherSuite = cipherSuite;
            mHolderEphemeralPublicKey = holderEphemeralPublicKey;
            mCounter = 1;

            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
                kpg.initialize(ecSpec);
                mEphemeralKeyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error generating ephemeral key", e);
            }

            try {
                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init(mEphemeralKeyPair.getPrivate());
                ka.doPhase(mHolderEphemeralPublicKey, true);
                byte[] sharedSecret = ka.generateSecret();

                // TODO: paulcrowley@ said to use non-null info
                byte[] salt = new byte[0];
                byte[] info = new byte[0];
                byte[] derivedKey = Util.computeHkdf("HmacSha256", sharedSecret, salt, info, 32);

                mSecretKey = new SecretKeySpec(derivedKey, "AES");

                mSecureRandom = new SecureRandom();

            } catch (InvalidKeyException |
                    NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error performing key agreement", e);
            }
        }

        PublicKey getEphemeralPublicKey() {
            return mEphemeralKeyPair.getPublic();
        }

        byte[] encryptMessageToHolder(byte[] messagePlaintext) throws IdentityCredentialException {
            byte[] messageCiphertext = null;
            try {
                ByteBuffer iv = ByteBuffer.allocate(12);
                iv.putInt(8, mCounter);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec encryptionParameterSpec = new GCMParameterSpec(128, iv.array());
                cipher.init(Cipher.ENCRYPT_MODE, mSecretKey, encryptionParameterSpec);
                messageCiphertext = cipher.doFinal(messagePlaintext); // This includes the auth tag
            } catch (BadPaddingException |
                    IllegalBlockSizeException |
                    NoSuchPaddingException |
                    InvalidKeyException |
                    NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error encrypting message", e);
            }
            mCounter += 2;
            return messageCiphertext;
        }

        byte[] decryptMessageFromHolder(byte[] messageCiphertext)
                throws IdentityCredentialException {
            int expectedCounter = mCounter - 1;
            ByteBuffer iv = ByteBuffer.allocate(12);
            iv.putInt(8, expectedCounter);
            byte[] plaintext = null;
            try {
                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, mSecretKey, new GCMParameterSpec(128, iv.array()));
                plaintext = cipher.doFinal(messageCiphertext);
            } catch (BadPaddingException |
                    IllegalBlockSizeException |
                    InvalidAlgorithmParameterException |
                    InvalidKeyException |
                    NoSuchAlgorithmException |
                    NoSuchPaddingException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error decrypting message", e);
            }
            return plaintext;
        }
    }
}
