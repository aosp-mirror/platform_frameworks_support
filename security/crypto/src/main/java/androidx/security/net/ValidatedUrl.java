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


import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A Url implementation that provides backwards compatible TLS, automatic OCSP certification
 * validation, and hostname verification.
 *
 * <pre>
 *     { @code
 *
 *     // If there is a certificate validation issue, an exception will be thrown when the
 *     // connection is attempted.
 *     // An exception will be thrown if the hostname cannot be verified as well.
 *     ValidatedUrl validatedUrl = new ValidatedUrl("http://www.example.com");
 *     HttpsUrlConnection urlConn = validatedUrl.openConnection();
 *     // set up connection...
 *     urlConn.connection;
 *     }
 * </pre>
 *
 * Example network_security_config:
 * On Android 9.0+, a network_security_config.xml must be created and configured to allow plain
 * text connections to the OCSP domains.
 * <pre>
 * {@literal
 * <?xml version="1.0" encoding="utf-8"?>
 * <network-security-config xmlns:tools="http://schemas.android.com/tools">
 *    <domain-config cleartextTrafficPermitted="true">
 *        <!-- Include domains that are relevant for your certificate authority -->
 *        <domain includeSubdomains="true">ocsp.example.com</domain>
 *    </domain-config>
 * </network-security-config>
 * }
 * </pre>
 */
public class ValidatedUrl {

    private static final String X509_CERT_PATH = "X.509";
    private static final String PKIX_CERT_PATH_VALIDATOR = "PKIX";
    private static final String ANDROID_CA_STORE = "AndroidCAStore";

    private final URL mUrl;
    private final String[] mSslCiphers;
    private final boolean mUseOcsp;

    /**
     * Creates a new ValidatedUrl
     *
     * @param url The url
     * @param useOcsp true if OCSP certificate validation checking is enabled
     * @throws MalformedURLException when a bad url is provided
     */
    public ValidatedUrl(@NonNull String url, boolean useOcsp)
            throws MalformedURLException {
        this(url, new String[0], useOcsp);
    }

    /**
     * Creates a new validated url
     *
     * {@link SSLSocket#setEnabledCipherSuites(String[])}
     *
     * @param url The url
     * @param sslCiphers An array of allowed SSL Ciphers.
     * @param useOcsp true if OCSP certificate validation checking is enabled
     * @throws MalformedURLException when a bad url is provided
     */
    public ValidatedUrl(@NonNull String url, @NonNull String[] sslCiphers, boolean useOcsp)
            throws MalformedURLException {
        mUrl = new URL(addProtocol(url));
        mSslCiphers = sslCiphers.clone();
        mUseOcsp = useOcsp;
    }


    /**
     * Gets the hostname used to construct the underlying URL.
     *
     * @return the hostname associated with the Url.
     */
    @NonNull
    public String getHostname() {
        return this.mUrl.getHost();
    }

    /**
     * Gets the port used to construct the underlying URL.
     *
     * @return the port associated with the Url.
     */
    public int getPort() {
        int port = mUrl.getPort();
        if (port == -1) {
            port = mUrl.getDefaultPort();
        }
        return port;
    }

    private String addProtocol(@NonNull String url) {
        if (!url.toLowerCase().startsWith("http://")
                && !url.toLowerCase().startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    /**
     * Opens a connection using default certs with a custom SSLSocketFactory and does certificate
     * revocation checks and hostname verification when the connection is made.
     *
     * @return the UrlConnection of the newly opened connection.
     * @throws IOException when a connection cannot be made
     */
    @NonNull
    public HttpsURLConnection openConnection() throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) mUrl.openConnection();
        SSLSocketFactory defaultSSLSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        urlConnection.setSSLSocketFactory(
                new ValidatedSSLSocketFactory(defaultSSLSocketFactory,
                mSslCiphers, this));
        return urlConnection;
    }


    /**
     * Checks the HttpsUrlConnection certificates for validity. This is called on openConnection
     * automatically and does not need to be run manually. If the HttpsUrlConnection is used for a
     * longer period of time, this method should be called to ensure that the connection is still
     * valid.
     *
     * For Android 7.0, API level 24+
     *
     * @param certs The list of server certs to check
     * @param sslSocket The ssl socket to check the hostname
     */
    void ensureValid(@NonNull List<? extends Certificate> certs, SSLSocket sslSocket) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                boolean hostnameVerified = HttpsURLConnection.getDefaultHostnameVerifier()
                        .verify(getHostname(), sslSocket.getSession());
                if (!hostnameVerified) {
                    throw new SecurityException(getHostname() + " could not be verified.");
                }
                List<Certificate> leafCerts = new ArrayList<>();
                for (Certificate cert : certs) {
                    if (!isRootCA(cert)) {
                        leafCerts.add(cert);
                    }
                }
                if (mUseOcsp) {
                    CertPath path = CertificateFactory.getInstance(X509_CERT_PATH)
                            .generateCertPath(leafCerts);
                    KeyStore ks = KeyStore.getInstance(ANDROID_CA_STORE);
                    ks.load(null, null);
                    CertPathValidator cpv = CertPathValidator.getInstance(PKIX_CERT_PATH_VALIDATOR);
                    PKIXParameters params = new PKIXParameters(ks);
                    PKIXRevocationChecker checker =
                            (PKIXRevocationChecker) cpv.getRevocationChecker();
                    checker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));
                    params.addCertPathChecker(checker);
                    cpv.validate(path, params);
                }
            } catch (GeneralSecurityException | IOException ex) {
                throw new SecurityException("Could not verify server certificates: "
                        + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Internal method to check if a cert is a CA.
     *
     * @param cert The cert to check
     * @return true if the cert is a RootCA, false otherwise
     */
    private boolean isRootCA(@NonNull Certificate cert) {
        boolean rootCA = false;
        if (cert instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate) cert;
            if (x509Certificate.getSubjectDN().getName().equals(
                    x509Certificate.getIssuerDN().getName())) {
                rootCA = true;
            }
        }
        return rootCA;
    }

    @Override
    public int hashCode() {
        return mUrl.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return mUrl.equals(obj);
    }

    @NonNull
    @Override
    public String toString() {
        return mUrl.toString();
    }
}
