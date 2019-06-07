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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Class used to personalize a new identity credential.
 *
 * <p>Credentials cannot be updated or modified after creation; any changes require deletion and
 * re-creation.</p>
 *
 * Use {@link IdentityCredentialStore#createCredential(String, String, int)} to create a new
 * credential.
 */
public abstract class WritableIdentityCredential {
    /**
     * <p>Generates and returns an X.509 certificate chain for the CredentialKey which identifies
     * this
     * credential to the issuing authority. The certificate contains a Keystore attestation
     * extension which describes the key and the security hardware in which it lives.</p>
     *
     * <p>TODO: add link with more information about Android Keystore attestation.</p>
     *
     * <p>It is not strictly necessary to use this method to provision a credential if the issuing
     * authority doesn't care about the nature of the security hardware. If called, however, this
     * method must be called before {@link #personalize(Collection, Collection)}.</p>
     *
     * <p>TODO: mention that this chain contains the SecurityStatement, what the format of this is,
     * and how to obtain it.</p>
     *
     * <p>The leaf-certificate in the returned chain - e.g. the certificate for CredentialKey - will
     * <em>not</em> be a CA certificate (e.g. the <code>keyCertSign</code> boolean in the
     * <code>KeyUsage</code> extension (OID = 2.5.29.15) will be false) despite the fact that
     * CredentialKey is used to sign X.509 certificates for dynamic authentication keys returned
     * by {@link IdentityCredential#getAuthKeysNeedingCertification()}.</p>
     *
     * @param challenge is a byte array whose contents should be unique, fresh and provided by
     *                  the issuing
     *                  authority. The value provided is embedded in the attestation extension
     *                  and enables
     *                  the issuing authority to verify that the attestation certificate is fresh.
     * @return the X.509 certificate for this credential's CredentialKey.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public @NonNull
    abstract Collection<X509Certificate> getCredentialKeyCertificateChain(
            @NonNull byte[] challenge) throws IdentityCredentialException;

    /**
     * Stores all of the data in the credential, with the specified access control profiles.
     *
     * <p>This method returns a <a href="https://tools.ietf.org/html/rfc7049">CBOR</a>
     * data structure
     * containing all of the data items stored and signed by the CredentialKey. The CBOR data
     * structure is defined by {@code ProofOfProvisioning} in the following
     * <a href="https://tools.ietf.org/html/draft-ietf-cbor-cddl-06">CDDL</a> schema:</p>
     *
     * <pre>
     *     ProofOfProvisioning = [
     *         SignedData,
     *         bstr                           ; ECDSA signature over SignedData
     *     ]
     *
     *     SignedData = [
     *          "ProofOfProvisioning",        ; tstr
     *          tstr,                         ; DocType
     *          [ * AccessControlProfile ],
     *          Data,
     *          bool                          ; true if this is a test credential, should
     *                                        ; always be false.
     *      ]
     *
     *      AccessControlProfile = {
     *          "id": uint,
     *          ? "readerCertificate" : bstr,
     *          ? (
     *              "capabilityType": uint
     *               ? "timeout": uint,
     *          )
     *      }
     *
     *      Data = {
     *          * Namespace =&gt; [ + Entry ]
     *      },
     *
     *      Namespace = tstr
     *
     *      Entry = {
     *          "name" : tstr,
     *          "accessControlProfiles" : [ * uint ],
     *          "value" : any,
     *      }
     * </pre>
     *
     * <p>This data structure provides a guarantee to the issuer about the data which may be
     * returned in the CBOR returned by
     * {@link IdentityCredential.GetEntryResult#getAuthenticatedData()} during a credential
     * presentation.</p>
     *
     * @param accessControlProfiles The collection of access control profiles that are used to
     *                              secure the data. Each
     *                              profile has a name, and each data item can specify any number
     *                              of profile names.
     * @param entryNamespaces       A collection of {@link EntryNamespace} specifying the data to
     *                              be provisioned,
     *                              grouped into namespaces.
     * @return A CBOR data structure, see above.
     * @throws AlreadyPersonalizedException if this credential has already been personalized.
     * @throws IdentityCredentialException  if unable to communicate with secure hardware.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public abstract byte[] personalize(Collection<AccessControlProfile> accessControlProfiles,
            Collection<EntryNamespace> entryNamespaces) throws IdentityCredentialException;
}
