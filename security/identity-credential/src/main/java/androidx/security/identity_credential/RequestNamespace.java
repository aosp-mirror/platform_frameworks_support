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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * An object that contains a set of requests for data entries in one namespace. This is used to
 * request data from a {@link IdentityCredential}.
 *
 * @see IdentityCredential#getEntries
 */
public class RequestNamespace {

    private String mNamespace;
    private Collection<String> mEntries = new ArrayList<>();

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
         *
         */
        public RequestNamespace build() {
            return mEntryNamespace;
        }
    }

    private RequestNamespace(String namespace) {
        this.mNamespace = namespace;
    }

    String getNamespaceName() {
        return mNamespace;
    }

    Collection<String> getEntryNames() {
        return Collections.unmodifiableCollection(mEntries);
    }

}
