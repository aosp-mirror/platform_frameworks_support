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
import static androidx.security.identity_credential.ResultNamespace.STATUS_NOT_REQUESTED;
import static androidx.security.identity_credential.ResultNamespace.STATUS_NO_SUCH_ENTRY;
import static androidx.security.identity_credential.ResultNamespace.STATUS_OK;
import static androidx.security.identity_credential.Util.cborPrettyPrint;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.InstrumentationRegistry;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ProvisioningTest {
    private static final String TAG = "ProvisioningTest";

    private static byte[] getExampleDrivingPrivilegesCbor() {
        // As per 7.4.4 of ISO 18013-5, driving privileges are defined with the following CDDL:
        //
        // driving_privileges = [
        //     * driving_privilege
        // ]
        //
        // driving_privilege = {
        //     vehicle_category_code: tstr ; Vehicle category code as per ISO 18013-2 Annex A
        //     ? issue_date: #6.0(tstr)    ; Date of issue encoded as full-date per RFC 3339
        //     ? expiry_date: #6.0(tstr)   ; Date of expiry encoded as full-date per RFC 3339
        //     ? code: tstr                ; Code as per ISO 18013-2 Annex A
        //     ? sign: tstr                ; Sign as per ISO 18013-2 Annex A
        //     ? value: int                ; Value as per ISO 18013-2 Annex A
        // }
        //
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addArray()
                    .addMap()
                    .put(new UnicodeString("vehicle_category_code"), new UnicodeString("TODO"))
                    .put(new UnicodeString("value"), new UnsignedInteger(42))
                    .end()
                    .end()
                    .build());
        } catch (CborException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private static Calendar getExamplePortraitCaptureDate() {
        Calendar portraitCaptureDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        portraitCaptureDate.clear();
        portraitCaptureDate.set(1932, Calendar.JUNE, 23);
        return portraitCaptureDate;
    }

    static Collection<X509Certificate> createCredential(IdentityCredentialStore store,
            String credentialName) throws IdentityCredentialException {
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
                .setUserAuthenticationRequired(false)
                .build());

        byte[] drivingPrivileges = getExampleDrivingPrivilegesCbor();
        Calendar portraitCaptureDate = getExamplePortraitCaptureDate();

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
                        .addBytestringEntry("Portrait image", idsNoAuth, new byte[]{0x01, 0x02})
                        .addIntegerEntry("Height", idsNoAuth, 180)
                        .addEntry("driving_privileges", idsNoAuth, drivingPrivileges)
                        .addCalendarEntry("portrait_capture_date", idsNoAuth, portraitCaptureDate)
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
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'driving_privileges',\n" +
                    "          'value' : [\n" +
                    "            {\n" +
                    "              'value' : 42,\n" +
                    "              'vehicle_category_code' : 'TODO'\n" +
                    "            }\n" +
                    "          ],\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'portrait_capture_date',\n" +
                    "          'value' : tag 0 '1932-06-23T00:00:00+00:00',\n" +
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

        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        return certificateChain;
    }

    static Collection<X509Certificate> createCredentialMultipleNamespaces(
            IdentityCredentialStore store,
            String credentialName) {
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
                .setUserAuthenticationRequired(false)
                .build());

        LinkedList<EntryNamespace> entryNamespaces = new LinkedList<>();
        Collection<Integer> idsNoAuth = new ArrayList<Integer>();
        idsNoAuth.add(0);
        entryNamespaces.add(
                new EntryNamespace.Builder("org.example.barfoo")
                        .addStringEntry("Bar", idsNoAuth, "Foo")
                        .addStringEntry("Foo", idsNoAuth, "Bar")
                        .build());
        entryNamespaces.add(
                new EntryNamespace.Builder("org.example.foobar")
                        .addStringEntry("Foo", idsNoAuth, "Bar")
                        .addStringEntry("Bar", idsNoAuth, "Foo")
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
                    "      'org.example.barfoo' : [\n" +
                    "        {\n" +
                    "          'name' : 'Bar',\n" +
                    "          'value' : 'Foo',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Foo',\n" +
                    "          'value' : 'Bar',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      'org.example.foobar' : [\n" +
                    "        {\n" +
                    "          'name' : 'Foo',\n" +
                    "          'value' : 'Bar',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          'name' : 'Bar',\n" +
                    "          'value' : 'Foo',\n" +
                    "          'accessControlProfiles' : [0]\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    false\n" +
                    "  ],\n" +
                    "  [] // Signature Removed\n" +
                    "]", pretty);

        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        return certificateChain;
    }

    @Test
    public void alreadyPersonalized() throws IdentityCredentialException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        createCredential(store, "test");
        try {
            createCredential(store, "test");
            assertTrue(false);
        } catch (AlreadyPersonalizedException e) {
            // The expected path.
        }
        store.deleteCredentialByName("test");
    }

    @Test
    public void deleteCredential()
            throws IdentityCredentialException, CborException, CertificateEncodingException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        assertNull(store.deleteCredentialByName("test"));
        Collection<X509Certificate> certificateChain = createCredential(store, "test");

        // Deleting the credential involves destroying the keys referenced in the returned
        // certificateChain... so get an encoded blob we can turn into a X509 cert when
        // checking the deletion receipt below, post-deletion.
        byte[] encodedCredentialCert = certificateChain.iterator().next().getEncoded();

        byte[] proofOfDeletionCbor = store.deleteCredentialByName("test");
        assertNotNull(proofOfDeletionCbor);

        // Check the returned CBOR is what is expected.
        String pretty = cborPrettyPrint(proofOfDeletionCbor);
        pretty = Util.replaceLine(pretty, -2, "  [] // Signature Removed");
        assertEquals("[\n" +
                "  ['ProofOfDeletion', 'org.iso.18013-5.2019.mdl', false],\n" +
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
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certBais = new ByteArrayInputStream(encodedCredentialCert);
            X509Certificate credentialCert = (X509Certificate) cf.generateCertificate(certBais);
            verifier.initVerify(credentialCert);
            verifier.update(dataThatWasSigned);
            assertTrue(verifier.verify(signature));
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (CertificateException e) {
            e.printStackTrace();
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

        IdentityCredential credential = store.getCredentialByName("test");

        // Check that the read-back certChain matches the created one.
        Collection<X509Certificate> readBackCertChain =
                credential.getCredentialKeyCertificateChain();
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
                        .addEntryName("driving_privileges")
                        .addEntryName("portrait_capture_date")
                        .build());
        IdentityCredential.GetEntryResult result = credential.getEntries(
                RequestNamespace.createItemsRequest(requestEntryNamespaces, null),
                requestEntryNamespaces,
                null,
                null,
                null);

        Collection<ResultNamespace> resultNamespaces = result.getEntryNamespaces();
        assertEquals(resultNamespaces.size(), 1);
        ResultNamespace ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(9, ns.getEntryNames().size());

        assertEquals("Alan", ns.getStringEntry("First name"));
        assertEquals("Turing", ns.getStringEntry("Last name"));
        assertEquals("Maida Vale, London, England", ns.getStringEntry("Home address"));
        assertEquals("19120623", ns.getStringEntry("Birth date"));
        assertEquals(true, ns.getBooleanEntry("Cryptanalyst"));
        assertArrayEquals(new byte[]{0x01, 0x02}, ns.getBytestringEntry("Portrait image"));
        assertEquals(180, ns.getIntegerEntry("Height"));
        byte[] drivingPrivileges = getExampleDrivingPrivilegesCbor();
        assertArrayEquals(drivingPrivileges, ns.getEntry("driving_privileges"));
        Calendar portraitCaptureDate = getExamplePortraitCaptureDate();
        assertEquals(0, portraitCaptureDate.compareTo(ns.getCalendarEntry("portrait_capture_date")));

        assertEquals("{\n"
                + "  'org.iso.18013-5.2019' : {\n"
                + "    'Height' : 180,\n"
                + "    'Last name' : 'Turing',\n"
                + "    'Birth date' : '19120623',\n"
                + "    'First name' : 'Alan',\n"
                + "    'Cryptanalyst' : true,\n"
                + "    'Home address' : 'Maida Vale, London, England',\n"
                + "    'Portrait image' : [0x01, 0x02],\n"
                + "    'driving_privileges' : [\n"
                + "      {\n"
                + "        'value' : 42,\n"
                + "        'vehicle_category_code' : 'TODO'\n"
                + "      }\n"
                + "    ],\n"
                + "    'portrait_capture_date' : tag 0 '1932-06-23T00:00:00+00:00'\n"
                + "  }\n"
                + "}", Util.cborPrettyPrint(result.getAuthenticatedData()));

        store.deleteCredentialByName("test");
    }

    @Test
    public void testProvisionAndRetrieveNoDeviceSigned()
            throws IdentityCredentialException, CborException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        Collection<X509Certificate> certChain = createCredential(store, "test");

        IdentityCredential credential = store.getCredentialByName("test");

        // Check that the read-back certChain matches the created one.
        Collection<X509Certificate> readBackCertChain =
                credential.getCredentialKeyCertificateChain();
        assertEquals(certChain.size(), readBackCertChain.size());
        Iterator<X509Certificate> it = readBackCertChain.iterator();
        for (X509Certificate expectedCert : certChain) {
            X509Certificate readBackCert = it.next();
            assertEquals(expectedCert, readBackCert);
        }

        LinkedList<RequestNamespace> requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryNameNoAuthentication("First name")
                        .addEntryNameNoAuthentication("Last name")
                        .addEntryNameNoAuthentication("Home address")
                        .addEntryNameNoAuthentication("Birth date")
                        .addEntryNameNoAuthentication("Cryptanalyst")
                        .addEntryNameNoAuthentication("Portrait image")
                        .addEntryNameNoAuthentication("Height")
                        .addEntryNameNoAuthentication("driving_privileges")
                        .addEntryNameNoAuthentication("portrait_capture_date")
                        .build());
        IdentityCredential.GetEntryResult result = credential.getEntries(
                RequestNamespace.createItemsRequest(requestEntryNamespaces, null),
                requestEntryNamespaces,
                null,
                null,
                null);

        Collection<ResultNamespace> resultNamespaces = result.getEntryNamespaces();
        assertEquals(resultNamespaces.size(), 1);
        ResultNamespace ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(9, ns.getEntryNames().size());

        assertEquals("Alan", ns.getStringEntry("First name"));
        assertEquals("Turing", ns.getStringEntry("Last name"));
        assertEquals("Maida Vale, London, England", ns.getStringEntry("Home address"));
        assertEquals("19120623", ns.getStringEntry("Birth date"));
        assertEquals(true, ns.getBooleanEntry("Cryptanalyst"));
        assertArrayEquals(new byte[]{0x01, 0x02}, ns.getBytestringEntry("Portrait image"));
        assertEquals(180, ns.getIntegerEntry("Height"));
        byte[] drivingPrivileges = getExampleDrivingPrivilegesCbor();
        assertArrayEquals(drivingPrivileges, ns.getEntry("driving_privileges"));
        Calendar portraitCaptureDate = getExamplePortraitCaptureDate();
        assertEquals(0, portraitCaptureDate.compareTo(ns.getCalendarEntry("portrait_capture_date")));

        // Checks that DeviceSigned is empty - this is because we used the NoAuthentication
        // variant of addEntryName() above.
        assertEquals("{}", Util.cborPrettyPrint(result.getAuthenticatedData()));

        store.deleteCredentialByName("test");
    }


    @Test
    public void testProvisionAndRetrieveWithFiltering() throws IdentityCredentialException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        Collection<X509Certificate> certChain = createCredential(store, "test");

        IdentityCredential credential = store.getCredentialByName("test");

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
        LinkedList<RequestNamespace> requestEntryNamespacesWithoutHomeAddress = new LinkedList<>();
        requestEntryNamespacesWithoutHomeAddress.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("First name")
                        .addEntryName("Last name")
                        .addEntryName("Birth date")
                        .addEntryName("Cryptanalyst")
                        .addEntryName("Portrait image")
                        .addEntryName("Height")
                        .build());
        IdentityCredential.GetEntryResult result = credential.getEntries(
                RequestNamespace.createItemsRequest(requestEntryNamespaces, null),
                requestEntryNamespacesWithoutHomeAddress,
                null,
                null,
                null);

        Collection<ResultNamespace> resultNamespaces = result.getEntryNamespaces();
        assertEquals(resultNamespaces.size(), 1);
        ResultNamespace ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(6, ns.getEntryNames().size());

        assertEquals("Alan", ns.getStringEntry("First name"));
        assertEquals("Turing", ns.getStringEntry("Last name"));
        assertEquals("19120623", ns.getStringEntry("Birth date"));
        assertEquals(true, ns.getBooleanEntry("Cryptanalyst"));
        assertArrayEquals(new byte[]{0x01, 0x02}, ns.getBytestringEntry("Portrait image"));
        assertEquals(180, ns.getIntegerEntry("Height"));

        store.deleteCredentialByName("test");
    }

    @Test
    public void nonExistentEntries() throws IdentityCredentialException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        Collection<X509Certificate> certChain = createCredential(store, "test");

        IdentityCredential credential = store.getCredentialByName("test");

        LinkedList<RequestNamespace> requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.iso.18013-5.2019")
                        .addEntryName("First name")
                        .addEntryName("Last name")
                        .addEntryName("Non-existent Entry")
                        .build());
        IdentityCredential.GetEntryResult result = credential.getEntries(
                null,
                requestEntryNamespaces,
                null,
                null,
                null);

        Collection<ResultNamespace> resultNamespaces = result.getEntryNamespaces();
        assertEquals(resultNamespaces.size(), 1);
        ResultNamespace ns = resultNamespaces.iterator().next();
        assertEquals("org.iso.18013-5.2019", ns.getNamespaceName());
        assertEquals(3, ns.getEntryNames().size());
        assertEquals(2, ns.getRetrievedEntryNames().size());

        assertEquals(STATUS_OK, ns.getStatus("First name"));
        assertEquals(STATUS_OK, ns.getStatus("Last name"));
        assertEquals(STATUS_NO_SUCH_ENTRY, ns.getStatus("Non-existent Entry"));
        assertEquals(STATUS_NOT_REQUESTED, ns.getStatus("Entry not even requested"));

        assertEquals("Alan", ns.getStringEntry("First name"));
        assertEquals("Turing", ns.getStringEntry("Last name"));
        assertNull(ns.getStringEntry("Non-existent Entry"));
        assertNull(ns.getStringEntry("Entry not even requested"));

        store.deleteCredentialByName("test");
    }

    @Test
    public void multipleNamespaces() throws IdentityCredentialException, CborException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = IdentityCredentialStore.getInstance(appContext);

        store.deleteCredentialByName("test");
        Collection<X509Certificate> certChain = createCredentialMultipleNamespaces(
                store, "test");

        IdentityCredential credential = store.getCredentialByName("test");

        // Request these in different order than they are stored
        LinkedList<RequestNamespace> requestEntryNamespaces = new LinkedList<>();
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.example.foobar")
                        .addEntryName("Foo")
                        .addEntryName("Bar")
                        .addEntryName("Non-exist")
                        .build());
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.example.barfoo")
                        .addEntryName("Bar")
                        .addEntryName("Non-exist")
                        .addEntryName("Foo")
                        .build());
        requestEntryNamespaces.add(
                new RequestNamespace.Builder("org.example.foofoo")
                        .addEntryName("Bar")
                        .addEntryName("Foo")
                        .addEntryName("Non-exist")
                        .build());
        IdentityCredential.GetEntryResult result = credential.getEntries(
                null,
                requestEntryNamespaces,
                null,
                null,
                null);

        // We should get the same number of namespaces back, as we requested - even for namespaces
        // that do not exist in the credential.
        //
        // Additionally, each namespace should have exactly the items requested, in the same order.
        Collection<ResultNamespace> resultNamespaces = result.getEntryNamespaces();
        assertEquals(resultNamespaces.size(), 3);

        ResultNamespace ns;
        Iterator<ResultNamespace> resultNamespacesIt = resultNamespaces.iterator();

        // First requested namespace - org.example.foobar
        ns = resultNamespacesIt.next();
        assertEquals("org.example.foobar", ns.getNamespaceName());
        assertArrayEquals(new String[]{"Foo", "Bar", "Non-exist"}, ns.getEntryNames().toArray());
        assertArrayEquals(new String[]{"Foo", "Bar"}, ns.getRetrievedEntryNames().toArray());

        assertEquals(STATUS_OK, ns.getStatus("Foo"));
        assertEquals(STATUS_OK, ns.getStatus("Bar"));
        assertEquals(STATUS_NO_SUCH_ENTRY, ns.getStatus("Non-exist"));
        assertEquals(STATUS_NOT_REQUESTED, ns.getStatus("Entry not even requested"));

        assertEquals("Bar", ns.getStringEntry("Foo"));
        assertEquals("Foo", ns.getStringEntry("Bar"));
        assertNull(ns.getStringEntry("Non-exist"));
        assertNull(ns.getStringEntry("Entry not even requested"));

        // Second requested namespace - org.example.barfoo
        ns = resultNamespacesIt.next();
        assertEquals("org.example.barfoo", ns.getNamespaceName());
        assertArrayEquals(new String[]{"Bar", "Non-exist", "Foo"}, ns.getEntryNames().toArray());
        assertArrayEquals(new String[]{"Bar", "Foo"}, ns.getRetrievedEntryNames().toArray());

        assertEquals(STATUS_OK, ns.getStatus("Foo"));
        assertEquals(STATUS_OK, ns.getStatus("Bar"));
        assertEquals(STATUS_NO_SUCH_ENTRY, ns.getStatus("Non-exist"));
        assertEquals(STATUS_NOT_REQUESTED, ns.getStatus("Entry not even requested"));

        assertEquals("Bar", ns.getStringEntry("Foo"));
        assertEquals("Foo", ns.getStringEntry("Bar"));
        assertNull(ns.getStringEntry("Non-exist"));
        assertNull(ns.getStringEntry("Entry not even requested"));

        // Third requested namespace - org.example.foofoo
        ns = resultNamespacesIt.next();
        assertEquals("org.example.foofoo", ns.getNamespaceName());
        assertArrayEquals(new String[]{"Bar", "Foo", "Non-exist"}, ns.getEntryNames().toArray());
        assertEquals(0, ns.getRetrievedEntryNames().size());
        assertEquals(STATUS_NO_SUCH_ENTRY, ns.getStatus("Foo"));
        assertEquals(STATUS_NO_SUCH_ENTRY, ns.getStatus("Bar"));
        assertEquals(STATUS_NO_SUCH_ENTRY, ns.getStatus("Non-exist"));
        assertEquals(STATUS_NOT_REQUESTED, ns.getStatus("Entry not even requested"));

        // Now check the returned CBOR ... note how it only has entries _and_ namespaces
        // for data that was returned. Also note how the data is in canonical order which
        // is fine since the mDL standard says in "7.2 Encoding of data structure and data
        // elements": The fourth rule regarding sorting of map keys is not required.
        //
        // Importantly, this is unlike the returned Collection<ResultNamespace> which mirrors
        // one to one the passed in Collection<RequestNamespace> structure, _including_ ensuring
        // the order is the same ... (which we - painfully - test for just above.)
        byte[] resultCbor = result.getAuthenticatedData();
        String pretty = cborPrettyPrint(resultCbor);
        assertEquals("{\n"
                + "  'org.example.barfoo' : {\n"
                + "    'Bar' : 'Foo',\n"
                + "    'Foo' : 'Bar'\n"
                + "  },\n"
                + "  'org.example.foobar' : {\n"
                + "    'Bar' : 'Foo',\n"
                + "    'Foo' : 'Bar'\n"
                + "  }\n"
                + "}", pretty);

        store.deleteCredentialByName("test");
    }


}
