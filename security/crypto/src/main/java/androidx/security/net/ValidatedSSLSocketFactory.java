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

import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

/**
 * A custom implementation of SSLSocketFactory which handles the creation of custom SSLSockets
 * that handle extra functionality and do validity checking.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ValidatedSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory mSslSocketFactory;
    private final String[] mSslCiphers;
    private final ValidatedUrl mValidatedUrl;

    ValidatedSSLSocketFactory(SSLSocketFactory sslSocketFactory,
            String[] sslCiphers, ValidatedUrl validatedUrl) {
        mSslSocketFactory = sslSocketFactory;
        mSslCiphers = sslCiphers;
        mValidatedUrl = validatedUrl;
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
        return ValidatedSSLSocket.create(mSslSocketFactory.createSocket(s, host, port, autoClose),
                mSslCiphers, mValidatedUrl);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return ValidatedSSLSocket.create(mSslSocketFactory.createSocket(host, port),
                mSslCiphers, mValidatedUrl);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        return ValidatedSSLSocket.create(
                mSslSocketFactory.createSocket(host, port, localHost, localPort),
                mSslCiphers, mValidatedUrl);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return ValidatedSSLSocket.create(
                mSslSocketFactory.createSocket(host, port),
                mSslCiphers, mValidatedUrl);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        return ValidatedSSLSocket.create(
                mSslSocketFactory.createSocket(address, port, localAddress, localPort),
                mSslCiphers, mValidatedUrl);
    }

}
