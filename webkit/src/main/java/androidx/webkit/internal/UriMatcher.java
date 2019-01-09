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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
  * Utility class to aid in matching URIs, forked from {@link android.content.UriMatcher} to:
  * - Support matching schemes (like http:// or https://).
  * - Return a handler object instead of int code.
  * - Add support to the wild card "**" for nested paths.
  *
  * @param <T> object returned when a match occurs.
  */
public class UriMatcher<T> {
    static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");

    private static final int NO_MATCH = -1;
    private static final int EXACT = 0;
    private static final int TEXT = 1;
    private static final int REST = 2;

    @IntDef({
        NO_MATCH,
        EXACT,
        TEXT,
        REST,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatchType {}

    @Nullable private T mHandler;
    private int mMatchType;
    @Nullable private String mText;
    @NonNull private ArrayList<UriMatcher<T>> mChildren;

    /**
      * Creates the root node of the URI tree.
      *
      * @param handler the handler to match for the root URI
      */
    public UriMatcher(@Nullable T handler) {
        mHandler = handler;
        mMatchType = NO_MATCH;
        mChildren = new ArrayList<UriMatcher<T>>();
        mText = null;
    }

    private UriMatcher() {
        this(null);
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

        String[] tokens = null;
        if (path != null) {
            String newPath = path;
            // Strip leading slash if present.
            if (path.length() > 0 && path.charAt(0) == '/') {
                newPath = path.substring(1);
            }
            tokens = PATH_SPLIT_PATTERN.split(newPath);
        }

        int numTokens = tokens != null ? tokens.length : 0;
        UriMatcher<T> node = this;
        for (int i = -2; i < numTokens; i++) {
            String token;
            if (i == -2) {
                token = scheme;
            } else if (i == -1) {
                token = authority;
            } else {
                token = tokens[i];
            }
            ArrayList<UriMatcher<T>> children = node.mChildren;
            int numChildren = children.size();
            UriMatcher<T> child;
            int j;
            for (j = 0; j < numChildren; j++) {
                child = children.get(j);
                if (token.equals(child.mText)) {
                    node = child;
                    break;
                }
            }
            if (j == numChildren) {
                // Child not found, create it
                child = new UriMatcher<>(null);
                if (token.equals("**")) {
                    child.mMatchType = REST;
                } else if (token.equals("*")) {
                    child.mMatchType = TEXT;
                } else {
                    child.mMatchType = EXACT;
                }
                child.mText = token;
                node.mChildren.add(child);
                node = child;
            }
        }
        node.mHandler = handler;
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
        final List<String> pathSegments = uri.getPathSegments();
        final int li = pathSegments.size();

        UriMatcher<T> node = this;

        if (li == 0 && uri.getAuthority() == null) {
            return this.mHandler;
        }

        for (int i = -2; i < li; i++) {
            String u;
            if (i == -2) {
                u = uri.getScheme();
            } else if (i == -1) {
                u = uri.getAuthority();
            } else {
                u = pathSegments.get(i);
            }
            ArrayList<UriMatcher<T>> list = node.mChildren;
            if (list == null) {
                break;
            }
            node = null;
            int lj = list.size();
            for (int j = 0; j < lj; j++) {
                UriMatcher<T> n = list.get(j);
                matchType_switch:
                switch (n.mMatchType) {
                    case EXACT:
                        if (n.mText.equals(u)) {
                            node = n;
                        }
                        break;
                    case TEXT:
                        node = n;
                        break;
                    case REST:
                        return n.mHandler;
                }
                if (node != null) {
                    break;
                }
            }
            if (node == null) {
                return null;
            }
        }

        return node.mHandler;
    }
}
