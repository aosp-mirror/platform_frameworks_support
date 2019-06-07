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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;

/**
 * An object that contains a set of requests for data entries in one namespace. This is used to
 * request data from a {@link IdentityCredential}.
 *
 * @see IdentityCredential#getEntries
 */
public class RequestNamespace {

    private String mNamespace;
    private Collection<String> mEntries = new ArrayList<>();

    private RequestNamespace(String namespace) {
        this.mNamespace = namespace;
    }

    /**
     * Helper function to create a CBOR data for requesting data items.
     *
     * <p>The returned CBOR data conforms to the following CDDL schema:</p>
     *
     * <pre>
     *   ItemsRequest = {
     *     ? "DocType" : DocType,
     *     "NameSpaces" : NameSpaces,
     *     ? "RequestInfo" : {* tstr => any} ; Additional info the reader wants to provide
     *   }
     *
     *   NameSpaces = {
     *     + NameSpace => DataItemNames ; Requested data elements for each NameSpace
     *   }
     *
     *   DocType = tstr
     *   NameSpace = tstr
     *   DataItemNames = [ + tstr ]
     * </pre>
     *
     * TODO: make it possible to pass RequestInfo.
     *
     * @param requestedEntryNamespaces A collection of {@link RequestNamespace} objects
     *                                 specifying all of the data
     *                                 elements to be retrieved, organized by namespace.
     * @param docType                  The document type or {@code null} if there is no document
     *                                 type.
     * @return CBOR data conforming to the CDDL mentioned above.
     */
    public static byte[] createItemsRequest(
            @NonNull Collection<RequestNamespace> requestedEntryNamespaces,
            @Nullable String docType) throws IdentityCredentialException {
        CborBuilder builder = new CborBuilder();
        MapBuilder<CborBuilder> mapBuilder = builder.addMap();
        if (docType != null) {
            mapBuilder.put("DocType", docType);
        }

        MapBuilder<MapBuilder<CborBuilder>> nsMapBuilder = mapBuilder.putMap("NameSpaces");
        for (RequestNamespace requestNamespace : requestedEntryNamespaces) {
            ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> entryNameArrayBuilder =
                    nsMapBuilder.putArray(requestNamespace.getNamespaceName());
            for (String entryName : requestNamespace.getEntryNames()) {
                entryNameArrayBuilder.add(entryName);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(baos);
        try {
            encoder.encode(builder.build());
        } catch (CborException e) {
            throw new IdentityCredentialException("Error encoding CBOR");
        }
        return baos.toByteArray();
    }

    String getNamespaceName() {
        return mNamespace;
    }

    Collection<String> getEntryNames() {
        return Collections.unmodifiableCollection(mEntries);
    }

    /**
     * A builder for {@link RequestNamespace}.
     */
    public static class Builder {
        private RequestNamespace mEntryNamespace;

        /**
         * Creates a new builder for a given namespace.
         *
         * @param namespace The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         */
        public Builder(String namespace) {
            this.mEntryNamespace = new RequestNamespace(namespace);
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name The name of the entry, e.g. {@code height}.
         * @return The builder.
         */
        public Builder addEntryName(String name) {
            mEntryNamespace.mEntries.add(name);
            return this;
        }

        /**
         * Creates a new {@link RequestNamespace} with all the entries added to the builder.
         *
         * @return A new {@link RequestNamespace} instance.
         */
        public RequestNamespace build() {
            return mEntryNamespace;
        }
    }
}
