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

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * An object that contains a set of data entries in one namespace. This is used to return
 * data data requested from a {@link IdentityCredential}.
 *
 * @see IdentityCredential#getEntries
 */
public class ResultNamespace {

    @Retention(SOURCE)
    @IntDef({STATUS_OK, STATUS_NO_SUCH_ENTRY,
            STATUS_USER_AUTHENTICATION_FAILED,
            STATUS_READER_AUTHENTICATION_FAILED,
            STATUS_NO_ACCESS_CONTROL_PROFILES})
    public @interface Status {}
    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_SUCH_ENTRY = 1;
    public static final int STATUS_USER_AUTHENTICATION_FAILED = 2;
    public static final int STATUS_READER_AUTHENTICATION_FAILED = 3;
    public static final int STATUS_NO_ACCESS_CONTROL_PROFILES = 4;

    private static class EntryData {
        EntryData(Object value, @Status int status) {
            this.mValue = value;
            this.mStatus = status;
        }

        @Status
        int mStatus;

        Object mValue;
    }

    private String mNamespace;
    private Map<String, EntryData> mEntries = new HashMap<>();

    static class Builder {
        private ResultNamespace mEntryNamespace;

        Builder(String namespace) {
            this.mEntryNamespace = new ResultNamespace(namespace);
        }

        Builder addBooleanEntry(String name, boolean value) {
            return addEntry(name, value);
        }

        Builder addLongEntry(String name, long value) {
            return addEntry(name, value);
        }

        Builder addIntEntry(String name, int value) {
            return addEntry(name, (long) value);
        }

        Builder addBytestringEntry(String name, byte[] value) {
            return addEntry(name, value);
        }

        Builder addStringEntry(String name, String value) {
            return addEntry(name, value);
        }

        Builder addEntry(String name, Object value) {
            mEntryNamespace.mEntries.put(name, new EntryData(value, STATUS_OK));
            return this;
        }

        Builder addErrorStatus(String name, @Status int status) {
            mEntryNamespace.mEntries.put(name, new EntryData(null, status));
            return this;
        }

        ResultNamespace build() {
            return mEntryNamespace;
        }
    }

    private ResultNamespace(String namespace) {
        this.mNamespace = namespace;
    }

    /**
     * Gets the name of the namespace.
     *
     * @return The name of the namespace, e.g. {@code org.iso.18013-5.2019}.
     */
    public String getNamespaceName() {
        return mNamespace;
    }

    /**
     * Get the names of all entries.
     *
     * This includes the name of entries that wasn't successfully retrieved.
     *
     * @return A collection of names.
     */
    public Collection<String> getEntryNames() {
        return Collections.unmodifiableCollection(mEntries.keySet());
    }

    /**
     * Get the names of all entries that was successfully retrieved.
     *
     * This only return entries for which {@link #getStatus(String)} will return {@link #STATUS_OK}.
     *
     * @return A collection of names.
     */
    public Collection<String> getRetrievedEntryNames() {
        LinkedList<String> result = new LinkedList<String>();
        for (Map.Entry<String, EntryData> entry : mEntries.entrySet()) {
            if (entry.getValue().mStatus == STATUS_OK) {
                result.add(entry.getKey());
            }
        }
        return result;
    }


    /**
     * Gets the status of an entry.
     *
     * This returns {@link #STATUS_OK} if the value was retrieved, {@link #STATUS_NO_SUCH_ENTRY}
     * if the given entry wasn't requested, {@link #STATUS_USER_AUTHENTICATION_FAILED} if the value
     * wasn't retrieved because the necessary user authentication wasn't performed,
     * {@link #STATUS_READER_AUTHENTICATION_FAILED} if the supplied reader certificate chain
     * didn't match the set of certificates the entry was provisioned with, or
     * {@link #STATUS_NO_ACCESS_CONTROL_PROFILES} if the entry was configured without any
     * access control profiles.
     *
     * @param name the name of the entry to get the value for.
     * @return the status indicating whether the value was retrieved and if not, why.
     */
     @Status
     public int getStatus(String name) {
         EntryData value = mEntries.get(name);
         if (value != null) {
             return value.mStatus;
         }
         return STATUS_NO_SUCH_ENTRY;
     }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String)} method
     * returns {@link #STATUS_OK}.
     *
     * @param name the name of the entry to get the value for.
     * @return the value or {@code false} if no entry with the given name exists.
     */
    public boolean getBooleanEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof Boolean) {
            return (Boolean) value.mValue;
        }
        return false;
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String)} method
     * returns {@link #STATUS_OK}.
     *
     * @param name the name of the entry to get the value for.
     * @return the value or 0 if no entry with the given name exists.
     */
    public long getIntegerEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof Long) {
            return (Long) value.mValue;
        }
        return 0;
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String)} method
     * returns {@link #STATUS_OK}.
     *
     * @param name the name of the entry to get the value for.
     * @return the value or {@code null} if no entry with the given name exists.
     */
    public String getTextStringEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof String) {
            return (String) value.mValue;
        }
        return null;
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String)} method
     * returns {@link #STATUS_OK}.
     *
     * @param name the name of the entry to get the value for.
     * @return the value or {@code null} if no entry with the given name exists.
     */
    public byte[] getByteStringEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value != null && value.mValue != null && value.mValue instanceof byte[]) {
            return (byte[]) value.mValue;
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
