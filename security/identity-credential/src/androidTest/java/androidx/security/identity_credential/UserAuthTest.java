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

import static androidx.security.identity_credential.IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256;
import static androidx.security.identity_credential.ReaderAuthTest.createReaderKey;
import static androidx.security.identity_credential.Util.cborPrettyPrint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.InstrumentationRegistry;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;


public class UserAuthTest {
    private static final String TAG = "UserAuthTest";

    @Test
    public void userAuth()
            throws IdentityCredentialException, CborException, InvalidAlgorithmParameterException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {
        // This test creates three different access control profiles:
        //
        // - free for all
        // - user authentication on every reader session
        // - user authentication with 10 second timeout
        //
        // and for each ACP, a single entry which is protected by only that ACP.
        //

        // Provision the credential.
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);
        store.deleteCredentialByName("test");
        WritableIdentityCredential wc = store.createCredential("test",
                "org.iso.18013-5.2019.mdl",
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256);
        LinkedList<AccessControlProfile> profiles = new LinkedList<>();
        // Profile 2 (user auth with 10 second timeout)
        profiles.add(new AccessControlProfile.Builder(2)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationTimeout(10)
                .build());
        // Profile 0 (free for all)
        profiles.add(new AccessControlProfile.Builder(0)
                .setUserAuthenticationRequired(false)
                .build());
        // Profile 1 (user auth on every reader session)
        profiles.add(new AccessControlProfile.Builder(1)
                .setUserAuthenticationRequired(true)
                .build());
        // (We add the profiles in this weird order - 2, 0, 1 - to check below that the
        // provisioning receipt lists them in the same order.)
        LinkedList<EntryNamespace> entryNamespaces = new LinkedList<>();
        Collection<Integer> idsProfile0 = new ArrayList<Integer>();
        idsProfile0.add(0);
        Collection<Integer> idsProfile1 = new ArrayList<Integer>();
        idsProfile1.add(1);
        Collection<Integer> idsProfile2 = new ArrayList<Integer>();
        idsProfile2.add(2);
        entryNamespaces.add(
                new EntryNamespace.Builder("org.iso.18013-5.2019")
                        .addStringEntry("Accessible to all (0)", idsProfile0, "foo0")
                        .addStringEntry("Accessible to auth-every-session (1)", idsProfile1, "foo1")
                        .addStringEntry("Accessible to auth-with-10-sec-timeout (2)", idsProfile2,
                                "foo2")
                        .build());
        byte[] proofOfProvisioningCbor = wc.personalize(profiles, entryNamespaces);

        String pretty = cborPrettyPrint(proofOfProvisioningCbor);
        pretty = Util.replaceLine(pretty, -2, "  [] // Signature Removed");
        Log.d(TAG, "pretty: " + pretty);
        assertEquals("[\n" +
                "  [\n" +
                "    'ProofOfProvisioning',\n" +
                "    'org.iso.18013-5.2019.mdl',\n" +
                "    [\n" +
                "      {\n" +
                "        'id' : 2,\n" +
                "        'timeout' : 10,\n" +
                "        'capabilityType' : 1\n" +
                "      },\n" +
                "      {\n" +
                "        'id' : 0\n" +
                "      },\n" +
                "      {\n" +
                "        'id' : 1,\n" +
                "        'capabilityType' : 1\n" +
                "      }\n" +
                "    ],\n" +
                "    {\n" +
                "      'org.iso.18013-5.2019' : [\n" +
                "        {\n" +
                "          'name' : 'Accessible to all (0)',\n" +
                "          'value' : 'foo0',\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Accessible to auth-every-session (1)',\n" +
                "          'value' : 'foo1',\n" +
                "          'accessControlProfiles' : [1]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Accessible to auth-with-10-sec-timeout (2)',\n" +
                "          'value' : 'foo2',\n" +
                "          'accessControlProfiles' : [2]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    false\n" +
                "  ],\n" +
                "  [] // Signature Removed\n" +
                "]", pretty);


        // Get the credential we'll be reading from and provision it with a sufficient number
        // of dynamic auth keys
        IdentityCredential credential = store.getCredentialByName("test");
        assertNotNull(credential);
        credential.setAvailableAuthenticationKeys(1, 10);
        Collection<X509Certificate> dynAuthKeyCerts = credential.getAuthKeysNeedingCertification();
        credential.storeStaticAuthenticationData(dynAuthKeyCerts.iterator().next(), new byte[0]);

        IdentityCredential.GetEntryResult result;
        Collection<String> entryNames;
        Collection<ResultNamespace> resultNamespaces;
        ResultNamespace ns;
        LinkedList<RequestNamespace> requestEntryNamespaces;

        // First, without the Auth Oracle, try retrieving all three items and check that only
        // "Accessible to all (0)" can be retrieved....
        requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("Accessible to all (0)")
                        .addEntryName("Accessible to auth-every-session (1)")
                        .addEntryName("Accessible to auth-with-10-sec-timeout (2)")
                        .build());
        result = credential.getEntries(RequestNamespace.createItemsRequest(requestEntryNamespaces,
                null),
                requestEntryNamespaces,
                null,
                null,
                null);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        entryNames = ns.getEntryNames();
        assertEquals(3, entryNames.size());
        assertTrue(entryNames.contains("Accessible to all (0)"));
        assertTrue(entryNames.contains("Accessible to auth-every-session (1)"));
        assertTrue(entryNames.contains("Accessible to auth-with-10-sec-timeout (2)"));
        assertEquals(ResultNamespace.STATUS_OK, ns.getStatus("Accessible to all (0)"));
        assertEquals(ResultNamespace.STATUS_USER_AUTHENTICATION_FAILED,
                ns.getStatus("Accessible to auth-every-session (1)"));
        assertEquals(ResultNamespace.STATUS_USER_AUTHENTICATION_FAILED,
                ns.getStatus("Accessible to auth-with-10-sec-timeout (2)"));
        assertEquals("foo0", ns.getStringEntry("Accessible to all (0)"));
        assertEquals(null, ns.getStringEntry("Accessible to auth-every-session (1)"));
        assertEquals(null, ns.getStringEntry("Accessible to auth-with-10-sec-timeout (2)"));

        credential = store.getCredentialByName("test");
        assertNotNull(credential);
        // Now use an Auth Oracle to fake user authentication...
        final boolean[] testAccessControlProfileIdAccess = new boolean[3];
        SoftwareIdentityCredential.InternalUserAuthenticationOracle oracle =
                new SoftwareIdentityCredential.InternalUserAuthenticationOracle() {
                    @Override
                    boolean checkUserAuthentication(int accessControlProfileId) {
                        return testAccessControlProfileIdAccess[accessControlProfileId];
                    }
                };
        ((SoftwareIdentityCredential) credential).setInternalUserAuthenticationOracle(oracle);

        // Ensure "Accessible to auth-every-session (1)" without user auth returns
        // STATUS_NO_USER_AUTHENTICATION
        requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("Accessible to auth-every-session (1)")
                        .build());
        result = credential.getEntries(
                RequestNamespace.createItemsRequest(requestEntryNamespaces, null),
                requestEntryNamespaces,
                null,
                null,
                null);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        entryNames = ns.getEntryNames();
        assertEquals(1, entryNames.size());
        assertTrue(entryNames.contains("Accessible to auth-every-session (1)"));
        assertEquals(ResultNamespace.STATUS_USER_AUTHENTICATION_FAILED,
                ns.getStatus("Accessible to auth-every-session (1)"));
        assertEquals(null, ns.getStringEntry("Accessible to auth-every-session (1)"));

        // Check that it's returned once the Auth Oracle says authentication works...
        testAccessControlProfileIdAccess[1] = true;
        credential = store.getCredentialByName("test");
        ((SoftwareIdentityCredential) credential).setInternalUserAuthenticationOracle(oracle);
        assertNotNull(credential);
        result = credential.getEntries(RequestNamespace.createItemsRequest(requestEntryNamespaces,
                null),
                requestEntryNamespaces,
                null,
                null,
                null);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        entryNames = ns.getEntryNames();
        assertEquals(1, entryNames.size());
        assertTrue(entryNames.contains("Accessible to auth-every-session (1)"));
        assertEquals(ResultNamespace.STATUS_OK,
                ns.getStatus("Accessible to auth-every-session (1)"));
        assertEquals("foo1", ns.getStringEntry("Accessible to auth-every-session (1)"));
    }

    @Test
    public void userAndReaderAuthInSameACP()
            throws IdentityCredentialException, CborException, InvalidAlgorithmParameterException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {
        // This test creates a single ACP which requires both user auth and reader auth.
        //
        // Entries protected by such an ACP can only be retrieved if both user auth AND reader auth
        // checks pass.
        //
        // Contrast with userAndReaderAuthDifferentACPs() which is the OR case.

        KeyPair readerKeyPairA = createReaderKey("readerKeyA", false);
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        X509Certificate certA = (X509Certificate) ks.getCertificate("readerKeyA");
        Collection<X509Certificate> certChainForA = new LinkedList<>();
        certChainForA.add(certA);

        // Provision the credential.
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);
        store.deleteCredentialByName("test");
        WritableIdentityCredential wc = store.createCredential("test",
                "org.iso.18013-5.2019.mdl",
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256);
        LinkedList<AccessControlProfile> profiles = new LinkedList<>();
        profiles.add(new AccessControlProfile.Builder(0)
                .setUserAuthenticationRequired(true)
                .setReaderCertificate(certA)
                .build());
        LinkedList<EntryNamespace> entryNamespaces = new LinkedList<>();
        Collection<Integer> idsProfile0 = new ArrayList<Integer>();
        idsProfile0.add(0);
        entryNamespaces.add(
                new EntryNamespace.Builder("org.iso.18013-5.2019")
                        .addStringEntry("User/reader protected", idsProfile0, "bsg75")
                        .build());
        byte[] proofOfProvisioningCbor = wc.personalize(profiles, entryNamespaces);

        String pretty = cborPrettyPrint(proofOfProvisioningCbor);
        pretty = Util.replaceLine(pretty, 8, "        'readerCertificate' : [] // Removed");
        pretty = Util.replaceLine(pretty, -2, "  [] // Signature Removed");
        Log.d(TAG, "pretty: " + pretty);
        assertEquals("[\n" +
                "  [\n" +
                "    'ProofOfProvisioning',\n" +
                "    'org.iso.18013-5.2019.mdl',\n" +
                "    [\n" +
                "      {\n" +
                "        'id' : 0,\n" +
                "        'capabilityType' : 1,\n" +
                "        'readerCertificate' : [] // Removed\n" +
                "      }\n" +
                "    ],\n" +
                "    {\n" +
                "      'org.iso.18013-5.2019' : [\n" +
                "        {\n" +
                "          'name' : 'User/reader protected',\n" +
                "          'value' : 'bsg75',\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    false\n" +
                "  ],\n" +
                "  [] // Signature Removed\n" +
                "]", pretty);

        // Get the credential we'll be reading from and provision it with a sufficient number
        // of dynamic auth keys
        IdentityCredential credential = store.getCredentialByName("test");
        assertNotNull(credential);
        credential.setAvailableAuthenticationKeys(1, 10);
        Collection<X509Certificate> dynAuthKeyCerts = credential.getAuthKeysNeedingCertification();
        credential.storeStaticAuthenticationData(dynAuthKeyCerts.iterator().next(), new byte[0]);

        IdentityCredential.GetEntryResult result;
        Collection<String> entryNames;
        Collection<ResultNamespace> resultNamespaces;
        ResultNamespace ns;
        LinkedList<RequestNamespace> requestEntryNamespaces;

        // Without the Auth Oracle and reader auth, retrieval should fail...
        requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("User/reader protected")
                        .build());
        result = credential.getEntries(
                RequestNamespace.createItemsRequest(requestEntryNamespaces, null),
                requestEntryNamespaces,
                null,
                null,
                null);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(0, ns.getRetrievedEntryNames().size());
        assertEquals(1, ns.getEntryNames().size());


        // With reader auth, it still won't work...
        credential = store.getCredentialByName("test");
        result = userAndReaderRetrievalHelper(credential, readerKeyPairA, certChainForA);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(0, ns.getRetrievedEntryNames().size());
        assertEquals(1, ns.getEntryNames().size());

        // ... however once we use an Auth Oracle it will work.
        credential = store.getCredentialByName("test");
        ((SoftwareIdentityCredential) credential).setInternalUserAuthenticationOracle(
                new SoftwareIdentityCredential.InternalUserAuthenticationOracle() {
                    @Override
                    boolean checkUserAuthentication(int accessControlProfileId) {
                        return true;
                    }
                });
        result = userAndReaderRetrievalHelper(credential, readerKeyPairA, certChainForA);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(1, ns.getRetrievedEntryNames().size());
        assertEquals(ResultNamespace.STATUS_OK, ns.getStatus("User/reader protected"));
        assertEquals("bsg75", ns.getStringEntry("User/reader protected"));
    }

    @Test
    public void userAndReaderAuthDifferentACPs()
            throws IdentityCredentialException, CborException, InvalidAlgorithmParameterException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {
        // This test creates a two ACPs - one which requires user auth and one which requires
        // reader auth.
        //
        // Entries with both ACPs can be retrieved with either user auth OR reader auth.
        //
        // Contrast with userAndReaderAuthInSameACP() which is the AND case.

        KeyPair readerKeyPairA = createReaderKey("readerKeyA", false);
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        X509Certificate certA = (X509Certificate) ks.getCertificate("readerKeyA");
        Collection<X509Certificate> certChainForA = new LinkedList<>();
        certChainForA.add(certA);

        // Provision the credential.
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);
        store.deleteCredentialByName("test");
        WritableIdentityCredential wc = store.createCredential("test",
                "org.iso.18013-5.2019.mdl",
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256);
        LinkedList<AccessControlProfile> profiles = new LinkedList<>();
        profiles.add(new AccessControlProfile.Builder(0)
                .setUserAuthenticationRequired(true)
                .build());
        profiles.add(new AccessControlProfile.Builder(1)
                .setUserAuthenticationRequired(false)
                .setReaderCertificate(certA)
                .build());
        LinkedList<EntryNamespace> entryNamespaces = new LinkedList<>();
        Collection<Integer> idsProfile0and1 = new ArrayList<Integer>();
        idsProfile0and1.add(0);
        idsProfile0and1.add(1);
        entryNamespaces.add(
                new EntryNamespace.Builder("org.iso.18013-5.2019")
                        .addStringEntry("User/reader protected", idsProfile0and1, "bsg75")
                        .build());
        byte[] proofOfProvisioningCbor = wc.personalize(profiles, entryNamespaces);

        String pretty = cborPrettyPrint(proofOfProvisioningCbor);
        pretty = Util.replaceLine(pretty, 11, "        'readerCertificate' : [] // Removed");
        pretty = Util.replaceLine(pretty, -2, "  [] // Signature Removed");
        Log.d(TAG, "pretty: " + pretty);
        assertEquals("[\n" +
                "  [\n" +
                "    'ProofOfProvisioning',\n" +
                "    'org.iso.18013-5.2019.mdl',\n" +
                "    [\n" +
                "      {\n" +
                "        'id' : 0,\n" +
                "        'capabilityType' : 1\n" +
                "      },\n" +
                "      {\n" +
                "        'id' : 1,\n" +
                "        'readerCertificate' : [] // Removed\n" +
                "      }\n" +
                "    ],\n" +
                "    {\n" +
                "      'org.iso.18013-5.2019' : [\n" +
                "        {\n" +
                "          'name' : 'User/reader protected',\n" +
                "          'value' : 'bsg75',\n" +
                "          'accessControlProfiles' : [0, 1]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    false\n" +
                "  ],\n" +
                "  [] // Signature Removed\n" +
                "]", pretty);

        // Get the credential we'll be reading from and provision it with a sufficient number
        // of dynamic auth keys
        IdentityCredential credential = store.getCredentialByName("test");
        assertNotNull(credential);
        credential.setAvailableAuthenticationKeys(1, 10);
        Collection<X509Certificate> dynAuthKeyCerts = credential.getAuthKeysNeedingCertification();
        credential.storeStaticAuthenticationData(dynAuthKeyCerts.iterator().next(), new byte[0]);

        IdentityCredential.GetEntryResult result;
        Collection<String> entryNames;
        Collection<ResultNamespace> resultNamespaces;
        ResultNamespace ns;
        LinkedList<RequestNamespace> requestEntryNamespaces;

        // Without the Auth Oracle or reader auth, retrieval should fail...
        requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("User/reader protected")
                        .build());
        result = credential.getEntries(RequestNamespace.createItemsRequest(requestEntryNamespaces,
                null),
                requestEntryNamespaces,
                null,
                null,
                null);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(0, ns.getRetrievedEntryNames().size());
        assertEquals(1, ns.getEntryNames().size());

        // With reader auth (but without Auth Oracle), it'll work...
        credential = store.getCredentialByName("test");
        result = userAndReaderRetrievalHelper(credential, readerKeyPairA, certChainForA);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(1, ns.getRetrievedEntryNames().size());
        assertEquals(ResultNamespace.STATUS_OK, ns.getStatus("User/reader protected"));
        assertEquals("bsg75", ns.getStringEntry("User/reader protected"));

        // With Auth Oracle (but without reader auth) it'll also work...
        credential = store.getCredentialByName("test");
        ((SoftwareIdentityCredential) credential).setInternalUserAuthenticationOracle(
                new SoftwareIdentityCredential.InternalUserAuthenticationOracle() {
                    @Override
                    boolean checkUserAuthentication(int accessControlProfileId) {
                        return true;
                    }
                });
        result = credential.getEntries(
                RequestNamespace.createItemsRequest(requestEntryNamespaces, null),
                requestEntryNamespaces,
                null,
                null,
                null);
        assertNotNull(result);
        resultNamespaces = result.getEntryNamespaces();
        assertEquals(1, resultNamespaces.size());
        ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(1, ns.getRetrievedEntryNames().size());
        assertEquals(ResultNamespace.STATUS_OK, ns.getStatus("User/reader protected"));
        assertEquals("bsg75", ns.getStringEntry("User/reader protected"));
    }

    private IdentityCredential.GetEntryResult userAndReaderRetrievalHelper(
            IdentityCredential credential,
            KeyPair readerKeyToSignWith,
            Collection<X509Certificate> readerCertificateChainToPresent)
            throws IdentityCredentialException, CborException, InvalidAlgorithmParameterException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {

        // Create the request message which will be signed by the reader.
        LinkedList<RequestNamespace> requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("User/reader protected")
                        .build());
        byte[] requestMessage = RequestNamespace.createItemsRequest(requestEntryNamespaces,
                "org.iso.18013-5.2019.mdl");

        // To issue the request we'll need a SessionTranscript. We'll create a fake one.
        ByteArrayOutputStream stBaos = new ByteArrayOutputStream();
        try {
            new CborEncoder(stBaos).encode(new CborBuilder()
                    .addArray()
                    .add(new byte[]{0x01, 0x02})  // The device engagement structure, encoded
                    .add(new byte[]{0x03, 0x04})  // Reader ephemeral public key, encoded
                    .end()
                    .build());
        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        byte[] sessionTranscript = stBaos.toByteArray();

        // Finally, create the structure that the reader signs, and sign it.
        ByteArrayOutputStream raBaos = new ByteArrayOutputStream();
        new CborEncoder(raBaos).encode(new CborBuilder()
                .addArray()
                .add("ReaderAuthentication")
                .add(sessionTranscript)
                .add(requestMessage)
                .end()
                .build());
        byte[] dataToBeSignedByReader = raBaos.toByteArray();
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(readerKeyToSignWith.getPrivate());
        s.update(dataToBeSignedByReader);
        byte[] readerSignature = s.sign();

        // Now issue the request.
        IdentityCredential.GetEntryResult result = credential.getEntries(
                requestMessage,
                requestEntryNamespaces,
                sessionTranscript,
                readerCertificateChainToPresent,
                readerSignature);
        return result;
    }

}
