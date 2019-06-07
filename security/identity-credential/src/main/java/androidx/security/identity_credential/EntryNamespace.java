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

    private static class EntryData {
        EntryData(Object value, Collection<Integer> accessControlProfileIds) {
            this.mValue = value;
            this.mAccessControlProfileIds = accessControlProfileIds;
        }

        Object mValue;

        Collection<Integer> mAccessControlProfileIds;
    }

    private String mNamespace;
    private LinkedHashMap<String, EntryData> mEntries = new LinkedHashMap<>();

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
         * @param name The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value The value to add.
         * @return The builder.
         */
        public Builder addBooleanEntry(String name, Collection<Integer> accessControlProfileIds,
                                       boolean value) {
            return addEntry(name, accessControlProfileIds, value);
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value The value to add.
         * @return The builder.
         */
        public Builder addLongEntry(String name, Collection<Integer> accessControlProfileIds,
                                    long value) {
            return addEntry(name, accessControlProfileIds, value);
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value The value to add.
         * @return The builder.
         */
        public Builder addIntEntry(String name, Collection<Integer> accessControlProfileIds,
                                   int value) {
            return addEntry(name, accessControlProfileIds, (long) value);
        }


        /**
         * Adds a new entry to the builder.
         *
         * @param name The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value The value to add.
         * @return The builder.
         */
        public Builder addBytestringEntry(String name, Collection<Integer> accessControlProfileIds,
                                          byte[] value) {
            return addEntry(name, accessControlProfileIds, value);
        }

        /**
         * Adds a new entry to the builder.
         *
         * @param name The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value The value to add.
         * @return The builder.
         */
        public Builder addStringEntry(String name, Collection<Integer> accessControlProfileIds,
                                      String value) {
            return addEntry(name, accessControlProfileIds, value);
        }

        private Builder addEntry(String name, Collection<Integer> accessControlProfileIds, Object value) {
            mEntryNamespace.mEntries.put(name, new EntryData(value, accessControlProfileIds));
            return this;
        }

        /**
         * Creates a new {@link EntryNamespace} with all the entries added to the builder.
         *
         * @return A new {@link EntryNamespace} instance.
         *
         */
        public EntryNamespace build() {
            return mEntryNamespace;
        }
    }

    private EntryNamespace(String namespace) {
        this.mNamespace = namespace;
    }

    String getNamespaceName() {
        return mNamespace;
    }

    Collection<String> getEntryNames() {
        return Collections.unmodifiableCollection(mEntries.keySet());
    }

    boolean getBooleanEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof Boolean) {
            return (Boolean) value.mValue;
        }
        return false;
    }

    long getIntegerEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof Long) {
            return (Long) value.mValue;
        }
        return 0;
    }

    String getTextStringEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof String) {
            return (String) value.mValue;
        }
        return null;
    }

    byte[] getByteStringEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof byte[]) {
            return (byte[]) value.mValue;
        }
        return null;
    }

    Collection<Integer> getAccessControlProfileIds(String name) {
        EntryData value = mEntries.get(name);
        if (value != null) {
            return value.mAccessControlProfileIds;
        }
        return null;
    }

    Object getEntryValue(String name) {
        EntryData value = mEntries.get(name);
        if (value != null) {
            return value.mValue;
        }
        return null;
    }

}
