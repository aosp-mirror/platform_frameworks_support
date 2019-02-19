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

import androidx.security.SecureConfig;

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

import static androidx.security.SecureConfig.SSL_TLS;

class ValidatableSSLSocketFactory extends SSLSocketFactory {

    private static String TAG = "ValidatableSSLSocketFactory";

    private SSLSocketFactory sslSocketFactory;
    private URLCompat secureURL;
    private Socket socket;
    private SecureConfig secureConfig;

    public ValidatableSSLSocketFactory(URLCompat secureURL, SSLSocketFactory sslSocketFactory, SecureConfig secureConfig) {
        this.secureURL = secureURL;
        this.sslSocketFactory = sslSocketFactory;
        this.secureConfig = secureConfig;
    }

    public ValidatableSSLSocketFactory(URLCompat secureURL, SSLSocketFactory sslSocketFactory) {
        this(secureURL, sslSocketFactory, SecureConfig.getStrongConfig());
    }

    public ValidatableSSLSocketFactory(URLCompat secureURL) {
        this(secureURL, (SSLSocketFactory) SSLSocketFactory.getDefault(), SecureConfig.getStrongConfig());
    }

    public ValidatableSSLSocketFactory(URLCompat secureURL, Map<String, InputStream> trustedCAs, SecureConfig secureConfig) {
        this(secureURL, createUserTrustSSLSocketFactory(trustedCAs, secureConfig, secureURL), secureConfig);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        if (socket == null) {
            socket = new ValidatableSSLSocket(secureURL, sslSocketFactory.createSocket(s, host, port, autoClose), secureConfig);
        }
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        if (socket == null) {
            socket = new ValidatableSSLSocket(secureURL, sslSocketFactory.createSocket(host, port), secureConfig);
        }
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        if (socket == null) {
            socket = new ValidatableSSLSocket(secureURL, sslSocketFactory.createSocket(host, port, localHost, localPort), secureConfig);
        }
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        if (socket == null) {
            socket = new ValidatableSSLSocket(secureURL, sslSocketFactory.createSocket(host, port), secureConfig);
        }
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        if (socket == null) {
            socket = new ValidatableSSLSocket(secureURL, sslSocketFactory.createSocket(address, port, localAddress, localPort), secureConfig);
        }
        return socket;
    }

    // TODO Evaluate the need for all of these options
    private static SSLSocketFactory createUserTrustSSLSocketFactory(Map<String, InputStream> trustAnchors, SecureConfig secureConfig, URLCompat secureURL) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            KeyStore clientStore = KeyStore.getInstance(secureConfig.getKeystoreType());
            clientStore.load(null, null);

            KeyStore trustStore = null;
            switch (secureConfig.getTrustAnchorOptions()) {
                case USER_ONLY:
                case USER_SYSTEM:
                case LIMITED_SYSTEM:
                    trustStore = KeyStore.getInstance(secureConfig.getKeystoreType());
                    trustStore.load(null, null);
                    break;
            }

            switch (secureConfig.getTrustAnchorOptions()) {
                case USER_SYSTEM:
                    KeyStore caStore = KeyStore.getInstance(secureConfig.getAndroidCAStore());
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
                        CertificateFactory cf = CertificateFactory.getInstance(secureConfig.getCertPath());
                        Certificate userCert = cf.generateCertificate(ca.getValue());
                        trustStore.setCertificateEntry(ca.getKey(), userCert);
                    }
                    break;
            }

            tmf.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance(SSL_TLS);

            KeyManager[] keyManagersArray = new KeyManager[1];
            keyManagersArray[0] = KeyManagerCompat.getDefault(secureURL.getClientCertAlias(), secureConfig);
            sslContext.init(keyManagersArray, tmf.getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Issue creating User SSLSocketFactory.");
        } catch (IOException ex) {
            throw new SecurityException("Issue creating User SSLSocketFactory.");
        }
    }

}
