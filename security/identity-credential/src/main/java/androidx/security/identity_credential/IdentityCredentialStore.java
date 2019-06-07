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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * An interface to a secure store for user identity documents.
 *
 * <p>This interface is deliberately fairly general and abstract.  To the extent possible,
 * specification of the message formats and semantics of communication with credential
 * verification devices and issuing authorities (IAs) is out of scope. It provides the
 * interface with secure storage but a credential-specific Android application will be
 * required to implement the presentation and verification protocols and processes
 * appropriate for the specific credential type.
 *
 * <p>TODO: mention various backends, e.g. software emulation vs. calling into credstore/IC HAL
 * if on R.</p>
 *
 * <p>Multiple credentials can be created.  Each credential comprises:</p>
 * <ul>
 * <li>A document type, which is a string.</li>
 *
 * <li>A set of namespaces, which serve to disambiguate value names. It is recommended
 * that namespaces be structured as reverse domain names so that IANA effectively serves
 * as the namespace registrar.</li>
 *
 * <li>For each namespace, a set of name/value pairs, each with an associated set of
 * access control profile IDs.  Names are and values are typed and are either signed
 * integers, booleans, UTF-8 strings or bytestrings.</li>
 *
 * <li>A set of access control profiles, each with a profile ID and a specification
 * of the conditions which satisfy the profile's requirements.</li>
 *
 * <li>An asymmetric key pair which is used to authenticate the credential to the IA,
 * called the <em>CredentialKey</em>.</li>
 *
 * <li>A set of zero or more named reader authentication public keys, which are used to authenticate
 * an authorized reader to the credential.</li>
 *
 * <li>A set of named signing keys, which are used to sign collections of values and session
 * transcripts.</li>
 * </ul>
 */
public class IdentityCredentialStore {

    /**
     * Specifies that the cipher suite that will be used to secure communications between the reader
     * is:
     *
     * <ul>
     * <li>ECDHE with HKDF-SHA-256 for key agreement.</li>
     * <li>AES-256 with GCM block mode for authenticated encryption (nonces are incremented by one
     * for every message).</li>
     * <li>ECDSA with SHA-256 for signing (used for signing session transcripts to defeat
     * man-in-the-middle attacks), signing keys are not ephemeral. See {@link IdentityCredential}
     * for details on reader and prover signing keys.</li>
     * </ul>
     *
     * <p>
     * At present this is the only supported cipher suite.
     */
    public static final int CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256 = 1;
    private static final String TAG = "IdentityCredentialStore";
    private static IdentityCredentialStore sInstance = new IdentityCredentialStore();
    private Context mContext = null;

    private IdentityCredentialStore() {
    }

    /**
     * Get the {@link IdentityCredentialStore}.
     *
     * @param context the application context.
     * @return the {@link IdentityCredentialStore}.
     */
    public static IdentityCredentialStore getInstance(Context context) {
        sInstance.mContext = context;
        return sInstance;
    }

    /**
     * Creates a new credential.
     *
     * @param credentialName The name used to identify the credential.
     * @param docType        The document type for the credential.
     * @param cipherSuite    the cipher suite to use.
     * @return A @{link WritableIdentityCredential} that can be used to create a new credential.
     * @throws CipherSuiteNotSupportedException if the cipher suite is not supported
     */
    public WritableIdentityCredential createCredential(@NonNull String credentialName,
            @NonNull String docType,
            @Ciphersuite int cipherSuite) throws CipherSuiteNotSupportedException {
        // TODO: Also support CredstoreWritableIdentityCredential and prefer it, if available.
        return new SoftwareWritableIdentityCredential(mContext, credentialName, docType,
                cipherSuite);
    }

    /**
     * Retrieve a named credential.
     *
     * @param credentialName the name of the credential to retrieve.
     * @return The named credential, or null if not found.
     */
    public @Nullable
    IdentityCredential getCredentialByName(@NonNull String credentialName) {
        /* TODO: Also support CredstoreWritableIdentityCredential and check both
         * CredstoreIdentityCredential and SoftwareIdentityCredential in that order.
         */
        try {
            return new SoftwareIdentityCredential(mContext, credentialName);
        } catch (IdentityCredentialException e) {
            // Do nothing.
        }
        return null;
    }

    /**
     * Delete a named credential.
     *
     * <p>This method returns a <a href="https://tools.ietf.org/html/rfc7049">CBOR</a>
     * data structure with the document type, signed by the CredentialKey. The CBOR data structure
     * is defined by {@code ProofOfDeletion} in the following
     * <a href="https://tools.ietf.org/html/draft-ietf-cbor-cddl-06">CDDL</a> schema:</p>
     *
     * <pre>
     *     ProofOfDeletion = [
     *         DeletionSignedData,
     *         bstr                       ; ECDSA signature over SignedData
     *     ]
     *
     *     DeletionSignedData = [
     *          "ProofOfDeletion",            ; tstr
     *          tstr,                         ; DocType
     *          bool                          ; true if this is a test credential, should
     *                                        ; always be false.
     *      ]
     * </pre>
     *
     * @param credentialName the name of the credential to delete.
     * @return {@code null} if the credential was not found, the CBOR data structure above
     * if the credential was found and deleted.
     * @throws IdentityCredentialException if an error occurred.
     */
    public byte[] deleteCredentialByName(@NonNull String credentialName)
            throws IdentityCredentialException {
        return SoftwareIdentityCredential.delete(mContext, credentialName);
    }

    /** @hide */
    @IntDef(value = {CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_128_GCM_SHA256})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Ciphersuite {
    }

}
