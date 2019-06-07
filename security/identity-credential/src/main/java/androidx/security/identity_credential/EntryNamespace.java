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

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * An object that contains a set of data entries in one namespace. This is used to provision data
 * into a {@link WritableIdentityCredential}.
 *
 * @see WritableIdentityCredential#personalize
 */
public class EntryNamespace {

    private String mNamespace;
    private LinkedHashMap<String, EntryData> mEntries = new LinkedHashMap<>();

    private EntryNamespace(String namespace) {
        this.mNamespace = namespace;
    }

    String getNamespaceName() {
        return mNamespace;
    }

    Collection<String> getEntryNames() {
        return Collections.unmodifiableCollection(mEntries.keySet());
    }

    Collection<Integer> getAccessControlProfileIds(String name) {
        EntryData value = mEntries.get(name);
        if (value != null) {
            return value.mAccessControlProfileIds;
        }
        return null;
    }

    byte[] getEntryValue(String name) {
        EntryData value = mEntries.get(name);
        if (value != null) {
            return value.mValue;
        }
        return null;
    }

    private static class EntryData {
        byte[] mValue;
        Collection<Integer> mAccessControlProfileIds;

        EntryData(byte[] value, Collection<Integer> accessControlProfileIds) {
            this.mValue = value;
            this.mAccessControlProfileIds = accessControlProfileIds;
        }
    }

    /**
     * A builder for {@link EntryNamespace}.
     */
    public static class Builder {
        private EntryNamespace mEntryNamespace;

        /**
         * Creates a new builder for a given namespace.
         *
         * @param namespace The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         */
        public Builder(String namespace) {
            this.mEntryNamespace = new EntryNamespace(namespace);
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        public Builder addBooleanEntry(String name, Collection<Integer> accessControlProfileIds,
                boolean value) {
            return addEntry(name, accessControlProfileIds, Util.cborEncodeBoolean(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        public Builder addIntegerEntry(String name, Collection<Integer> accessControlProfileIds,
                int value) {
            return addEntry(name, accessControlProfileIds, Util.cborEncodeInt(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        public Builder addBytestringEntry(String name, Collection<Integer> accessControlProfileIds,
                byte[] value) {
            return addEntry(name, accessControlProfileIds, Util.cborEncodeBytestring(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        public Builder addStringEntry(String name, Collection<Integer> accessControlProfileIds,
                String value) {
            return addEntry(name, accessControlProfileIds, Util.cborEncodeString(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        public Builder addCalendarEntry(String name, Collection<Integer> accessControlProfileIds,
                Calendar value) {
            return addEntry(name, accessControlProfileIds, Util.cborEncodeCalendar(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add, in CBOR encoding.
         * @return The builder.
         */
        public Builder addEntry(String name, Collection<Integer> accessControlProfileIds,
                byte[] value) {
            // TODO: validate/verify that value is proper CBOR.
            mEntryNamespace.mEntries.put(name, new EntryData(value, accessControlProfileIds));
            return this;
        }

        /**
         * Creates a new {@link EntryNamespace} with all the entries added to the builder.
         *
         * @return A new {@link EntryNamespace} instance.
         */
        public EntryNamespace build() {
            return mEntryNamespace;
        }
    }

}
