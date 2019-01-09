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

package androidx.webkit.internal;

import android.content.UriMatcher;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
  * Utility class to aid in matching URIs, wraps {@link android.content.UriMatcher} to support
  * matching schemes (http:// or https://) and return a handler object instead of int
  * code.
  *
  * @param <T> object returned when a match occurs.
  */
public class HttpUriMatcher<T> {
    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";

    @Nullable private final UriMatcher mHttpUriMatcher;
    @Nullable private final UriMatcher mHttpsUriMatcher;
    @Nullable private final ArrayList<T> mHttpCodeToHandler;
    @Nullable private final ArrayList<T> mHttpsCodeToHandler;

    public HttpUriMatcher() {
        mHttpUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mHttpsUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mHttpCodeToHandler = new ArrayList<>();
        mHttpsCodeToHandler = new ArrayList<>();
    }

    /**
      * Add a URI to match, and the handler to return when this URI is
      * matched. URI nodes may be exact match string, the token "*"
      * that matches any text, or the token "#" that matches only
      * numbers.
      * <p>
      * Starting from API level {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2},
      * this method will accept a leading slash in the path.
      *
      * @param scheme the scheme (http/https) to match
      * @param authority the authority to match
      * @param path the path to match. * may be used as a wild card for
      * any text, and # may be used as a wild card for numbers.
      * @param handler the handler that is returned when a URI is matched
      * against the given components.
      */
    public void addURI(@NonNull String scheme, @NonNull String authority, @Nullable String path,
                       @NonNull T handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler can't be null");
        }

        if (scheme.equals(HTTP_SCHEME)) {
            mHttpUriMatcher.addURI(authority, path, mHttpCodeToHandler.size());
            mHttpCodeToHandler.add(handler);
        } else if (scheme.equals(HTTPS_SCHEME)) {
            mHttpsUriMatcher.addURI(authority, path, mHttpsCodeToHandler.size());
            mHttpsCodeToHandler.add(handler);
        } else {
            throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        }
    }

    /**
      * Try to match against the path in a url.
      *
      * @param uri The url whose path we will match against.
      *
      * @return  The handler for the matched node (added using addURI),
      * or {@code null} if there is no matched node.
      */
    @Nullable
    public T match(@NonNull Uri uri) {
        if (HTTP_SCHEME.equals(uri.getScheme())) {
            int code = mHttpUriMatcher.match(uri);
            if (code == UriMatcher.NO_MATCH) {
                return null;
            }
            if (code >= mHttpCodeToHandler.size()) {
                throw new AssertionError("Match code cannot be >= handlers list size");
            }
            return mHttpCodeToHandler.get(code);
        } else if (HTTPS_SCHEME.equals(uri.getScheme())) {
            int code = mHttpsUriMatcher.match(uri);
            if (code == UriMatcher.NO_MATCH) {
                return null;
            }
            if (code >= mHttpsCodeToHandler.size()) {
                throw new AssertionError("Match code cannot be >= handlers list size");
            }
            return mHttpsCodeToHandler.get(code);
        } else {
            return null;
        }
    }
}
