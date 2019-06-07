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

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnicodeString;


class SoftwareWritableIdentityCredential extends WritableIdentityCredential {

    private static final String TAG = "SoftwareWritableIdentityCredential";

    private KeyPair mKeyPair = null;
    private Collection<X509Certificate> mCertificates = null;

    /**
     * Generates CredentialKey.
     *
     * If called a second time on the same object, does nothing and returns null.
     *
     * @param challenge The attestation challenge.
     *
     * @return Attestation mCertificate chain or null if called a second time.
     *
     * @throws AlreadyPersonalizedException
     *             if this credential has already been personalized.
     *
     * @throws CipherSuiteNotSupportedException
     *             if the cipher suite is not supported
     *
     * @throws IdentityCredentialException
     *             if unable to communicate with secure hardware.
     */
    private Collection<X509Certificate> ensureCredentialKey(byte[] challenge) throws IdentityCredentialException {

        if (mKeyPair != null) {
            return null;
        }

        String aliasForCredential = CredentialData.getAliasFromCredentialName(mCredentialName);

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                if (alias.equals(aliasForCredential)) {
                    ks.deleteEntry(aliasForCredential);
                }
            }

            // TODO: tweak validity
            Calendar calendar = Calendar.getInstance();
            Date validityStart = calendar.getTime();
            calendar.add(Calendar.YEAR, 1);
            Date validityEnd = calendar.getTime();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    aliasForCredential,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setKeyValidityStart(validityStart)
                    .setKeyValidityEnd(validityEnd);
            // Attestation is only available in Nougat and onwards.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (challenge != null) {
                    builder.setAttestationChallenge(challenge);
                } else {
                    byte[] dummyChallenge = new byte[0];
                    builder.setAttestationChallenge(dummyChallenge);
                }
            }
            kpg.initialize(builder.build());
            mKeyPair = kpg.generateKeyPair();

            Certificate[] certificates = ks.getCertificateChain(aliasForCredential);
            mCertificates = new LinkedList<>();
            for (Certificate certificate : certificates) {
                mCertificates.add((X509Certificate) certificate);
            }
        } catch (InvalidAlgorithmParameterException |
                NoSuchAlgorithmException |
                NoSuchProviderException |
                CertificateException |
                KeyStoreException |
                IOException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error creating CredentialKey");
        }
        return mCertificates;
    }

    @Override
    public @NonNull
    Collection<X509Certificate> getCredentialKeyCertificateChain(
            @NonNull byte[] challenge) throws IdentityCredentialException {
        Collection<X509Certificate> certificates = ensureCredentialKey(challenge);
        if (certificates == null) {
            throw new IdentityCredentialException("getCredentialKeyCertificateChain() must be called before personalize()");
        }
        return certificates;
    }

    private String mDocType;
    private String mCredentialName;
    private @IdentityCredentialStore.Ciphersuite int mCipherSuite;
    private Context mContext;

    SoftwareWritableIdentityCredential(Context context,
                                       @NonNull String credentialName,
                                       @NonNull String docType,
                                       @IdentityCredentialStore.Ciphersuite int cipherSuite) throws CipherSuiteNotSupportedException {
        if (cipherSuite != IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256) {
            throw new CipherSuiteNotSupportedException("Cipher suite not supported");
        }

        mContext = context;
        mDocType = docType;
        mCredentialName = credentialName;
        mCipherSuite = cipherSuite;
    }

    @Override
    public byte[] personalize(Collection<AccessControlProfile> accessControlProfiles,
                              Collection<EntryNamespace> entryNamespaces) throws IdentityCredentialException {

        ensureCredentialKey(null);

        CborBuilder accessControlProfileBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> arrayBuilder = accessControlProfileBuilder.addArray();
        for (AccessControlProfile profile : accessControlProfiles) {
            arrayBuilder.add(Util.accessControlProfileToCbor(profile));
        }

        CborBuilder dataBuilder = new CborBuilder();
        MapBuilder<CborBuilder> dataMapBuilder = dataBuilder.addMap();
        for (EntryNamespace entryNamespace : entryNamespaces) {
            dataMapBuilder.put(
                    new UnicodeString(entryNamespace.getNamespaceName()),
                    Util.entryNamespaceToCbor(entryNamespace));
        }

        CborBuilder signedDataBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> signedDataArrayBuilder = signedDataBuilder.addArray()
                .add(mDocType)
                .add(accessControlProfileBuilder.build().get(0))
                .add(dataBuilder.build().get(0))
                .add(false);

        Signature s = null;
        try {
            s = Signature.getInstance("SHA256withECDSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            s.initSign(mKeyPair.getPrivate());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        DataItem dataToSignItem = signedDataBuilder.build().get(0);

        ByteArrayOutputStream dtsBaos = new ByteArrayOutputStream();
        CborEncoder dtsEncoder = new CborEncoder(dtsBaos);
        byte[] dataToSign = new byte[0];
        try {
            dtsEncoder.encode(dataToSignItem);
        } catch (CborException e) {
            e.printStackTrace();
        }
        dataToSign = dtsBaos.toByteArray();
        byte[] signature = new byte[0];
        try {
            s.update(dataToSign);
            signature = s.sign();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        CborBuilder proofOfProvisioningBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> proofOfProvisioningArrayBuilder = proofOfProvisioningBuilder.addArray();
        proofOfProvisioningArrayBuilder.add(dataToSignItem).add(signature);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(baos);
        try {
            encoder.encode(proofOfProvisioningBuilder.build());
        } catch (CborException e) {
            e.printStackTrace();
        }
        byte[] encodedBytes = baos.toByteArray();

        CredentialData data = CredentialData.createCredentialData(
                mContext,
                mDocType,
                mCipherSuite,
                mCredentialName,
                CredentialData.getAliasFromCredentialName(mCredentialName),
                mCertificates,
                accessControlProfiles,
                entryNamespaces);

        return encodedBytes;
    }

}
