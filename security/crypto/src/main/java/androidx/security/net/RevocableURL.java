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

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

/**
 * A URL that provides TLS, certification validity checks.
 */
@TargetApi(Build.VERSION_CODES.N)
public class RevocableURL {

    private static final String TAG = "RevocableURL";

    private URL mUrl;
    private RevocableURLConfig mRevocableURLConfig;
    private String mClientCertAlias;

    public RevocableURL(@NonNull String spec)
            throws MalformedURLException {
        this(spec, null, RevocableURLConfig.getDefault());
    }

    public RevocableURL(@NonNull String spec, @NonNull String clientCertAlias)
            throws MalformedURLException {
        this(spec, clientCertAlias, RevocableURLConfig.getDefault());
    }

    public RevocableURL(@NonNull String spec, @NonNull String clientCertAlias,
                        @NonNull RevocableURLConfig revocableURLConfig)
            throws MalformedURLException {
        this.mUrl = new URL(addProtocol(spec));
        this.mClientCertAlias = clientCertAlias;
        this.mRevocableURLConfig = revocableURLConfig;
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
        int port = this.mUrl.getPort();
        if (port == -1) {
            port = this.mUrl.getDefaultPort();
        }
        return port;
    }

    private String addProtocol(@NonNull String spec) {
        if (!spec.toLowerCase().startsWith("http://")
                && !spec.toLowerCase().startsWith("https://")) {
            return "https://" + spec;
        }
        return spec;
    }


    /**
     * Gets the client cert alias.
     *
     * @return The client cert alias.
     */
    @NonNull
    public String getClientCertAlias() {
        return this.mClientCertAlias;
    }


    /**
     * Opens a connection using default certs with a custom SSLSocketFactory.
     *
     * @return the UrlConnection of the newly opened connection.
     * @throws IOException
     */
    @NonNull
    public URLConnection openConnection() throws GeneralSecurityException, IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) this.mUrl.openConnection();
        urlConnection.setSSLSocketFactory(new RevocableSSLSocketFactory(this));
        return urlConnection;
    }

    /**
     * Checks the hostname against an open SSLSocket connect to the hostname for validity for certs
     * and hostname validity. Only used internally by RevocableSSLSocket.
     * <p>
     * Example Code:
     * SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getAES256GCMConfig();
     * SSLSocket socket = (SSLSocket) sf.createSocket("https://"+hostname, 443);
     * socket.startHandshake();
     * boolean valid = SecurityExt.ensureValid(hostname, socket);
     * </p>
     *
     * @param hostname The host name to check
     * @param socket   The SSLSocket that is open to the URL of the host to check
     * @return true if the SSLSocket has a valid cert and if the hostname is valid, false otherwise.
     */
    void ensureValid(@NonNull String hostname, @NonNull SSLSocket socket) throws
            GeneralSecurityException, SSLException {
        HttpsURLConnection.getDefaultHostnameVerifier()
                .verify(hostname, socket.getSession());
        ensureValid(Arrays.asList(socket.getSession().getPeerCertificates()));
    }

    /**
     * Checks the HttpsUrlConnection certificates for validity.
     * <p>
     * Example Code:
     * RevocableURL mUrl = new RevocableURL("https://" + host);
     * conn = (HttpsURLConnection) mUrl.openConnection();
     * boolean valid = SecurityExt.ensureValid(conn);
     * </p>
     *
     * @param conn The connection to check the certificates of
     * @throws GeneralSecurityException
     */
    public void ensureValid(@NonNull HttpsURLConnection conn) throws GeneralSecurityException,
            SSLException {
        ensureValid(Arrays.asList(conn.getServerCertificates()));
    }


    /**
     * Internal method to check a list of certificates for validity.
     *
     * @param certs list of certs to check
     * @throws GeneralSecurityException
     */
    private void ensureValid(@NonNull List<? extends Certificate> certs)
            throws GeneralSecurityException {
        List<Certificate> leafCerts = new ArrayList<>();
        for (Certificate cert : certs) {
            if (!isRootCA(cert)) {
                leafCerts.add(cert);
            }
        }
        CertPath path = CertificateFactory.getInstance(mRevocableURLConfig.getCertPath())
                .generateCertPath(leafCerts);
        KeyStore ks = KeyStore.getInstance(RevocableURLConfig.ANDROID_CA_STORE);
        try {
            ks.load(null, null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
        CertPathValidator cpv = CertPathValidator.getInstance(mRevocableURLConfig
                .getCertPathValidator());
        PKIXParameters params = new PKIXParameters(ks);
        PKIXRevocationChecker checker = (PKIXRevocationChecker) cpv.getRevocationChecker();
        checker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));
        params.addCertPathChecker(checker);
        cpv.validate(path, params);
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

}
