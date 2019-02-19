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

import android.security.keystore.KeyProperties;

import androidx.security.SecureConfig;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.GCMParameterSpec;

public class EphemeralSecretKey implements KeySpec, SecretKey {

    private static final long serialVersionUID = 7177238317307289223L;
    private static final String TAG = "EphemeralSecretKey";

    private byte[] key;

    private String algorithm;

    private boolean keyDestroyed;

    private SecureConfig secureConfig;

    public EphemeralSecretKey(byte[] key) {
        this(key, KeyProperties.KEY_ALGORITHM_AES, SecureConfig.getStrongConfig());
    }

    public EphemeralSecretKey(byte[] key, String algorithm) {
        this(key, algorithm, SecureConfig.getStrongConfig());
    }

    public EphemeralSecretKey(byte[] key, SecureConfig secureConfig) {
        this(key, KeyProperties.KEY_ALGORITHM_AES, secureConfig);
    }

    /**
     * Constructs a secret key from the given byte array.
     *
     * <p>This constructor does not check if the given bytes indeed specify a
     * secret key of the specified algorithm. For example, if the algorithm is
     * DES, this constructor does not check if <code>key</code> is 8 bytes
     * long, and also does not check for weak or semi-weak keys.
     * In order for those checks to be performed, an algorithm-specific
     * <i>key specification</i> class (in this case:
     * {@link DESKeySpec DESKeySpec})
     * should be used.
     *
     * @param key       the key material of the secret key. The contents of
     *                  the array are copied to protect against subsequent modification.
     * @param algorithm the name of the secret-key algorithm to be associated
     *                  with the given key material.
     *                  See Appendix A in the <a href=
     *                  "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/crypto/CryptoSpec.html#AppA">
     *                  Java Cryptography Architecture Reference Guide</a>
     *                  for information about standard algorithm names.
     * @throws IllegalArgumentException if <code>algorithm</code>
     *                                  is null or <code>key</code> is null or empty.
     */
    public EphemeralSecretKey(byte[] key, String algorithm, SecureConfig secureConfig) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        this.key = key;
        this.algorithm = algorithm;
        this.keyDestroyed = false;
        this.secureConfig = secureConfig;
    }

    /**
     * Constructs a secret key from the given byte array, using the first
     * <code>len</code> bytes of <code>key</code>, starting at
     * <code>offset</code> inclusive.
     *
     * <p> The bytes that constitute the secret key are
     * those between <code>key[offset]</code> and
     * <code>key[offset+len-1]</code> inclusive.
     *
     * <p>This constructor does not check if the given bytes indeed specify a
     * secret key of the specified algorithm. For example, if the algorithm is
     * DES, this constructor does not check if <code>key</code> is 8 bytes
     * long, and also does not check for weak or semi-weak keys.
     * In order for those checks to be performed, an algorithm-specific key
     * specification class (in this case:
     * {@link DESKeySpec DESKeySpec})
     * must be used.
     *
     * @param key       the key material of the secret key. The first
     *                  <code>len</code> bytes of the array beginning at
     *                  <code>offset</code> inclusive are copied to protect
     *                  against subsequent modification.
     * @param offset    the offset in <code>key</code> where the key material
     *                  starts.
     * @param len       the length of the key material.
     * @param algorithm the name of the secret-key algorithm to be associated
     *                  with the given key material.
     *                  See Appendix A in the <a href=
     *                  "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/crypto/CryptoSpec.html#AppA">
     *                  Java Cryptography Architecture Reference Guide</a>
     *                  for information about standard algorithm names.
     * @throws IllegalArgumentException       if <code>algorithm</code>
     *                                        is null or <code>key</code> is null, empty, or too short,
     *                                        i.e. {@code key.length-offset<len}.
     * @throws ArrayIndexOutOfBoundsException is thrown if
     *                                        <code>offset</code> or <code>len</code> index bytes outside the
     *                                        <code>key</code>.
     */
    public EphemeralSecretKey(byte[] key, int offset, int len, String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        if (key.length - offset < len) {
            throw new IllegalArgumentException
                    ("Invalid offset/length combination");
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        this.key = new byte[len];
        System.arraycopy(key, offset, this.key, 0, len);
        this.algorithm = algorithm;
        this.keyDestroyed = false;
    }

    /**
     * Returns the name of the algorithm associated with this secret key.
     *
     * @return the secret key algorithm.
     */
    public String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns the name of the encoding format for this secret key.
     *
     * @return the string "RAW".
     */
    public String getFormat() {
        return "RAW";
    }

    /**
     * Returns the key material of this secret key.
     *
     * @return the key material. Returns a new array
     * each time this method is called.
     */
    public byte[] getEncoded() {
        return this.key;
    }

    /**
     * Calculates a hash code value for the object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode() {
        int retval = 0;
        for (int i = 1; i < this.key.length; i++) {
            retval += this.key[i] * i;
        }
        if (this.algorithm.equalsIgnoreCase("TripleDES"))
            return (retval ^= "desede".hashCode());
        else
            return (retval ^=
                    this.algorithm.toLowerCase(Locale.ENGLISH).hashCode());
    }

    /**
     * Tests for equality between the specified object and this
     * object. Two SecretKeySpec objects are considered equal if
     * they are both SecretKey instances which have the
     * same case-insensitive algorithm name and key encoding.
     *
     * @param obj the object to test for equality with this object.
     * @return true if the objects are considered equal, false if
     * <code>obj</code> is null or otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof SecretKey))
            return false;

        String thatAlg = ((SecretKey) obj).getAlgorithm();
        if (!(thatAlg.equalsIgnoreCase(this.algorithm))) {
            if ((!(thatAlg.equalsIgnoreCase("DESede"))
                    || !(this.algorithm.equalsIgnoreCase("TripleDES")))
                    && (!(thatAlg.equalsIgnoreCase("TripleDES"))
                    || !(this.algorithm.equalsIgnoreCase("DESede"))))
                return false;
        }

        byte[] thatKey = ((SecretKey) obj).getEncoded();

        return MessageDigest.isEqual(this.key, thatKey);
    }

    @Override
    public void destroy() {
        if (!keyDestroyed) {
            Arrays.fill(key, (byte) 0);
            this.key = null;
            keyDestroyed = true;
        }
    }

    public void destroyCipherKey(Cipher cipher, int opmode) {
        try {
            byte[] blankKey = new byte[secureConfig.getSymmetricKeySize() / 8];
            byte[] iv = new byte[SecureConfig.AES_IV_SIZE_BYTES];
            Arrays.fill(blankKey, (byte) 0);
            Arrays.fill(iv, (byte) 0);
            EphemeralSecretKey blankSecretKey = new EphemeralSecretKey(blankKey, secureConfig.getSymmetricKeyAlgorithm());
            cipher.init(opmode, blankSecretKey, new GCMParameterSpec(secureConfig.getSymmetricGcmTagLength(), iv));
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Could not destroy key.");
        }
    }

    @Override
    public boolean isDestroyed() {
        return keyDestroyed;
    }
}

