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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * An object that contains a set of data entries in one namespace. This is used to return
 * data data requested from a {@link IdentityCredential}.
 *
 * @see IdentityCredential#getEntries
 */
public class ResultNamespace {

    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_SUCH_ENTRY = 1;
    public static final int STATUS_NOT_REQUESTED = 2;
    public static final int STATUS_USER_AUTHENTICATION_FAILED = 3;
    public static final int STATUS_READER_AUTHENTICATION_FAILED = 4;
    public static final int STATUS_NO_ACCESS_CONTROL_PROFILES = 5;
    private String mNamespace;
    private Map<String, EntryData> mEntries = new LinkedHashMap<>();

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
     * if the given entry wasn't retrieved, @{link #STATUS_NOT_REQUESTED} if it wasn't requested,
     * {@link #STATUS_USER_AUTHENTICATION_FAILED} if the value
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
        return STATUS_NOT_REQUESTED;
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
        if (value == null || value.mValue == null) {
            return false;
        }
        try {
            return Util.cborDecodeBoolean(value.mValue);
        } catch (IdentityCredentialException e) {
            return false;
        }
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
    public int getIntegerEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value == null || value.mValue == null) {
            return 0;
        }
        try {
            return Util.cborDecodeInt(value.mValue);
        } catch (IdentityCredentialException e) {
            return 0;
        }
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
    public String getStringEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value == null || value.mValue == null) {
            return null;
        }
        try {
            return Util.cborDecodeString(value.mValue);
        } catch (IdentityCredentialException e) {
            return null;
        }
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
    public byte[] getBytestringEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value == null || value.mValue == null) {
            return null;
        }
        try {
            return Util.cborDecodeBytestring(value.mValue);
        } catch (IdentityCredentialException e) {
            return null;
        }
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
    public Calendar getCalendarEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value == null || value.mValue == null) {
            return null;
        }
        try {
            return Util.cborDecodeCalendar(value.mValue);
        } catch (IdentityCredentialException e) {
            return null;
        }
    }


    /**
     * Gets the raw CBOR data for the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String)} method
     * returns {@link #STATUS_OK}.
     *
     * @param name the name of the entry to get the value for.
     * @return the raw CBOR data or {@code null} if no entry with the given name exists.
     */
    public byte[] getEntry(String name) {
        EntryData value = mEntries.get(name);
        if (value == null || value.mValue == null) {
            return null;
        }
        return value.mValue;
    }

    @Retention(SOURCE)
    @IntDef({STATUS_OK, STATUS_NO_SUCH_ENTRY,
            STATUS_NOT_REQUESTED,
            STATUS_USER_AUTHENTICATION_FAILED,
            STATUS_READER_AUTHENTICATION_FAILED,
            STATUS_NO_ACCESS_CONTROL_PROFILES})
    public @interface Status {
    }

    private static class EntryData {
        @Status
        int mStatus;
        byte[] mValue;

        EntryData(byte[] value, @Status int status) {
            this.mValue = value;
            this.mStatus = status;
        }
    }

    static class Builder {
        private ResultNamespace mEntryNamespace;

        Builder(String namespace) {
            this.mEntryNamespace = new ResultNamespace(namespace);
        }

        Builder addEntry(String name, byte[] value) {
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

}
