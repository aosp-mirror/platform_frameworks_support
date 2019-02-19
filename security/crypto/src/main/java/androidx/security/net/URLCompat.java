/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.security.net;


import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import androidx.security.SecureConfig;
import androidx.security.config.TldConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

@TargetApi(Build.VERSION_CODES.N)
public class URLCompat {

    private static final String TAG = "URLCompat";

    private URL url;
    private SecureConfig secureConfig;
    private String clientCertAlias;

    public URLCompat(String spec, String clientCertAlias) throws MalformedURLException {
        this(spec, clientCertAlias, SecureConfig.getStrongConfig());
    }

    public URLCompat(String spec, String clientCertAlias, SecureConfig secureConfig) throws MalformedURLException {
        this.url = new URL(addProtocol(spec));
        this.clientCertAlias = clientCertAlias;
        this.secureConfig = secureConfig;
    }

    public String getHostname() {
        return this.url.getHost();
    }

    private String addProtocol(String spec) {
        if (!spec.toLowerCase().startsWith("http://") && !spec.toLowerCase().startsWith("https://")) {
            return "https://" + spec;
        }
        return spec;
    }

    public String getClientCertAlias() {
        return this.clientCertAlias;
    }

    public URLConnection openConnection() throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) this.url.openConnection();
        urlConnection.setSSLSocketFactory(new ValidatableSSLSocketFactory(this));
        return urlConnection;
    }

    public URLConnection openUserTrustedCertConnection(Map<String, InputStream> trustedCAs) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) this.url.openConnection();
        urlConnection.setSSLSocketFactory(new ValidatableSSLSocketFactory(this, trustedCAs, secureConfig));
        return urlConnection;
    }

    /**
     * Checks the hostname against an open SSLSocket connect to the hostname for validity for certs
     * and hostname validity.
     * <p>
     * Example Code:
     * SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
     * SSLSocket socket = (SSLSocket) sf.createSocket("https://"+hostname, 443);
     * socket.startHandshake();
     * boolean valid = SecurityExt.isValid(hostname, socket);
     * </p>
     *
     * @param hostname The host name to check
     * @param socket   The SSLSocket that is open to the URL of the host to check
     * @return true if the SSLSocket has a valid cert and if the hostname is valid, false otherwise.
     */
    public boolean isValid(String hostname, SSLSocket socket) {
        try {
            Log.i(TAG, "Hostname verifier: " + HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, socket.getSession()));
            Log.i(TAG, "isValid Peer Certs: " + isValid(Arrays.asList(socket.getSession().getPeerCertificates())));
            return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, socket.getSession())
                    && isValid(Arrays.asList(socket.getSession().getPeerCertificates()))
                    && validTldWildcards(Arrays.asList(socket.getSession().getPeerCertificates()));
        } catch (SSLPeerUnverifiedException e) {
            Log.i(TAG, "Valid Check failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks the HttpsUrlConnection certificates for validity.
     * <p>
     * Example Code:
     * URL url = new URL("https://" + urlText);
     * conn = (HttpsURLConnection) url.openConnection();
     * boolean valid = SecurityExt.isValid(conn);
     * </p>
     *
     * @param conn The connection to check the certificates of
     * @return true if the certificates for the HttpsUrlConnection are valid, false otherwise
     */
    public boolean isValid(HttpsURLConnection conn) {
        try {
            return isValid(Arrays.asList(conn.getServerCertificates())) &&
                    validTldWildcards(Arrays.asList(conn.getServerCertificates()));
        } catch (SSLPeerUnverifiedException e) {
            Log.i(TAG, "Valid Check failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Internal method to check a list of certificates for validity.
     *
     * @param certs list of certs to check
     * @return true if the certs are valid, false otherwise
     */
    private boolean isValid(List<? extends Certificate> certs) {
        try {
            List<Certificate> leafCerts = new ArrayList<>();
            for (Certificate cert : certs) {
                if (!isRootCA(cert)) {
                    leafCerts.add(cert);
                }
            }
            CertPath path = CertificateFactory.getInstance(secureConfig.getCertPath())
                    .generateCertPath(leafCerts);
            KeyStore ks = KeyStore.getInstance(secureConfig.getAndroidCAStore());
            try {
                ks.load(null, null);
            } catch (IOException e) {
                e.printStackTrace();
                throw new AssertionError(e);
            }
            CertPathValidator cpv = CertPathValidator.getInstance(secureConfig.getCertPathValidator());
            PKIXParameters params = new PKIXParameters(ks);
            PKIXRevocationChecker checker = (PKIXRevocationChecker) cpv.getRevocationChecker();
            checker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));
            params.addCertPathChecker(checker);
            cpv.validate(path, params);
            return true;
        } catch (CertPathValidatorException e) {
            // If this message prints out "Unable to determine revocation status due to network error"
            // Make sure your network security config allows for clear text access of the relevant
            // OCSP url.
            e.printStackTrace();
            return false;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal method to check if a cert is a CA.
     *
     * @param cert The cert to check
     * @return true if the cert is a RootCA, false otherwise
     */
    private boolean isRootCA(Certificate cert) {
        boolean rootCA = false;
        if (cert instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate) cert;
            if (x509Certificate.getSubjectDN().getName().equals(x509Certificate.getIssuerDN().getName())) {
                rootCA = true;
            }
        }
        return rootCA;
    }


    private boolean validTldWildcards(List<? extends Certificate> certs) {
        // For a more complete list https://publicsuffix.org/list/public_suffix_list.dat
        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate) {
                X509Certificate x509Cert = (X509Certificate) cert;
                try {
                    Collection<List<?>> subAltNames = x509Cert.getSubjectAlternativeNames();
                    if (subAltNames != null) {
                        List<String> dnsNames = new ArrayList<>();
                        for (List<?> tldList : subAltNames) {
                            if (tldList.size() >= 2) {
                                dnsNames.add(tldList.get(1).toString().toUpperCase());
                            }
                        }
                        // Populate DNS NAMES, make sure they are lower case
                        for (String dnsName : dnsNames) {
                            if (TldConstants.VALID_TLDS.contains(dnsName)) {
                                Log.i(TAG, "FAILED WILDCARD TldConstants CHECK: " + dnsName);
                                return false;
                            }
                        }
                    }
                } catch (CertificateParsingException ex) {
                    Log.i(TAG, "Cert Parsing Issue: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

}
