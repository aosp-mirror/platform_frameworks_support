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

import static androidx.security.net.RevocableURLConfig.SSL_TLS;

import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * A custom implementation of SSLSocketFactory which handles the creation of custom SSLSockets
 * that handle extra functionality and do validity checking.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class RevocableSSLSocketFactory extends SSLSocketFactory {

    private static final String TAG = "RevocableSSLSocketFactory";

    private SSLSocketFactory mSslSocketFactory;
    private RevocableURL mRevocableURL;
    private Socket mSocket;
    private RevocableURLConfig mRevocableURLConfig;

    RevocableSSLSocketFactory(RevocableURL revocableURL, SSLSocketFactory sslSocketFactory,
                              RevocableURLConfig revocableURLConfig) throws IOException {
        this.mRevocableURL = revocableURL;
        this.mSslSocketFactory = sslSocketFactory;
        this.mRevocableURLConfig = revocableURLConfig;
        this.mSocket = new RevocableSSLSocket(revocableURL,
                mSslSocketFactory.createSocket(mRevocableURL.getHostname(),
                        mRevocableURL.getPort()), mRevocableURLConfig);
    }

    RevocableSSLSocketFactory(RevocableURL revocableURL, SSLSocketFactory sslSocketFactory)
            throws IOException {
        this(revocableURL, sslSocketFactory, RevocableURLConfig.getDefault());
    }

    RevocableSSLSocketFactory(RevocableURL revocableURL) throws IOException {
        this(revocableURL, (SSLSocketFactory) SSLSocketFactory.getDefault(),
                RevocableURLConfig.getDefault());
    }

    RevocableSSLSocketFactory(RevocableURL revocableURL,
                              Map<String, InputStream> trustedCAs,
                              RevocableURLConfig revocableURLConfig) throws IOException {
        this(revocableURL, createUserTrustSSLSocketFactory(trustedCAs,
                revocableURLConfig, revocableURL),
                revocableURLConfig);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mSslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mSslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        if (mSocket == null) {
            mSocket = new RevocableSSLSocket(
                    mRevocableURL, mSslSocketFactory.createSocket(s, host, port, autoClose),
                    mRevocableURLConfig);
        }
        return mSocket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        if (mSocket == null) {
            mSocket = new RevocableSSLSocket(mRevocableURL,
                    mSslSocketFactory.createSocket(host, port),
                    mRevocableURLConfig);
        }
        return mSocket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        if (mSocket == null) {
            mSocket = new RevocableSSLSocket(
                    mRevocableURL, mSslSocketFactory.createSocket(host, port, localHost, localPort),
                    mRevocableURLConfig);
        }
        return mSocket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        if (mSocket == null) {
            mSocket = new RevocableSSLSocket(mRevocableURL, mSslSocketFactory
                    .createSocket(host, port),
                    mRevocableURLConfig);
        }
        return mSocket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        if (mSocket == null) {
            mSocket = new RevocableSSLSocket(
                    mRevocableURL, mSslSocketFactory.createSocket(address, port, localAddress,
                    localPort),
                    mRevocableURLConfig);
        }
        return mSocket;
    }

    // TODO Evaluate the need for all of these options
    private static SSLSocketFactory createUserTrustSSLSocketFactory(Map<String, InputStream>
            trustAnchors, RevocableURLConfig revocableURLConfig, RevocableURL revocableURL) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            KeyStore clientStore = KeyStore.getInstance(RevocableURLConfig.KEYSTORE_TYPE);
            clientStore.load(null, null);

            KeyStore trustStore = null;
            switch (revocableURLConfig.getTrustAnchorOptions()) {
                case USER_ONLY:
                case USER_SYSTEM:
                case LIMITED_SYSTEM:
                    trustStore = KeyStore.getInstance(RevocableURLConfig.KEYSTORE_TYPE);
                    trustStore.load(null, null);
                    break;
            }

            switch (revocableURLConfig.getTrustAnchorOptions()) {
                case USER_SYSTEM:
                    KeyStore caStore = KeyStore.getInstance(RevocableURLConfig.ANDROID_CA_STORE);
                    caStore.load(null, null);
                    Enumeration<String> caAliases = caStore.aliases();
                    while (caAliases.hasMoreElements()) {
                        String alias = caAliases.nextElement();
                        trustStore.setCertificateEntry(alias, caStore.getCertificate(alias));
                    }
                    break;
                case USER_ONLY:
                case LIMITED_SYSTEM:
                    for (Map.Entry<String, InputStream> ca : trustAnchors.entrySet()) {
                        CertificateFactory cf = CertificateFactory
                                .getInstance(revocableURLConfig.getCertPath());
                        Certificate userCert = cf.generateCertificate(ca.getValue());
                        trustStore.setCertificateEntry(ca.getKey(), userCert);
                    }
                    break;
            }

            tmf.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance(SSL_TLS);

            KeyManager[] keyManagersArray = new KeyManager[1];
            keyManagersArray[0] = RevocableX509KeyManager.getInstance(
                    revocableURL.getClientCertAlias(), revocableURLConfig);
            sslContext.init(keyManagersArray, tmf.getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Issue creating User SSLSocketFactory.");
        } catch (IOException ex) {
            throw new SecurityException("Issue creating User SSLSocketFactory.");
        }
    }

}
