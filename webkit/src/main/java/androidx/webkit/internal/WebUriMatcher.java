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

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * Utility class to aid in matching URIs. Doesn't support wildcards like "#", "*" or "**".
 *
 * @param <T> handler object returned when a match occurs.
 */
public class WebUriMatcher<T> {
    @NonNull private ArrayList<MatcherValues> mMatchers;

    public WebUriMatcher() {
        mMatchers = new ArrayList<>();
    }

    private class MatcherValues {
        @NonNull final String mScheme;
        @NonNull final String mAuthority;
        @NonNull final String mPath;
        @Nullable final T mHandler;

        MatcherValues(@NonNull final String scheme, @NonNull final String authority,
                    @Nullable final String path, @NonNull final T handler) {
            mScheme = scheme;
            mAuthority = authority;
            mPath = path;
            mHandler = handler;
        }
    }

    /**
     * Add a URI to match, and the handler to return when this URI is
     * matched. Matches URIs on the form: "scheme://authority/path**"
     *
     * @param scheme the scheme (http/https) to match
     * @param authority the authority to match (For example example.com)
     * @param path the prefix path to match.
     * @param handler the handler that is returned when a URI is matched
     * against the given components.
     */
    @NonNull
    public void addUri(@NonNull final String scheme, @NonNull final String authority,
                        @Nullable final String path, @NonNull final T handler) {
        mMatchers.add(new MatcherValues(scheme, authority, path, handler));
    }

    /**
     * Try to matches the URI against registered matchers. Match happens when:
     *      - Scheme exact matches the given URI's scheme.
     *      - Authority exact matches the given URI's scheme.
     *      - Path is a prefix of the given URI's scheme.
     * @param uri The url whose path we will match against.
     *
     * @return  The handler for the matched node (added using addUri),
     * or {@code null} if there is no matched node.
     */
    @Nullable
    public T match(@NonNull Uri uri) {
        for (MatcherValues matcher : mMatchers) {
            if (uri.getScheme().equals(matcher.mScheme)
                    && uri.getAuthority().equals(matcher.mAuthority)
                    && uri.getPath().startsWith(matcher.mPath)) {
                return matcher.mHandler;
            }
        }

        return null;
    }
}
