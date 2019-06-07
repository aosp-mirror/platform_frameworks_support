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

import android.annotation.SuppressLint;
import android.content.Context;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;

import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Collection;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnicodeString;

class SoftwareIdentityCredential extends IdentityCredential {

    // Class used for unit-tests in UserAuthTest
    static abstract class InternalUserAuthenticationOracle {
        abstract boolean checkUserAuthentication(int accessControlProfileId);
    }

    private static final String TAG = "SoftwareIdentityCredential";

    private String mCredentialName;

    private Context mContext;

    private String mDocType;
    private int mCipherSuite;

    private CredentialData mData;

    private KeyPair mEphemeralKeyPair = null;

    private SecretKey mSecretKey = null;

    private SecureRandom mSecureRandom = null;

    private int mEphemeralCounter;


    /**
     * @hide
     */
    SoftwareIdentityCredential(Context context, String credentialName) throws IdentityCredentialException {
        mContext = context;
        mCredentialName = credentialName;
        mData = CredentialData.loadCredentialData(context, mCredentialName);
    }

    static byte[] delete(Context context, String credentialName) throws IdentityCredentialException {
        return CredentialData.delete(context, credentialName);
    }

    @SuppressLint("NewApi")
    @Override
    public @NonNull KeyPair createEphemeralKeyPair() throws IdentityCredentialException {
        if (mEphemeralKeyPair == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
                kpg.initialize(ecSpec);
                mEphemeralKeyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error generating ephemeral key");
            }
        }
        return mEphemeralKeyPair;
    }

    @Override
    public void setReaderEphemeralPublicKey(@NonNull PublicKey readerEphemeralPublicKey) throws IdentityCredentialException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(mEphemeralKeyPair.getPrivate());
            ka.doPhase(readerEphemeralPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();

            // TODO: paulcrowley@ said to use non-null info
            byte[] salt = new byte[0];
            byte[] info = new byte[0];
            byte[] derivedKey = Util.computeHkdf("HmacSha256", sharedSecret, salt, info, 32);

            mSecretKey = new SecretKeySpec(derivedKey, "AES");

            mSecureRandom = new SecureRandom();

            mEphemeralCounter = 2;

        } catch (InvalidKeyException |
                NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error performing key agreement");
        }
    }

    @Override
    public @NonNull byte[] encryptMessageToReader(@NonNull byte[] messagePlaintext) throws IdentityCredentialException {
        byte[] messageCiphertext = null;
        try {
            ByteBuffer iv = ByteBuffer.allocate(12);
            iv.putInt(8, mEphemeralCounter);
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
            throw new IdentityCredentialException("Error encrypting message");
        }
        mEphemeralCounter += 2;
        return messageCiphertext;
    }

    @Override
    public @NonNull byte[] decryptMessageFromReader(@NonNull byte[] messageCiphertext) throws IdentityCredentialException {
        int expectedCounter = mEphemeralCounter - 1;
        ByteBuffer iv = ByteBuffer.allocate(12);
        iv.putInt(8, expectedCounter);
        byte[] plainText = null;
        try {
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, mSecretKey, new GCMParameterSpec(128, iv.array()));
            plainText = cipher.doFinal(messageCiphertext);
        } catch (BadPaddingException |
                 IllegalBlockSizeException |
                 InvalidAlgorithmParameterException |
                 InvalidKeyException |
                 NoSuchAlgorithmException |
                 NoSuchPaddingException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error decrypting message");
        }
        return plainText;
    }

    @Override
    public @NonNull Collection<X509Certificate> getCredentialKeyCertificateChain() {
        return mData.getCredentialKeyCertificateChain();
    }

    private boolean mAllowUsingExhaustedKeys = false;

    @Override
    public void setAllowUsingExhaustedKeys(boolean allowUsingExhaustedKeys) {
        mAllowUsingExhaustedKeys = true;
    }


    private byte[] mAuthKeyAssociatedData = null;
    private PrivateKey mAuthKey = null;
    private Signature mAuthKeySignature = null;
    private BiometricPrompt.CryptoObject mAuthKeyCryptoObject = null;

    private void ensureAuthKey() throws IdentityCredentialException {
        if (mAuthKey != null) {
            return;
        }

        Pair<PrivateKey, byte[]> keyAndStaticData = mData.selectAuthenticationKey(mAllowUsingExhaustedKeys);
        if (keyAndStaticData == null) {
            throw new NoAuthenticationKeyAvailableException("No authentication key available for signing");
        }
        mAuthKey = keyAndStaticData.first;
        mAuthKeyAssociatedData = keyAndStaticData.second;

        try {
            mAuthKeySignature = Signature.getInstance("SHA256withECDSA");
            mAuthKeySignature.initSign(mAuthKey);
        } catch (NoSuchAlgorithmException |
                 InvalidKeyException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error initializing key for DeviceAuthentication signature");
        }
        mAuthKeyCryptoObject = new BiometricPrompt.CryptoObject(mAuthKeySignature);
    }

    @Override
    public BiometricPrompt.CryptoObject getCryptoObject() throws IdentityCredentialException {
        ensureAuthKey();
        return mAuthKeyCryptoObject;
    }

    private boolean mCannotCallGetEntriesAgain = false;

    @Override
    public GetEntryResult getEntries(@NonNull Collection<RequestNamespace> requestedEntryNamespaces,
                                     @Nullable byte[] requestMessage,
                                     @Nullable byte[] sessionTranscript,
                                     @Nullable Collection<X509Certificate> readerCertificateChain,
                                     @Nullable byte[] readerSignature) throws IdentityCredentialException {
        if (mCannotCallGetEntriesAgain) {
            throw new AlreadyCalledException("getEntries() has already successfully " +
                    "completed, it's not possible to use this instance for more than one call.");
        }

        if (readerCertificateChain != null) {
            if (!Util.validateCertificateChain(readerCertificateChain)) {
                throw new InvalidReaderCertificateChainException();
            }
        }

        // Check reader signature, if requested.
        if (readerSignature != null) {
            if (readerCertificateChain == null) {
                throw new IdentityCredentialException("readerSignature non-null but readerCertificateChain was null");
            }
            if (sessionTranscript == null) {
                throw new IdentityCredentialException("readerSignature non-null but sessionTranscript was null");
            }
            if (requestMessage == null) {
                throw new IdentityCredentialException("readerSignature non-null but requestMessage was null");
            }

            ByteArrayOutputStream raBaos = new ByteArrayOutputStream();
            try {
                new CborEncoder(raBaos).encode(new CborBuilder()
                        .addArray()
                        .add("ReaderAuthentication")
                        .add(sessionTranscript)
                        .add(requestMessage)
                        .end()
                        .build());
            } catch (CborException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error creating DeviceAuthentication CBOR");
            }
            byte[] dataThatWasSigned = raBaos.toByteArray();
            try {
                Signature verifier = Signature.getInstance("SHA256withECDSA");
                verifier.initVerify(readerCertificateChain.iterator().next());
                verifier.update(dataThatWasSigned);
                if (!verifier.verify(readerSignature)) {
                    throw new IdentityCredentialException("Error verifying readerSignature");
                }
            } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error verifying readerSignature");
            }
        }

        GetEntryResult result = new GetEntryResult();

        // TODO: parse requestMessage() and check that requestedEntryNamespaces is a subset of the request there

        // TODO: perform access checks (user auth and reader auth).

        CborBuilder deviceNameSpaceBuilder = new CborBuilder();
        MapBuilder<CborBuilder> deviceNameSpacesMapBuilder = deviceNameSpaceBuilder.addMap();

        // Generate the resulting DeviceNamespaces CBOR.
        //
        // This operation is O(n^2) in number of namespaces... but n is likely small. Possibly
        // replace with something faster.
        for (RequestNamespace requestedEntryNamespace : requestedEntryNamespaces) {
            for (EntryNamespace loadedNamespace : mData.getEntryNamespaces()) {
                MapBuilder<MapBuilder<CborBuilder>> deviceNamespaceBuilder = null;

                if (requestedEntryNamespace.getNamespaceName().equals(loadedNamespace.getNamespaceName())) {
                    ResultNamespace.Builder resultNamespaceBuilder = null;

                    for (String requestedEntryName : requestedEntryNamespace.getEntryNames()) {
                        Object value = loadedNamespace.getEntryValue(requestedEntryName);
                        if (value != null) {
                            String namespaceName = requestedEntryNamespace.getNamespaceName();
                            if (resultNamespaceBuilder == null) {
                                resultNamespaceBuilder = new ResultNamespace.Builder(namespaceName);
                            }

                            if (deviceNamespaceBuilder == null) {
                                deviceNamespaceBuilder = deviceNameSpacesMapBuilder.putMap(namespaceName);
                            }

                            Collection<Integer> accessControlProfileIds = loadedNamespace.getAccessControlProfileIds(requestedEntryName);

                            @ResultNamespace.Status
                            int status = checkAccess(accessControlProfileIds, readerCertificateChain);

                            if (status != ResultNamespace.STATUS_OK) {
                                resultNamespaceBuilder.addErrorStatus(requestedEntryName, status);
                            } else {
                                resultNamespaceBuilder.addEntry(requestedEntryName, value);

                                DataItem valueDataItem = Util.valueToCbor(value);
                                if (valueDataItem == null) {
                                    throw new IdentityCredentialException("Unexpected type for value");
                                }
                                deviceNamespaceBuilder.put(new UnicodeString(requestedEntryName), valueDataItem);
                            }
                        }
                    }
                    if (resultNamespaceBuilder != null) {
                        result.getEntryNamespaces().add(resultNamespaceBuilder.build());
                    }
                }
            }
        }
        ByteArrayOutputStream adBaos = new ByteArrayOutputStream();
        CborEncoder adEncoder = new CborEncoder(adBaos);
        DataItem deviceNameSpace = deviceNameSpaceBuilder.build().get(0);
        try {
            adEncoder.encode(deviceNameSpace);
        } catch (CborException e) {
            e.printStackTrace();
        }
        result.mAuthenticatedData = adBaos.toByteArray();

        // If the sessionTranscript is available, create the ECDSA signature
        // so the reader can authenticate the DeviceNamespaces CBOR. Also
        // return the staticAuthenticationData associated with the key chosen
        // to be used for signing.
        //
        // Unfortunately we can't do MACing because Android Keystore doesn't
        // implement ECDH. So we resort to ECSDA signing instead.
        if (sessionTranscript != null) {
            ensureAuthKey();
            result.mStaticAuthenticationData = mAuthKeyAssociatedData;

            ByteArrayOutputStream daBaos = new ByteArrayOutputStream();
            try {
                new CborEncoder(daBaos).encode(new CborBuilder()
                        .addArray()
                        .add("DeviceAuthentication")
                        .add(sessionTranscript)
                        .add(mData.getDocType())
                        .add(deviceNameSpace)
                        .end()
                        .build());
            } catch (CborException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error creating DeviceAuthentication CBOR");
            }
            byte[] deviceAuthentication = daBaos.toByteArray();

            try {
                mAuthKeySignature.update(deviceAuthentication);
                result.mEcdsaSignature = mAuthKeySignature.sign();
            } catch (SignatureException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error signing DeviceAuthentication CBOR");
            }
        }

        mCannotCallGetEntriesAgain = true;

        return result;
    }

    private String byteArrayToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder("{");
        for (byte b : bytes) {
            builder.append(String.format("0x%02x, ", b));
        }
        builder.append("}");
        return builder.toString();
    }

    @ResultNamespace.Status
    private int checkAccessSingleProfile(AccessControlProfile profile,
                                         Collection<X509Certificate> readerCertificateChain) throws IdentityCredentialException {

        if (profile.isUserAuthenticationRequired()) {
            if (mInternalUserAuthenticationOracle != null) {
                if (!mInternalUserAuthenticationOracle.checkUserAuthentication(profile.getAccessControlProfileId())) {
                    return ResultNamespace.STATUS_USER_AUTHENTICATION_FAILED;
                }
            } else {
                if (!mData.checkUserAuthentication(profile.getAccessControlProfileId())) {
                    return ResultNamespace.STATUS_USER_AUTHENTICATION_FAILED;
                }
            }
        }

        X509Certificate profileCert = profile.getReaderCertificate();
        if (profileCert != null) {
            if (readerCertificateChain == null) {
                return ResultNamespace.STATUS_READER_AUTHENTICATION_FAILED;
            }

            // Need to check if the cert required by the profile is in the given chain.
            boolean foundMatchingCert = false;
            byte[] profilePublicKeyEncoded = profileCert.getPublicKey().getEncoded();
            for (X509Certificate readerCert : readerCertificateChain) {
                byte[] readerCertPublicKeyEncoded = readerCert.getPublicKey().getEncoded();
                if (Arrays.equals(profilePublicKeyEncoded, readerCertPublicKeyEncoded)) {
                    foundMatchingCert = true;
                    break;
                }
            }
            if (!foundMatchingCert) {
                return ResultNamespace.STATUS_READER_AUTHENTICATION_FAILED;
            }
        }

        // Neither user auth nor reader auth required. This means access is always granted.
        return ResultNamespace.STATUS_OK;
    }

    @ResultNamespace.Status
    private int checkAccess(Collection<Integer> accessControlProfileIds,
                                Collection<X509Certificate> readerCertificateChain) throws IdentityCredentialException {
        // Access is granted if at least one of the profiles grants access.
        //
        // If an item is configured without any profiles, access is denied.
        //
        @ResultNamespace.Status int lastStatus = ResultNamespace.STATUS_NO_ACCESS_CONTROL_PROFILES;

        for (int id : accessControlProfileIds) {
            AccessControlProfile profile = mData.getAccessControlProfile(id);
            lastStatus = checkAccessSingleProfile(profile, readerCertificateChain);
            if (lastStatus == ResultNamespace.STATUS_OK) {
                return lastStatus;
            }
        }
        return lastStatus;
    }

    @Override
    public void setAvailableAuthenticationKeys(int keyCount,
                                               int maxUsesPerKey) throws IdentityCredentialException {
        mData.setAvailableAuthenticationKeys(keyCount, maxUsesPerKey);
    }

    @Override
    public @NonNull Collection<X509Certificate> getAuthKeysNeedingCertification() throws IdentityCredentialException {
        return mData.getAuthKeysNeedingCertification();
    }

    @Override
    public void storeStaticAuthenticationData(X509Certificate authenticationKey,
                                              byte[] staticAuthData)
            throws IdentityCredentialException {
        mData.storeStaticAuthenticationData(authenticationKey, staticAuthData);
    }

    @Override
    public @NonNull int[] getAuthenticationDataUsageCount() throws IdentityCredentialException {
        return mData.getAuthKeyUseCounts();
    }

    InternalUserAuthenticationOracle mInternalUserAuthenticationOracle = null;

    void setInternalUserAuthenticationOracle(InternalUserAuthenticationOracle oracle) {
        mInternalUserAuthenticationOracle = oracle;
    }
}