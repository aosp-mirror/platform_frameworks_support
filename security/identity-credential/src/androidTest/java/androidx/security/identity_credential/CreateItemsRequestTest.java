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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.LinkedList;

import co.nstant.in.cbor.CborException;

public class CreateItemsRequestTest {
    @Test
    public void basicRequest() throws IdentityCredentialException, CborException {
        LinkedList<RequestNamespace> requestedEntryNamespaces = new LinkedList<>();
        requestedEntryNamespaces.add(new RequestNamespace.Builder("org.test.ns")
                .addEntryName("xyz")
                .addEntryName("abc")
                .build());
        String docType = "org.test.ns";
        assertEquals("{\n"
                        + "  'DocType' : 'org.test.ns',\n"
                        + "  'NameSpaces' : {\n"
                        + "    'org.test.ns' : ['xyz', 'abc']\n"
                        + "  }\n"
                        + "}",
                Util.cborPrettyPrint(
                        RequestNamespace.createItemsRequest(requestedEntryNamespaces, docType)));
    }

    @Test
    public void multipleNamespaces() throws IdentityCredentialException, CborException {
        LinkedList<RequestNamespace> requestedEntryNamespaces = new LinkedList<>();
        requestedEntryNamespaces.add(new RequestNamespace.Builder("org.test.ns1")
                .addEntryName("foo")
                .addEntryName("bar")
                .build());
        requestedEntryNamespaces.add(new RequestNamespace.Builder("org.test.ns2")
                .addEntryName("xyz")
                .addEntryName("abc")
                .build());
        String docType = "org.test.ns";
        assertEquals("{\n"
                        + "  'DocType' : 'org.test.ns',\n"
                        + "  'NameSpaces' : {\n"
                        + "    'org.test.ns1' : ['foo', 'bar'],\n"
                        + "    'org.test.ns2' : ['xyz', 'abc']\n"
                        + "  }\n"
                        + "}",
                Util.cborPrettyPrint(
                        RequestNamespace.createItemsRequest(requestedEntryNamespaces, docType)));
    }

    @Test
    public void noDocType() throws IdentityCredentialException, CborException {
        LinkedList<RequestNamespace> requestedEntryNamespaces = new LinkedList<>();
        requestedEntryNamespaces.add(new RequestNamespace.Builder("org.test.ns1")
                .addEntryName("foo")
                .addEntryName("bar")
                .build());
        assertEquals("{\n"
                        + "  'NameSpaces' : {\n"
                        + "    'org.test.ns1' : ['foo', 'bar']\n"
                        + "  }\n"
                        + "}",
                Util.cborPrettyPrint(
                        RequestNamespace.createItemsRequest(requestedEntryNamespaces, null)));
    }

    @Test
    public void empty() throws IdentityCredentialException, CborException {
        LinkedList<RequestNamespace> requestedEntryNamespaces = new LinkedList<>();
        assertEquals("{\n"
                        + "  'NameSpaces' : {}\n"
                        + "}",
                Util.cborPrettyPrint(
                        RequestNamespace.createItemsRequest(requestedEntryNamespaces, null)));
    }
}
