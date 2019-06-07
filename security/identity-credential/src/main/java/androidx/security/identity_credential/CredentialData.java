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
import androidx.annotation.RequiresApi;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

class CredentialData {
    private static final String TAG = "CredentialData";

    private Context mContext;
    private String mDocType;
    private int mCipherSuite;
    private String mCredentialName;
    private String mCredentialKeyAlias;
    private Collection<X509Certificate> mCertificateChain;
    private ArrayList<AccessControlProfile> mAccessControlProfiles;
    private Map<Integer, AccessControlProfile> mProfileIdToAcpMap;
    private ArrayList<EntryNamespace> mEntryNamespaces;

    private int mAuthKeyCount;
    private int mAuthMaxUsesPerKey;

    // If non-empty, the alias for the key that must be unlocked by user auth for every reader
    // session.
    private String mAcpAuthKeyAlias;

    // A map from ACP id to key alias.
    //
    // This is for ACPs with positive timeouts.
    private Map<Integer, String> mAcpTimeoutKeyAliases;

    // Note that a dynamic authentication key may have two Android Keystore keys associated with
    // it.. the obvious one is for a previously certificated key. This key may possibly have an
    // use-count which is already exhausted. The other one is for a key yet pending certification.
    //
    // Why is it implemented this way? Because we never want selectAuthenticationKey() to fail.
    // That is, it's better to use a key with an exhausted use-count (slightly bad for user privacy
    // in terms of linkability between multiple presentations) than the user not being able to
    // present their credential at all...
    private static class AuthKeyData {
        AuthKeyData() {
            mAlias = "";
            mUseCount = 0;
            mCertificate = new byte[0];
            mStaticAuthenticationData = new byte[0];
            mPendingAlias = "";
            mPendingCertificate = new byte[0];
        }

        // The mAlias for the key in Android Keystore. Is set to the empty string if the key has not
        // yet been created. This is set to the empty string if no key has been certified.
        String mAlias;

        // The X509 certificate for the key. This is empty if mAlias is empty.
        byte[] mCertificate;

        // The static authentication data, as set by the application as part of certification. This
        // is empty if mAlias is empty.
        byte[] mStaticAuthenticationData;

        // The number of times a key has been used.
        int mUseCount;

        // The alias for a key pending certification. Once a key has been certified - by
        // calling storeStaticAuthenticationData() - and is no longer pending, both mPendingAlias
        // and mPendingCertificate will be set to the empty string and empty byte array.
        String mPendingAlias;

        // The X509 certificate for a key pending certification.
        byte[] mPendingCertificate;
    }

    // The data for each authentication key, this is always mAuthKeyCount items.
    private ArrayList<AuthKeyData> mAuthKeyDatas;

    private CredentialData(Context context, String credentialName) {
        mContext = context;
        mDocType = "";
        mCipherSuite = -1;
        mCredentialName = credentialName;
        mCredentialKeyAlias = "";
        mAcpAuthKeyAlias = "";
        mAcpTimeoutKeyAliases = new HashMap<>();
        mAccessControlProfiles = new ArrayList<AccessControlProfile>();
        mProfileIdToAcpMap = new HashMap<Integer, AccessControlProfile>();
        mEntryNamespaces = new ArrayList<EntryNamespace>();
        mAuthKeyCount = 0;
        mAuthMaxUsesPerKey = 1;
        mAuthKeyDatas = new ArrayList<AuthKeyData>();
    }

    /**
     * Creates a new {@link CredentialData} with the given name and saves it to disk.
     * <p>
     * The created data will be configured with zero authentication keys and max one use per key.
     * <p>
     * The created data can later be loaded via the {@link #loadCredentialData(Context, String)}
     * method and deleted by calling {@link #delete()}.
     *
     * An auth-bound key will be created for each access control profile with user-authentication.
     *
     * @param context               the context.
     * @param credentialName        the name of the credential.
     * @param credentialKeyAlias    the alias of the credential key which must have already been created.
     * @param certificateChain      the certificate chain for the credential key.
     * @param accessControlProfiles the access control profiles.
     * @param entryNamespaces       the entry namespaces.
     * @return a new @{link CredentialData} object
     * @throws IdentityCredentialException TODO
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    static CredentialData createCredentialData(Context context,
                                               String docType,
                                               int cipherSuite,
                                               String credentialName,
                                               String credentialKeyAlias,
                                               Collection<X509Certificate> certificateChain,
                                               Collection<AccessControlProfile> accessControlProfiles,
                                               Collection<EntryNamespace> entryNamespaces) throws IdentityCredentialException {
        CredentialData data = new CredentialData(context, credentialName);
        data.mDocType = docType;
        data.mCipherSuite = cipherSuite;
        data.mCredentialKeyAlias = credentialKeyAlias;
        data.mCertificateChain = certificateChain;
        data.mAccessControlProfiles = new ArrayList<>();
        data.mProfileIdToAcpMap = new HashMap<>();
        for (AccessControlProfile item : accessControlProfiles) {
            data.mAccessControlProfiles.add(item);
            data.mProfileIdToAcpMap.put(item.getAccessControlProfileId(), item);
        }
        data.mEntryNamespaces = new ArrayList<>();
        for (EntryNamespace item : entryNamespaces) {
            data.mEntryNamespaces.add(item);
        }

        data.mAcpTimeoutKeyAliases = new HashMap<>();
        for (AccessControlProfile profile : accessControlProfiles) {
            boolean isAuthRequired = profile.isUserAuthenticationRequired();
            int timeoutSeconds = profile.getUserAuthenticationTimeout();
            if (isAuthRequired) {
                if (timeoutSeconds == 0) {
                    if (data.mAcpAuthKeyAlias.isEmpty()) {
                        data.mAcpAuthKeyAlias = getAcpKeyAliasFromCredentialName(credentialName);
                        try {
                            KeyGenerator kg = KeyGenerator.getInstance(
                                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                                    data.mAcpAuthKeyAlias,
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                    .setRandomizedEncryptionRequired(false)
                                    .setUserAuthenticationRequired(true)
                                    .setKeySize(128);
                            kg.init(builder.build());
                            kg.generateKey();
                        } catch (InvalidAlgorithmParameterException |
                                NoSuchAlgorithmException |
                                NoSuchProviderException e) {
                            e.printStackTrace();
                            throw new IdentityCredentialException("Error creating ACP auth-bound key");
                        }
                    }
                } else {
                    int profileId = profile.getAccessControlProfileId();
                    String acpAlias = getAcpTimeoutKeyAliasFromCredentialName(credentialName, profileId);
                    try {
                        KeyGenerator kg = KeyGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                                acpAlias,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false)
                                .setUserAuthenticationRequired(true)
                                .setUserAuthenticationValidityDurationSeconds(timeoutSeconds)
                                .setKeySize(128);
                        kg.init(builder.build());
                        kg.generateKey();
                    } catch (InvalidAlgorithmParameterException |
                            NoSuchAlgorithmException |
                            NoSuchProviderException e) {
                        e.printStackTrace();
                        throw new IdentityCredentialException("Error creating ACP auth-bound timeout key");
                    }
                    data.mAcpTimeoutKeyAliases.put(profileId, acpAlias);
                }
            }
        }

        data.createDataEncryptionKey();

        data.saveToDisk();
        return data;
    }

    private void createDataEncryptionKey() throws IdentityCredentialException {
        try {
            String dataKeyAlias = getDataKeyAliasFromCredentialName(mCredentialName);
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    dataKeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(false)
                    .setKeySize(128);
            kg.init(builder.build());
            kg.generateKey();
       } catch (InvalidAlgorithmParameterException |
                NoSuchAlgorithmException |
                NoSuchProviderException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error creating data encryption key");
        }
    }

    /**
     * Loads a @{link CredentialData} object previously created with {@link #createCredentialData(Context, String, String, Collection, Collection)}.
     *
     * @param context        the application context
     * @param credentialName the name of the credential.
     * @return a new @{link CredentialData} object.
     * @throws IdentityCredentialException TODO
     */
    static CredentialData loadCredentialData(Context context, String credentialName) throws IdentityCredentialException {
        CredentialData data = new CredentialData(context, credentialName);
        String dataKeyAlias = getDataKeyAliasFromCredentialName(credentialName);
        data.loadFromDisk(dataKeyAlias);
        return data;
    }

    static private String getFilenameForCredentialData(String credentialName) {
        // TODO: sanitize/validate/escape passed-in credential name.
        return "idcredlib_data_" + credentialName + ".bin";
    }

    static String getAliasFromCredentialName(String credentialName) {
        // TODO: sanitize/validate/escape passed-in credential name.
        return "idcredlib_credkey_" + credentialName;
    }

    static String getDataKeyAliasFromCredentialName(String credentialName) {
        // TODO: sanitize/validate/escape passed-in credential name.
        return "idcredlib_datakey_" + credentialName;
    }

    static String getAcpTimeoutKeyAliasFromCredentialName(String credentialName, int acpProfileId) {
        // TODO: sanitize/validate/escape passed-in credential name.
        return "idcredlib_acp_" + credentialName + "_timeoutForId_" + acpProfileId;
    }

    static String getAcpKeyAliasFromCredentialName(String credentialName) {
        // TODO: sanitize/validate/escape passed-in credential name.
        return "idcredlib_acp_" + credentialName;
    }

    static byte[] delete(Context context, String credentialName) throws IdentityCredentialException {
        String filename = getFilenameForCredentialData(credentialName);
        AtomicFile file = new AtomicFile(context.getFileStreamPath(filename));
        try {
            FileInputStream fis = file.openRead();
        } catch (FileNotFoundException e) {
            return null;
        }

        CredentialData data = new CredentialData(context, credentialName);
        String dataKeyAlias = getDataKeyAliasFromCredentialName(credentialName);
        data.loadFromDisk(dataKeyAlias);

        KeyStore.Entry entry;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            entry = ks.getEntry(data.mCredentialKeyAlias, null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException |
                KeyStoreException | UnrecoverableEntryException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error loading keystore");
        }

        byte[] encodedBytes = Util.buildProofCbor("ProofOfDeletion",
                data.mDocType,
                data.mAccessControlProfiles,
                data.mEntryNamespaces,
                ((KeyStore.PrivateKeyEntry) entry).getPrivateKey());

        file.delete();

        return encodedBytes;
    }

    private void saveToDisk() throws IdentityCredentialException {
        CborBuilder dtsBuilder = new CborBuilder();
        MapBuilder<CborBuilder> dtsMapBuilder = dtsBuilder.addMap();

        dtsMapBuilder.put("docType", mDocType);
        dtsMapBuilder.put("cipherSuite", mCipherSuite);
        dtsMapBuilder.put("credentialKeyAlias", mCredentialKeyAlias);

        ArrayBuilder<MapBuilder<CborBuilder>> credentialKeyCertChainBuilder = dtsMapBuilder.putArray("credentialKeyCertChain");
        for (X509Certificate certificate : mCertificateChain) {
            try {
                credentialKeyCertChainBuilder.add(certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error encoding certificate");
            }
        }
        dtsMapBuilder.put("authKeyCount", mAuthKeyCount);
        dtsMapBuilder.put("authKeyMaxUses", mAuthMaxUsesPerKey);

        ArrayBuilder<MapBuilder<CborBuilder>> authKeyDataArrayBuilder = dtsMapBuilder.putArray("authKeyDatas");
        for (AuthKeyData data : mAuthKeyDatas) {
            authKeyDataArrayBuilder.addArray()
                    .add(data.mAlias)
                    .add(data.mUseCount)
                    .add(data.mCertificate)
                    .add(data.mStaticAuthenticationData)
                    .add(data.mPendingAlias)
                    .add(data.mPendingCertificate)
                    .end();
        }

        ArrayBuilder<MapBuilder<CborBuilder>> acpArrayBuilder = dtsMapBuilder.putArray("accessControlProfiles");
        for (AccessControlProfile profile : mAccessControlProfiles) {
            acpArrayBuilder.add(Util.accessControlProfileToCbor(profile));
        }

        MapBuilder<MapBuilder<CborBuilder>> ensArrayBuilder = dtsMapBuilder.putMap("entryNamespaces");
        for (EntryNamespace entryNamespace : mEntryNamespaces) {
            ensArrayBuilder.put(new UnicodeString(entryNamespace.getNamespaceName()),
                    Util.entryNamespaceToCbor(entryNamespace));
        }

        dtsMapBuilder.put("acpAuthKeyAlias", mAcpAuthKeyAlias);

        MapBuilder<MapBuilder<CborBuilder>> acpTimeoutKeyMapBuilder = dtsMapBuilder.putMap("acpTimeoutKeyMap");
        for (Map.Entry<Integer, String> entry : mAcpTimeoutKeyAliases.entrySet()) {
            int profileId = entry.getKey();
            String acpAlias = entry.getValue();
            acpTimeoutKeyMapBuilder.put(new UnsignedInteger(profileId), new UnicodeString(acpAlias));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(baos);
        try {
            encoder.encode(dtsBuilder.build());
        } catch (CborException e) {
            e.printStackTrace();
        }
        byte[] cleartextDataToSaveBytes = baos.toByteArray();

        // Encrypt data, using data encryption key.
        byte[] dataToSaveBytes = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            String dataKeyAlias = getDataKeyAliasFromCredentialName(mCredentialName);

            byte[] iv = new byte[12];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            KeyStore.Entry entry = ks.getEntry(dataKeyAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            byte[] cipherText = cipher.doFinal(cleartextDataToSaveBytes); // This includes the auth tag
            ByteBuffer byteBuffer = ByteBuffer.allocate(12 + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            dataToSaveBytes = byteBuffer.array();
        } catch (InvalidAlgorithmParameterException |
                NoSuchPaddingException |
                BadPaddingException |
                NoSuchAlgorithmException |
                CertificateException |
                InvalidKeyException |
                UnrecoverableEntryException |
                IOException |
                IllegalBlockSizeException |
                KeyStoreException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error encrypting CBOR for saving to disk");
        }

        String filename = getFilenameForCredentialData(mCredentialName);
        AtomicFile file = new AtomicFile(mContext.getFileStreamPath(filename));
        FileOutputStream outputStream = null;
        try {
            outputStream = file.startWrite();
            outputStream.write(dataToSaveBytes);
            outputStream.close();
            file.finishWrite(outputStream);
        } catch (IOException e) {
            if (outputStream != null) {
                file.failWrite(outputStream);
            }
            e.printStackTrace();
        }
    }

    private void loadFromDisk(String dataKeyAlias) throws IdentityCredentialException {
        String filename = getFilenameForCredentialData(mCredentialName);
        byte[] encryptedFileData = new byte[0];
        try {
            AtomicFile file = new AtomicFile(mContext.getFileStreamPath(filename));
            encryptedFileData = file.readFully();
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] fileData = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            KeyStore.Entry entry = ks.getEntry(dataKeyAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();

            if(encryptedFileData.length < 12) {
                throw new IdentityCredentialException("Encrypted CBOR on disk is too small");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedFileData);
            byte[] iv = new byte[12];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[encryptedFileData.length - 12];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            fileData = cipher.doFinal(cipherText);
        } catch (InvalidAlgorithmParameterException |
                NoSuchPaddingException |
                BadPaddingException |
                NoSuchAlgorithmException |
                CertificateException |
                InvalidKeyException |
                IOException |
                IllegalBlockSizeException |
                UnrecoverableEntryException |
                KeyStoreException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error decrypting CBOR");
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new IdentityCredentialException("Expected 1 item, found " + dataItems.size());
            }
            if (!(dataItems.get(0) instanceof co.nstant.in.cbor.model.Map)) {
                throw new IdentityCredentialException("Item is not a map");
            }
            co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) dataItems.get(0);

            mDocType = ((UnicodeString) map.get(new UnicodeString("docType"))).getString();

            mCipherSuite = ((Number) map.get(new UnicodeString("cipherSuite"))).getValue().intValue();

            mCredentialKeyAlias = ((UnicodeString) map.get(new UnicodeString("credentialKeyAlias"))).getString();

            DataItem credentialKeyCertChain = map.get(new UnicodeString("credentialKeyCertChain"));
            if (credentialKeyCertChain == null || !(credentialKeyCertChain instanceof Array)) {
                throw new IdentityCredentialException("credentialKeyCertChain not found or not array");
            }
            mCertificateChain = new LinkedList<>();
            for (DataItem item : ((Array) credentialKeyCertChain).getDataItems()) {
                byte[] encodedCert = ((ByteString) item).getBytes();
                try {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    ByteArrayInputStream certBais = new ByteArrayInputStream(encodedCert);
                    mCertificateChain.add((X509Certificate) cf.generateCertificate(certBais));
                } catch (CertificateException e) {
                    e.printStackTrace();
                    throw new IdentityCredentialException("Error decoding certificate blob");
                }
            }


            DataItem accessControlProfiles = map.get(new UnicodeString("accessControlProfiles"));
            if (accessControlProfiles == null || !(accessControlProfiles instanceof Array)) {
                throw new IdentityCredentialException("accessControlProfiles not found or not array");
            }
            mAccessControlProfiles = new ArrayList<AccessControlProfile>();
            mProfileIdToAcpMap = new HashMap<Integer, AccessControlProfile>();
            for (DataItem item : ((Array) accessControlProfiles).getDataItems()) {
                AccessControlProfile profile = Util.accessControlProfileFromCbor(item);
                mAccessControlProfiles.add(profile);
                mProfileIdToAcpMap.put(profile.getAccessControlProfileId(), profile);
            }

            DataItem entryNamespaces = map.get(new UnicodeString("entryNamespaces"));
            if (entryNamespaces == null || !(entryNamespaces instanceof co.nstant.in.cbor.model.Map)) {
                throw new IdentityCredentialException("entryNamespaces not found or not map");
            }
            mEntryNamespaces = new ArrayList<EntryNamespace>();
            for (DataItem key : ((co.nstant.in.cbor.model.Map) entryNamespaces).getKeys()) {
                if (!(key instanceof UnicodeString)) {
                    throw new IdentityCredentialException("Key in entryNamespaces is not a string");
                }
                String namespaceName = ((UnicodeString) key).getString();
                DataItem item = ((co.nstant.in.cbor.model.Map) entryNamespaces).get(key);
                mEntryNamespaces.add(Util.entryNamespaceFromCbor(namespaceName, item));
            }

            mAcpAuthKeyAlias = ((UnicodeString) map.get(new UnicodeString("acpAuthKeyAlias"))).getString();

            DataItem userAuthKeyAliases = map.get(new UnicodeString("acpTimeoutKeyMap"));
            if (userAuthKeyAliases == null || !(userAuthKeyAliases instanceof co.nstant.in.cbor.model.Map)) {
                throw new IdentityCredentialException("acpTimeoutKeyMap not found or not map");
            }
            mAcpTimeoutKeyAliases = new HashMap<>();
            for (DataItem key : ((co.nstant.in.cbor.model.Map) userAuthKeyAliases).getKeys()) {
                if (!(key instanceof UnsignedInteger)) {
                    throw new IdentityCredentialException("Key in acpTimeoutKeyMap is not an integer");
                }
                int profileId = ((UnsignedInteger) key).getValue().intValue();
                DataItem item = ((co.nstant.in.cbor.model.Map) userAuthKeyAliases).get(key);
                if (!(item instanceof UnicodeString)) {
                    throw new IdentityCredentialException("Item in acpTimeoutKeyMap is not a string");
                }
                String acpAlias = ((UnicodeString) item).getString();
                mAcpTimeoutKeyAliases.put(profileId, acpAlias);
            }

            mAuthKeyCount = ((Number) map.get(new UnicodeString("authKeyCount"))).getValue().intValue();
            mAuthMaxUsesPerKey = ((Number) map.get(new UnicodeString("authKeyMaxUses"))).getValue().intValue();

            DataItem authKeyDatas = map.get(new UnicodeString("authKeyDatas"));
            if (authKeyDatas == null || !(authKeyDatas instanceof Array)) {
                throw new IdentityCredentialException("authKeyDatas not found or not array");
            }
            mAuthKeyDatas = new ArrayList<AuthKeyData>();
            for (DataItem arrayItem : ((Array) authKeyDatas).getDataItems()) {
                List<DataItem> items = ((Array) arrayItem).getDataItems();
                AuthKeyData data = new AuthKeyData();

                data.mAlias = ((UnicodeString) items.get(0)).getString();
                data.mUseCount = ((Number) items.get(1)).getValue().intValue();
                data.mCertificate = ((ByteString) items.get(2)).getBytes();
                data.mStaticAuthenticationData = ((ByteString) items.get(3)).getBytes();
                data.mPendingAlias = ((UnicodeString) items.get(4)).getString();
                data.mPendingCertificate = ((ByteString) items.get(5)).getBytes();

                mAuthKeyDatas.add(data);
            }
        } catch (CborException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a credential previously created with
     * {@link #createCredentialData(Context, String, String, Collection, Collection)}.
     * <p>
     * The object can no longer be used after calling this method.
     *
     * @throws IdentityCredentialException
     */
    void delete() throws IdentityCredentialException {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            if (ks.containsAlias(mCredentialKeyAlias)) {
                ks.deleteEntry(mCredentialKeyAlias);
            }
            String dataKeyAlias = getDataKeyAliasFromCredentialName(mCredentialName);
            if (ks.containsAlias(dataKeyAlias)) {
                ks.deleteEntry(dataKeyAlias);
            }

            if (!mAcpAuthKeyAlias.isEmpty()) {
                if (ks.containsAlias(mAcpAuthKeyAlias)) {
                    ks.deleteEntry(mAcpAuthKeyAlias);
                }
            }

            for (Map.Entry<Integer, String> entry : mAcpTimeoutKeyAliases.entrySet()) {
                String acpAlias = entry.getValue();
                if (ks.containsAlias(acpAlias)) {
                    ks.deleteEntry(acpAlias);
                }
            }

            for (AuthKeyData data : mAuthKeyDatas) {
                if (!data.mAlias.isEmpty() && ks.containsAlias(data.mAlias)) {
                    ks.deleteEntry(data.mAlias);
                }
            }

            String filename = getFilenameForCredentialData(mCredentialName);
            AtomicFile file = new AtomicFile(mContext.getFileStreamPath(filename));
            file.delete();
        } catch (KeyStoreException | CertificateException |
                NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error deleting CredentialKey");
        }
    }

    Collection<AccessControlProfile> getAccessControlProfiles() {
        return mAccessControlProfiles;
    }

    Collection<EntryNamespace> getEntryNamespaces() {
        return mEntryNamespaces;
    }

    String getCredentialKeyAlias() {
        return mCredentialKeyAlias;
    }

    int getAuthKeyCount() {
        return mAuthKeyCount;
    }

    int getAuthMaxUsesPerKey() {
        return mAuthMaxUsesPerKey;
    }

    int[] getAuthKeyUseCounts() {
        int[] result = new int[mAuthKeyCount];
        int n = 0;
        for (AuthKeyData data : mAuthKeyDatas) {
            result[n++] = data.mUseCount;
        }
        return result;
    }

    void setAvailableAuthenticationKeys(int keyCount, int maxUsesPerKey) throws IdentityCredentialException {
        int prevKeyCount = mAuthKeyCount;
        mAuthKeyCount = keyCount;
        mAuthMaxUsesPerKey = maxUsesPerKey;

        if (prevKeyCount < mAuthKeyCount) {
            // Added non-zero number of auth keys...
            for (int n = prevKeyCount; n < mAuthKeyCount; n++) {
                mAuthKeyDatas.add(new AuthKeyData());
            }
        } else if (prevKeyCount > mAuthKeyCount) {
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null, null);
            } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error loading keystore");
            }

            int numKeysToDelete = prevKeyCount - mAuthKeyCount;
            // Removed non-zero number of auth keys. For now we just delete
            // the keys at the beginning... (an optimization could be to instead
            // delete the keys with the biggest use count).
            for (int n = 0; n < numKeysToDelete; n++) {
                AuthKeyData data = mAuthKeyDatas.get(0);
                if (!data.mAlias.isEmpty()) {
                    try {
                        if (ks.containsAlias(data.mAlias)) {
                            ks.deleteEntry(data.mAlias);
                        }
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                        throw new IdentityCredentialException("Error deleting auth key with mAlias " + data.mAlias);
                    }
                }
                if (!data.mPendingAlias.isEmpty()) {
                    try {
                        if (ks.containsAlias(data.mPendingAlias)) {
                            ks.deleteEntry(data.mPendingAlias);
                        }
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                        throw new IdentityCredentialException("Error deleting auth key with mPendingAlias " + data.mPendingAlias);
                    }
                }
                mAuthKeyDatas.remove(0);
            }
        }
        saveToDisk();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    Collection<X509Certificate> getAuthKeysNeedingCertification() throws IdentityCredentialException {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error loading keystore");
        }

        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();

        // Determine which keys need certification (or re-certification) and generate
        // keys and X.509 certs for these and mark them as pending.
        for (int n = 0; n < mAuthKeyCount; n++) {
            AuthKeyData data = mAuthKeyDatas.get(n);

            boolean certificationNeeded = data.mAlias.isEmpty() || (data.mUseCount >= mAuthMaxUsesPerKey);
            boolean certificationPending = !data.mPendingAlias.isEmpty();

            if (certificationNeeded && !certificationPending) {
                try {
                    // Calculate name to use and be careful to avoid collisions when
                    // re-certifying an already populated slot.
                    String aliasForAuthKey = mCredentialKeyAlias + String.format("_auth_%d", n);
                    if (aliasForAuthKey.equals(data.mAlias)) {
                        aliasForAuthKey = aliasForAuthKey + "_";
                    }

                    KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                    KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                            aliasForAuthKey,
                            KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512);
                    kpg.initialize(builder.build());
                    KeyPair keyPair = kpg.generateKeyPair();

                    X509Certificate certificate = Util.signPublicKeyWithPrivateKey(aliasForAuthKey,
                            mCredentialKeyAlias);
                    if (certificate == null) {
                        throw new IdentityCredentialException("Error signing AuthKey with CredentialKey");
                    }

                    data.mPendingAlias = aliasForAuthKey;
                    data.mPendingCertificate = certificate.getEncoded();
                    certificationPending = true;
                } catch (InvalidAlgorithmParameterException |
                        NoSuchAlgorithmException |
                        NoSuchProviderException |
                        CertificateEncodingException e) {
                    e.printStackTrace();
                    throw new IdentityCredentialException("Error creating auth key");
                }
            }

            if (certificationPending) {
                try {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    ByteArrayInputStream bais = new ByteArrayInputStream(data.mPendingCertificate);
                    certificates.add((X509Certificate) cf.generateCertificate(bais));
                } catch (CertificateException e) {
                    e.printStackTrace();
                    throw new IdentityCredentialException("Error creating certificate for auth key");
                }
            }
        }

        saveToDisk();

        return certificates;
    }

    void storeStaticAuthenticationData(X509Certificate authenticationKey,
                                       byte[] staticAuthData)
            throws IdentityCredentialException {
        AuthKeyData dataForAuthKey = null;
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");

            for (AuthKeyData data : mAuthKeyDatas) {
                if (data.mPendingCertificate.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data.mPendingCertificate);
                    X509Certificate certificate = (X509Certificate) cf.generateCertificate(bais);
                    if (certificate.equals(authenticationKey)) {
                        dataForAuthKey = data;
                        break;
                    }
                }
            }
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error encoding certificate");
        }

        if (dataForAuthKey == null) {
            throw new UnknownAuthenticationKeyException("No such authentication key");
        }

        Log.d(TAG, "Certifying key with mAlias=" + dataForAuthKey.mAlias +
                " and mPendingAlias=" + dataForAuthKey.mPendingAlias);

        // Delete old key, if set.
        if (!dataForAuthKey.mAlias.isEmpty()) {
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null, null);
                if (ks.containsAlias(dataForAuthKey.mAlias)) {
                    ks.deleteEntry(dataForAuthKey.mAlias);
                }
            } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error deleting old authentication key");
            }
        }
        dataForAuthKey.mAlias = dataForAuthKey.mPendingAlias;
        dataForAuthKey.mCertificate = dataForAuthKey.mPendingCertificate;
        dataForAuthKey.mStaticAuthenticationData = staticAuthData;
        dataForAuthKey.mUseCount = 0;
        dataForAuthKey.mPendingAlias = "";
        dataForAuthKey.mPendingCertificate = new byte[0];

        saveToDisk();
    }

    /**
     * Selects an authentication key to use.
     *
     * The victim is picked simply by choosing the key with the smallest use count. (This may
     * change in the future.)
     *
     * The use count of the returned authentication key will be increased by one.
     *
     * @param allowUsingExhaustedKeys
     *            If {@code true}, allow using an authentication key which use count has been
     *            exceeded if no other key is available. If @{code false}, this method will
     *            throw @{link NoAuthenticationKeyAvailableException} if in the same situation.
     *
     * @return A pair containing the authentication key and its associated static authentication
     *         data or {@code null} if no key could be found.
     *
     * @throws IdentityCredentialException
     */
    public Pair<PrivateKey, byte[]> selectAuthenticationKey(boolean allowUsingExhaustedKeys) throws IdentityCredentialException {
        AuthKeyData candidate = null;

        for (int n = 0; n < mAuthKeyCount; n++) {
            AuthKeyData data = mAuthKeyDatas.get(n);
            if (!data.mAlias.isEmpty() && data.mUseCount < mAuthMaxUsesPerKey) {
                if (candidate == null) {
                    candidate = data;
                } else {
                    if (data.mUseCount < candidate.mUseCount) {
                        candidate = data;
                    }
                }
            }
        }

        // If we're allowed to use exhausted keys, search again...
        if (candidate == null && allowUsingExhaustedKeys) {
            for (int n = 0; n < mAuthKeyCount; n++) {
                AuthKeyData data = mAuthKeyDatas.get(n);
                if (!data.mAlias.isEmpty()) {
                    if (candidate == null) {
                        candidate = data;
                    } else {
                        if (data.mUseCount < candidate.mUseCount) {
                            candidate = data;
                        }
                    }
                }
            }
        }

        if (candidate == null) {
            return null;
        }

        KeyStore.Entry entry = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            entry = ks.getEntry(candidate.mAlias, null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException |
                KeyStoreException | UnrecoverableEntryException e) {
            e.printStackTrace();
            throw new IdentityCredentialException("Error loading keystore");
        }

        Pair<PrivateKey, byte[]> result = new Pair<>(((KeyStore.PrivateKeyEntry) entry).getPrivateKey(),
                candidate.mStaticAuthenticationData);

        candidate.mUseCount += 1;
        saveToDisk();

        return result;
    }


    String getDocType() {
        return mDocType;
    }

    int getCipherSuite() {
        return mCipherSuite;
    }

    Collection<X509Certificate> getCredentialKeyCertificateChain() {
        return mCertificateChain;
    }

    AccessControlProfile getAccessControlProfile(int profileId) throws IdentityCredentialException {
        AccessControlProfile profile = mProfileIdToAcpMap.get(profileId);
        if (profile == null) {
            throw new IdentityCredentialException("No profile with id " + profileId);
        }
        return profile;
    }

    private boolean checkUserAuthenticationNonTimeout() throws IdentityCredentialException {
        // Unfortunately there are no APIs to tell if a key needs user authentication to work so
        // we check if the key is available by simply trying to encrypt some data.
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            KeyStore.Entry entry = ks.getEntry(mAcpAuthKeyAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();

            byte[] iv = new byte[12];
            for (int n = 0; n < iv.length; n++) {
                iv[n] = 0x00;
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] clearText = {0x01, 0x02};
            byte[] cipherText = cipher.doFinal(clearText);
        } catch (InvalidAlgorithmParameterException |
                NoSuchPaddingException |
                BadPaddingException |
                NoSuchAlgorithmException |
                CertificateException |
                InvalidKeyException |
                IOException |
                IllegalBlockSizeException |
                UnrecoverableEntryException |
                KeyStoreException e) {
            // If this fails, it probably means authentication is needed... (there's no
            // specific exception for that, unfortunately.)
            //e.printStackTrace();
            //throw new IdentityCredentialException("Error checking for user authentication");
            return false;
        }
        return true;
    }

    private boolean checkUserAuthenticationTimeout(String acpAlias) throws IdentityCredentialException {
        // Unfortunately there are no APIs to tell if a key needs user authentication to work so
        // we check if the key is available by simply trying to encrypt some data.
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null, null);
            KeyStore.Entry entry = ks.getEntry(acpAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();

            byte[] iv = new byte[12];
            for (int n = 0; n < iv.length; n++) {
                iv[n] = 0x00;
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] clearText = {0x01, 0x02};
            byte[] cipherText = cipher.doFinal(clearText);
        } catch (InvalidAlgorithmParameterException |
                    NoSuchPaddingException |
                    BadPaddingException |
                    NoSuchAlgorithmException |
                    CertificateException |
                    InvalidKeyException |
                    IOException |
                    IllegalBlockSizeException |
                    UnrecoverableEntryException |
                    KeyStoreException e) {
            // If this fails, it probably means authentication is needed... (there's no
            // specific exception for that, unfortunately.)
            //e.printStackTrace();
            //throw new IdentityCredentialException("Error checking for user authentication");
            return false;
        }
        return true;
    }

    public boolean checkUserAuthentication(int accessControlProfileId) throws IdentityCredentialException {
        AccessControlProfile profile = getAccessControlProfile(accessControlProfileId);
        if (profile.getUserAuthenticationTimeout() == 0) {
            return checkUserAuthenticationNonTimeout();
        };
        String acpAlias = mAcpTimeoutKeyAliases.get(accessControlProfileId);
        if (acpAlias == null) {
            throw new IdentityCredentialException("No key alias for ACP with ID " + accessControlProfileId);
        }
        return checkUserAuthenticationTimeout(acpAlias);
    }

}