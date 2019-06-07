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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;

import static androidx.security.identity_credential.IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256;
import static androidx.security.identity_credential.Util.cborPrettyPrint;
import static org.junit.Assert.*;

import androidx.test.InstrumentationRegistry;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ProvisioningTest {
    private static final String TAG = "ProvisioningTest";


    @Test
    public void deleteCredential() throws IdentityCredentialException, CborException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        assertNull(store.deleteCredentialByName("test"));
        Collection<X509Certificate> certificateChain = createCredential(store, "test");
        byte[] proofOfDeletionCbor = store.deleteCredentialByName("test");
        assertNotNull(proofOfDeletionCbor);

        // Check the returned CBOR is what is expected.
        String pretty = cborPrettyPrint(proofOfDeletionCbor);
        pretty = Util.replaceLine(pretty, -2, "  [] // Signature Removed");
        assertEquals("[\n" +
                "  [\n" +
                "    'ProofOfDeletion',\n" +
                "    'org.iso.18013-5.2019.mdl',\n" +
                "    [\n" +
                "      {\n" +
                "        'id' : 0\n" +
                "      }\n" +
                "    ],\n" +
                "    {\n" +
                "      'org.iso.18013-5.2019' : [\n" +
                "        {\n" +
                "          'name' : 'First name',\n" +
                "          'value' : 'Alan',\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Last name',\n" +
                "          'value' : 'Turing',\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Home address',\n" +
                "          'value' : 'Maida Vale, London, England',\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Birth date',\n" +
                "          'value' : '19120623',\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Cryptanalyst',\n" +
                "          'value' : true,\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Portrait image',\n" +
                "          'value' : [0x01, 0x02],\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        },\n" +
                "        {\n" +
                "          'name' : 'Height',\n" +
                "          'value' : 180,\n" +
                "          'accessControlProfiles' : [0]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    false\n" +
                "  ],\n" +
                "  [] // Signature Removed\n" +
                "]", pretty);

        // Now extract signature and check it was made by top-most key in
        // the certificate chain for the credential
        ByteArrayInputStream bais = new ByteArrayInputStream(proofOfDeletionCbor);
        DataItem item = new CborDecoder(bais).decode().get(0);
        Array outerArray = (Array) item;
        assertEquals(2, outerArray.getDataItems().size());
        DataItem itemThatWasSigned = outerArray.getDataItems().get(0);
        byte[] signature = ((ByteString) outerArray.getDataItems().get(1)).getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder enc = new CborEncoder(baos);
        enc.encode(itemThatWasSigned);
        byte[] dataThatWasSigned = baos.toByteArray();

        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(certificateChain.iterator().next());
            verifier.update(dataThatWasSigned);
            assertTrue(verifier.verify(signature));
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Finally, check deleting an already deleted credential returns the expected.
        assertNull(store.deleteCredentialByName("test"));
    }

    @Test
    public void testProvisionAndRetrieve() throws IdentityCredentialException, CborException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        Collection<X509Certificate> certChain = createCredential(store, "test");
        retrieveCredential(store, "test", certChain);
        store.deleteCredentialByName("test");
    }


    private void retrieveCredential(IdentityCredentialStore store, String credentialName,
                                    Collection<X509Certificate> certChain) throws IdentityCredentialException {
        IdentityCredential credential = store.getCredentialByName(credentialName);

        // Check that the read-back certChain matches the created one.
        Collection<X509Certificate> readBackCertChain = credential.getCredentialKeyCertificateChain();
        assertEquals(certChain.size(), readBackCertChain.size());
        Iterator<X509Certificate> it = readBackCertChain.iterator();
        for (X509Certificate expectedCert : certChain) {
            X509Certificate readBackCert = it.next();
            assertEquals(expectedCert, readBackCert);
        }

        LinkedList<RequestNamespace> requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("First name")
                        .addEntryName("Last name")
                        .addEntryName("Home address")
                        .addEntryName("Birth date")
                        .addEntryName("Cryptanalyst")
                        .addEntryName("Portrait image")
                        .addEntryName("Height")
                        .build());
        IdentityCredential.GetEntryResult result = credential.getEntries(requestEntryNamespaces,
        null,
        null,
        null,
        null);

        Collection<ResultNamespace> resultNamespaces = result.getEntryNamespaces();
        assertEquals(resultNamespaces.size(), 1);
        ResultNamespace ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(7, ns.getEntryNames().size());

        assertEquals("Alan", ns.getTextStringEntry("First name"));
        assertEquals("Turing", ns.getTextStringEntry("Last name"));
        assertEquals("Maida Vale, London, England", ns.getTextStringEntry("Home address"));
        assertEquals("19120623", ns.getTextStringEntry("Birth date"));
        assertEquals(true, ns.getBooleanEntry("Cryptanalyst"));
        assertArrayEquals(new byte[] {0x01, 0x02}, ns.getByteStringEntry("Portrait image"));
        assertEquals(180, ns.getIntegerEntry("Height"));
    }

    static Collection<X509Certificate> createCredential(IdentityCredentialStore store, String credentialName) {
        WritableIdentityCredential wc = null;
        try {
            wc = store.createCredential(credentialName,
                    "org.iso.18013-5.2019.mdl",
                    CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256);
        } catch (CipherSuiteNotSupportedException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        Collection<X509Certificate> certificateChain = null;
        try {
            certificateChain = wc.getCredentialKeyCertificateChain("SomeChallenge".getBytes());
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        LinkedList<AccessControlProfile> profiles = new LinkedList<>();

        // Profile 0 (no authentication)
        profiles.add(new AccessControlProfile.Builder(0)
                .build());

        LinkedList<EntryNamespace> entryNamespaces = new LinkedList<>();
        Collection<Integer> idsNoAuth = new ArrayList<Integer>();
        idsNoAuth.add(0);
        entryNamespaces.add(
                new EntryNamespace.Builder("org.iso.18013-5.2019")
                        .addStringEntry("First name", idsNoAuth, "Alan")
                        .addStringEntry("Last name", idsNoAuth, "Turing")
                        .addStringEntry("Home address", idsNoAuth, "Maida Vale, London, England")
                        .addStringEntry("Birth date", idsNoAuth, "19120623")
                        .addBooleanEntry("Cryptanalyst", idsNoAuth, true)
                        .addBytestringEntry("Portrait image", idsNoAuth, new byte[] {0x01, 0x02})
                        .addIntEntry("Height", idsNoAuth, 180)
                        .build());

        try {
            byte[] proofOfProvisioningCbor = wc.personalize(profiles, entryNamespaces);
            String pretty = cborPrettyPrint(proofOfProvisioningCbor);
            pretty = Util.replaceLine(pretty, -2, "  [] // Signature Removed");
            // Checks that order of elements is the order it was added, using the API.
            assertEquals("[\n" +
                    "  [\n" +
                    "    'ProofOfProvisioning',\n" +
                    "    'org.iso.18013-5.2019.mdl',\n" +
                    "    [\n" +
                    "      {\n" +
                    "        'id' : 0\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    {\n" +
                    "      'org.iso.18013-5.2019' : [\n" +
                    "        {\n" +
                    "          'name' : 'First name',\n" +
                    "          'value' : 'Alan',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Last name',\n" +
                    "          'value' : 'Turing',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Home address',\n" +
                    "          'value' : 'Maida Vale, London, England',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Birth date',\n" +
                    "          'value' : '19120623',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Cryptanalyst',\n" +
                    "          'value' : true,\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Portrait image',\n" +
                    "          'value' : [0x01, 0x02],\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Height',\n" +
                    "          'value' : 180,\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    false\n" +
                    "  ],\n" +
                    "  [] // Signature Removed\n" +
                    "]", pretty);

            // Now extract signature and check it was made by top-most key in
            // the certificate chain for the credential
            ByteArrayInputStream bais = new ByteArrayInputStream(proofOfProvisioningCbor);
            DataItem item = new CborDecoder(bais).decode().get(0);
            Array outerArray = (Array) item;
            assertEquals(2, outerArray.getDataItems().size());
            DataItem itemThatWasSigned = outerArray.getDataItems().get(0);
            byte[] signature = ((ByteString) outerArray.getDataItems().get(1)).getBytes();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder enc = new CborEncoder(baos);
            enc.encode(itemThatWasSigned);
            byte[] dataThatWasSigned = baos.toByteArray();

            try {
                Signature verifier = Signature.getInstance("SHA256withECDSA");
                verifier.initVerify(certificateChain.iterator().next());
                verifier.update(dataThatWasSigned);
                assertTrue(verifier.verify(signature));
            } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                e.printStackTrace();
                assertTrue(false);
            }

            // TODO: Check challenge is in certificatechain

            // TODO: Check each cert signs the next one

            // TODO: Check bottom cert is the Google well-know cert

            // TODO: need to also get and check SecurityStatement

        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        return certificateChain;
    }

}
