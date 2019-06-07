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
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;

import java.io.ByteArrayInputStream;
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
import java.util.HashMap;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;

class SoftwareIdentityCredential extends IdentityCredential {

    private static final String TAG = "SoftwareIdentityCredential";
    InternalUserAuthenticationOracle mInternalUserAuthenticationOracle = null;
    private String mCredentialName;

    private Context mContext;

    private String mDocType;
    private int mCipherSuite;

    private CredentialData mData;

    private KeyPair mEphemeralKeyPair = null;

    private SecretKey mSecretKey = null;

    private SecureRandom mSecureRandom = null;

    private int mEphemeralCounter;
    private boolean mAllowUsingExhaustedKeys = true;
    private byte[] mAuthKeyAssociatedData = null;
    private PrivateKey mAuthKey = null;
    private Signature mAuthKeySignature = null;
    private BiometricPrompt.CryptoObject mAuthKeyCryptoObject = null;
    private boolean mCannotCallGetEntriesAgain = false;

    /**
     * @hide
     */
    SoftwareIdentityCredential(Context context, String credentialName)
            throws IdentityCredentialException {
        mContext = context;
        mCredentialName = credentialName;
        mData = CredentialData.loadCredentialData(context, mCredentialName);
    }

    static byte[] delete(Context context, String credentialName)
            throws IdentityCredentialException {
        return CredentialData.delete(context, credentialName);
    }

    // This only extracts the requested namespaces, not DocType or RequestInfo. We
    // can do this later if it's needed.
    private static HashMap<String, RequestNamespace> parseRequestMessage(
            @Nullable byte[] requestMessage)
            throws IdentityCredentialException {
        HashMap<String, RequestNamespace> result = new HashMap<>();

        if (requestMessage == null) {
            return result;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(requestMessage);
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new IdentityCredentialException("Expected 1 item, found " + dataItems.size());
            }
            if (!(dataItems.get(0) instanceof Map)) {
                throw new IdentityCredentialException("Item is not a map");
            }
            Map map = (Map) dataItems.get(0);

            DataItem nameSpaces = map.get(new UnicodeString("NameSpaces"));
            if (!(nameSpaces instanceof Map)) {
                throw new IdentityCredentialException(
                        "NameSpaces entry not found or not map");
            }

            for (DataItem keyItem : ((Map) nameSpaces).getKeys()) {
                if (!(keyItem instanceof UnicodeString)) {
                    throw new IdentityCredentialException(
                            "Key item in NameSpaces map not UnicodeString");
                }
                String nameSpace = ((UnicodeString) keyItem).getString();
                RequestNamespace.Builder builder = new RequestNamespace.Builder(nameSpace);

                DataItem valueItem = ((Map) nameSpaces).get(keyItem);
                if (!(valueItem instanceof Array)) {
                    throw new IdentityCredentialException(
                            "Value item in NameSpaces map not Array");
                }
                for (DataItem item : ((Array) valueItem).getDataItems()) {
                    if (!(item instanceof UnicodeString)) {
                        throw new IdentityCredentialException(
                                "Item in nameSpaces array not UnicodeString");
                    }
                    builder.addEntryName(((UnicodeString) item).getString());
                }
                result.put(nameSpace, builder.build());
            }

        } catch (CborException e) {
            throw new IdentityCredentialException("Error decoding request message", e);
        }
        return result;
    }

    @SuppressLint("NewApi")
    @Override
    public @NonNull
    KeyPair createEphemeralKeyPair() throws IdentityCredentialException {
        if (mEphemeralKeyPair == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
                kpg.initialize(ecSpec);
                mEphemeralKeyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException e) {
                throw new IdentityCredentialException("Error generating ephemeral key", e);
            }
        }
        return mEphemeralKeyPair;
    }

    @Override
    public void setReaderEphemeralPublicKey(@NonNull PublicKey readerEphemeralPublicKey)
            throws IdentityCredentialException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(mEphemeralKeyPair.getPrivate());
            ka.doPhase(readerEphemeralPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();

            byte[] salt = new byte[0];
            byte[] info = new byte[0];
            byte[] derivedKey = Util.computeHkdf("HmacSha256", sharedSecret, salt, info, 32);

            mSecretKey = new SecretKeySpec(derivedKey, "AES");

            mSecureRandom = new SecureRandom();

            mEphemeralCounter = 2;

        } catch (InvalidKeyException |
                NoSuchAlgorithmException e) {
            throw new IdentityCredentialException("Error performing key agreement", e);
        }
    }

    @Override
    public @NonNull
    byte[] encryptMessageToReader(@NonNull byte[] messagePlaintext)
            throws IdentityCredentialException {
        byte[] messageCiphertextAndAuthTag = null;
        try {
            ByteBuffer iv = ByteBuffer.allocate(12);
            iv.putInt(8, mEphemeralCounter);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec encryptionParameterSpec = new GCMParameterSpec(128, iv.array());
            cipher.init(Cipher.ENCRYPT_MODE, mSecretKey, encryptionParameterSpec);
            messageCiphertextAndAuthTag = cipher.doFinal(messagePlaintext);
        } catch (BadPaddingException |
                IllegalBlockSizeException |
                NoSuchPaddingException |
                InvalidKeyException |
                NoSuchAlgorithmException |
                InvalidAlgorithmParameterException e) {
            throw new IdentityCredentialException("Error encrypting message", e);
        }
        mEphemeralCounter += 2;
        return messageCiphertextAndAuthTag;
    }

    @Override
    public @NonNull
    byte[] decryptMessageFromReader(@NonNull byte[] messageCiphertext)
            throws IdentityCredentialException {
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
            throw new IdentityCredentialException("Error decrypting message", e);
        }
        return plainText;
    }

    @Override
    public @NonNull
    Collection<X509Certificate> getCredentialKeyCertificateChain() {
        return mData.getCredentialKeyCertificateChain();
    }

    @Override
    public void setAllowUsingExhaustedKeys(boolean allowUsingExhaustedKeys) {
        mAllowUsingExhaustedKeys = allowUsingExhaustedKeys;
    }

    private void ensureAuthKey() throws IdentityCredentialException {
        if (mAuthKey != null) {
            return;
        }

        Pair<PrivateKey, byte[]> keyAndStaticData = mData.selectAuthenticationKey(
                mAllowUsingExhaustedKeys);
        if (keyAndStaticData == null) {
            throw new NoAuthenticationKeyAvailableException(
                    "No authentication key available for signing");
        }
        mAuthKey = keyAndStaticData.first;
        mAuthKeyAssociatedData = keyAndStaticData.second;

        try {
            mAuthKeySignature = Signature.getInstance("SHA256withECDSA");
            mAuthKeySignature.initSign(mAuthKey);
        } catch (NoSuchAlgorithmException |
                InvalidKeyException e) {
            throw new IdentityCredentialException(
                    "Error initializing key for DeviceAuthentication signature", e);
        }
        mAuthKeyCryptoObject = new BiometricPrompt.CryptoObject(mAuthKeySignature);
    }

    @Override
    public BiometricPrompt.CryptoObject getCryptoObject() throws IdentityCredentialException {
        ensureAuthKey();
        return mAuthKeyCryptoObject;
    }

    @Override
    public GetEntryResult getEntries(
            @Nullable byte[] requestMessage,
            @NonNull Collection<RequestNamespace> entriesToRequest,
            @Nullable byte[] sessionTranscript,
            @Nullable Collection<X509Certificate> readerCertificateChain,
            @Nullable byte[] readerSignature) throws IdentityCredentialException {
        if (mCannotCallGetEntriesAgain) {
            throw new AlreadyCalledException("getEntries() has already successfully " +
                    "completed, it's not possible to use this instance for more than one call.");
        }

        if (readerCertificateChain != null) {
            if (!Util.validateCertificateChain(readerCertificateChain)) {
                throw new InvalidReaderCertificateChainException("Failed to validate the " +
                        "certificate chain of the reader.");
            }
        }

        HashMap<String, RequestNamespace> requestMessageMap = parseRequestMessage(requestMessage);

        // Check reader signature, if requested.
        if (readerSignature != null) {
            if (readerCertificateChain == null) {
                throw new IdentityCredentialException(
                        "readerSignature non-null but readerCertificateChain was null");
            }
            if (sessionTranscript == null) {
                throw new IdentityCredentialException(
                        "readerSignature non-null but sessionTranscript was null");
            }
            if (requestMessage == null) {
                throw new IdentityCredentialException(
                        "readerSignature non-null but requestMessage was null");
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
                throw new IdentityCredentialException("Error creating DeviceAuthentication CBOR",
                        e);
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
                throw new IdentityCredentialException("Error verifying readerSignature", e);
            }
        }

        GetEntryResult result = new GetEntryResult();

        CborBuilder deviceNameSpaceBuilder = new CborBuilder();
        MapBuilder<CborBuilder> deviceNameSpacesMapBuilder = deviceNameSpaceBuilder.addMap();


        retrieveValues(readerCertificateChain,
                requestMessageMap,
                entriesToRequest,
                result,
                deviceNameSpacesMapBuilder);

        ByteArrayOutputStream adBaos = new ByteArrayOutputStream();
        CborEncoder adEncoder = new CborEncoder(adBaos);
        DataItem deviceNameSpace = deviceNameSpaceBuilder.build().get(0);
        try {
            adEncoder.encode(deviceNameSpace);
        } catch (CborException e) {
            throw new IdentityCredentialException("Error encoding deviceNameSpace", e);
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
                throw new IdentityCredentialException("Error creating DeviceAuthentication CBOR",
                        e);
            }
            byte[] deviceAuthentication = daBaos.toByteArray();

            try {
                mAuthKeySignature.update(deviceAuthentication);
                result.mEcdsaSignature = mAuthKeySignature.sign();
            } catch (SignatureException e) {
                throw new IdentityCredentialException("Error signing DeviceAuthentication CBOR", e);
            }
        }

        mCannotCallGetEntriesAgain = true;

        return result;
    }

    private void retrieveValues(
            @Nullable Collection<X509Certificate> readerCertificateChain,
            HashMap<String, RequestNamespace> requestMessageMap,
            Collection<RequestNamespace> entriesToRequest,
            GetEntryResult result,
            MapBuilder<CborBuilder> deviceNameSpacesMapBuilder)
            throws IdentityCredentialException {

        for (RequestNamespace requestedEntryNamespace : entriesToRequest) {
            String namespaceName = requestedEntryNamespace.getNamespaceName();

            EntryNamespace loadedNamespace = mData.lookupEntryNamespace(namespaceName);

            RequestNamespace requestMessageNamespace = requestMessageMap.get(namespaceName);

            retrieveValuesForNamespace(readerCertificateChain, result, deviceNameSpacesMapBuilder,
                    requestedEntryNamespace,
                    requestMessageNamespace,
                    namespaceName,
                    loadedNamespace);
        }
    }

    private void retrieveValuesForNamespace(
            @Nullable Collection<X509Certificate> readerCertificateChain,
            GetEntryResult result,
            MapBuilder<CborBuilder> deviceNameSpacesMapBuilder,
            @NonNull RequestNamespace requestedEntryNamespace,
            @Nullable RequestNamespace requestMessageNamespace,
            String namespaceName,
            @Nullable EntryNamespace loadedNamespace) throws IdentityCredentialException {
        MapBuilder<MapBuilder<CborBuilder>> deviceNamespaceBuilder = null;
        ResultNamespace.Builder resultNamespaceBuilder = null;

        for (Pair<String, Boolean> requestedEntry : requestedEntryNamespace.getEntries()) {
            String requestedEntryName = requestedEntry.first;
            boolean includeInDeviceSigned = requestedEntry.second;

            if (resultNamespaceBuilder == null) {
                resultNamespaceBuilder = new ResultNamespace.Builder(namespaceName);
            }

            byte[] value = null;
            if (loadedNamespace != null) {
                value = loadedNamespace.getEntryValue(requestedEntryName);
            }

            if (value == null) {
                resultNamespaceBuilder.addErrorStatus(requestedEntryName,
                        ResultNamespace.STATUS_NO_SUCH_ENTRY);
            } else {

                Collection<Integer> accessControlProfileIds =
                        loadedNamespace.getAccessControlProfileIds(requestedEntryName);

                boolean isInRequestMessage = false;
                if (requestMessageNamespace != null) {
                    isInRequestMessage = requestMessageNamespace.hasEntry(requestedEntryName);
                }

                @ResultNamespace.Status
                int status = checkAccess(accessControlProfileIds,
                        readerCertificateChain,
                        isInRequestMessage);

                if (status != ResultNamespace.STATUS_OK) {
                    resultNamespaceBuilder.addErrorStatus(requestedEntryName, status);
                } else {

                    resultNamespaceBuilder.addEntry(requestedEntryName, value);

                    if (includeInDeviceSigned) {
                        if (deviceNamespaceBuilder == null) {
                            deviceNamespaceBuilder = deviceNameSpacesMapBuilder.putMap(
                                    namespaceName);
                        }

                        DataItem dataItem = Util.cborToDataItem(value);
                        deviceNamespaceBuilder.put(new UnicodeString(requestedEntryName), dataItem);
                    }
                }
            }
        }
        if (resultNamespaceBuilder != null) {
            result.getEntryNamespaces().add(resultNamespaceBuilder.build());
        }
    }

    @ResultNamespace.Status
    private int checkAccessSingleProfile(AccessControlProfile profile,
            Collection<X509Certificate> readerCertificateChain,
            boolean isInRequestMessage) throws IdentityCredentialException {

        if (profile.isUserAuthenticationRequired()) {
            if (mInternalUserAuthenticationOracle != null) {
                if (!mInternalUserAuthenticationOracle.checkUserAuthentication(
                        profile.getAccessControlProfileId())) {
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
            if (!isInRequestMessage) {
                // The requested entry wasn't in the signed request message however this
                // profile indicated that reader authentication is required .. so we can't
                // return the value.
                return ResultNamespace.STATUS_READER_AUTHENTICATION_FAILED;
            }

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
            Collection<X509Certificate> readerCertificateChain,
            boolean isInRequestMessage) throws IdentityCredentialException {
        // Access is granted if at least one of the profiles grants access.
        //
        // If an item is configured without any profiles, access is denied.
        //
        @ResultNamespace.Status int lastStatus = ResultNamespace.STATUS_NO_ACCESS_CONTROL_PROFILES;

        for (int id : accessControlProfileIds) {
            AccessControlProfile profile = mData.getAccessControlProfile(id);
            lastStatus = checkAccessSingleProfile(profile, readerCertificateChain,
                    isInRequestMessage);
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
    public @NonNull
    Collection<X509Certificate> getAuthKeysNeedingCertification()
            throws IdentityCredentialException {
        return mData.getAuthKeysNeedingCertification();
    }

    @Override
    public void storeStaticAuthenticationData(X509Certificate authenticationKey,
            byte[] staticAuthData)
            throws IdentityCredentialException {
        mData.storeStaticAuthenticationData(authenticationKey, staticAuthData);
    }

    @Override
    public @NonNull
    int[] getAuthenticationDataUsageCount() throws IdentityCredentialException {
        return mData.getAuthKeyUseCounts();
    }

    void setInternalUserAuthenticationOracle(InternalUserAuthenticationOracle oracle) {
        mInternalUserAuthenticationOracle = oracle;
    }

    // Class used for unit-tests in UserAuthTest
    static abstract class InternalUserAuthenticationOracle {
        abstract boolean checkUserAuthentication(int accessControlProfileId);
    }
}