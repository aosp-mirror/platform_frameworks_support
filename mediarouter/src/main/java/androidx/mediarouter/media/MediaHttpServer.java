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

package androidx.mediarouter.media;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.RestrictTo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class MediaHttpServer {
    private static final String TAG = "MediaHttpServer";

    private static final String MIMETYPE_TEXT_PLAIN = "text/plain";

    private NanoHTTPD mServer = new NanoHTTPD(0) {
        @Override
        public Response serve(IHTTPSession session) {
            if (session.getMethod() == Method.GET) {
                String path = session.getUri();
                return serveGet(path);
            }
            return notFound();
        }

        private Response serveGet(String path) {
            if (!path.startsWith("/")) {
                return notFound();
            }
            String id = path.substring(1);
            Uri uri = mUriMap.get(id);
            if (uri == null) {
                return notFound();
            }
            ContentResolver resolver = mContext.getContentResolver();
            InputStream stream;
            try {
                stream = resolver.openInputStream(uri);
            } catch (FileNotFoundException ex) {
                Log.e(TAG, "can not open uri " + uri, ex);
                return notFound();
            }
            String type = resolver.getType(uri);
            return newChunkedResponse(Response.Status.OK, type, stream);
        }

        private Response notFound() {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIMETYPE_TEXT_PLAIN, null);
        }
    };

    final Context mContext;
    final HashMap<String, Uri> mUriMap = new HashMap<>();
    private String mUrlPrefix;

    public MediaHttpServer(Context context) {
        mContext = context.getApplicationContext();
    }

    /** start */
    public void start() throws IOException {
        mServer.start();

        String host = getHost();
        mUrlPrefix = "http://" + host + ":" + mServer.getListeningPort();
        Log.d(TAG, "listening to " + mUrlPrefix);
    }

    /** stop */
    public void stop() {
        mServer.stop();

        mUrlPrefix = null;
    }

    /** addContent */
    public void addContent(String id, Uri uri) {
        mUriMap.put(id, uri);
    }

    /** getUriForId */
    public Uri getUriForId(String id) {
        if (mUrlPrefix == null) {
            return null;
        }
        return Uri.parse(mUrlPrefix + "/" + id);
    }

    private String getHost() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            Enumeration<InetAddress> inets = nic.getInetAddresses();
            while (inets.hasMoreElements()) {
                InetAddress inet = inets.nextElement();
                if (inet.isLoopbackAddress()) {
                    continue;
                }
                if (inet instanceof Inet4Address) {
                    return inet.getHostAddress();
                }
            }
        }
        throw new SocketException();
    }
}
