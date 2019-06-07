/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 */
class NavDeepLink {
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z]+[+\\w\\-.]*:");

    private final ArrayList<String> mArguments = new ArrayList<>();
    private final Pattern mPattern;
    private final boolean mExactDeepLink;
    private final Uri mUri;
    private final boolean mIsParameterizedQuery;

    /**
     * NavDestinations should be created via {@link Navigator#createDestination}.
     */
    NavDeepLink(@NonNull String uri) {
        mUri = Uri.parse(uri);
        mIsParameterizedQuery = mUri.getQuery() != null;
        StringBuilder uriRegex = new StringBuilder("^");

        if (!SCHEME_PATTERN.matcher(uri).find()) {
            uriRegex.append("http[s]?://");
        }
        if (mIsParameterizedQuery) {
            Pattern fillInPattern = Pattern.compile("(\\?)");
            Matcher matcher = fillInPattern.matcher(uri);
            if (matcher.find()) {
                uriRegex.append(Pattern.quote(uri.substring(0, matcher.start())));
                uriRegex.append("(.+)?");
            }
            mExactDeepLink = false;
        } else {
            Pattern fillInPattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = fillInPattern.matcher(uri);
            int appendPos = 0;
            // Track whether this is an exact deep link
            boolean exactDeepLink = !uri.contains(".*");
            while (matcher.find()) {
                String argName = matcher.group(1);
                mArguments.add(argName);
                // Use Pattern.quote() to treat the input string as a literal
                uriRegex.append(Pattern.quote(uri.substring(appendPos, matcher.start())));
                uriRegex.append("(.+?)");
                appendPos = matcher.end();
                exactDeepLink = false;
            }
            if (appendPos < uri.length()) {
                // Use Pattern.quote() to treat the input string as a literal
                uriRegex.append(Pattern.quote(uri.substring(appendPos)));
            }
            mExactDeepLink = exactDeepLink;
        }
        // Since we've used Pattern.quote() above, we need to
        // specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        String finalRegex = uriRegex.toString().replace(".*", "\\E.*\\Q");
        mPattern = Pattern.compile(finalRegex);
    }

    boolean matches(@NonNull Uri deepLink) {
        return mPattern.matcher(deepLink.toString()).matches();
    }

    boolean isExactDeepLink() {
        return mExactDeepLink;
    }

    @Nullable
    Bundle getMatchingArguments(@NonNull Uri deepLink,
            @NonNull Map<String, NavArgument> arguments) {
        Matcher matcher = mPattern.matcher(deepLink.toString());
        if (!matcher.matches()) {
            return null;
        }
        Bundle bundle = new Bundle();
        if (mIsParameterizedQuery) {
            // If there are query params that do not exists for this Deep Link we should throw
            if (!mUri.getQueryParameterNames().containsAll(deepLink.getQueryParameterNames())) {
                throw new IllegalArgumentException("Please ensure the given query parameters are a"
                        + " subset of those in NavDeepLink " + this);
            }
            for (String paramName : mUri.getQueryParameterNames()) {
                String value = deepLink.getQueryParameter(paramName);
                NavArgument argument = arguments.get(paramName);
                if (argument != null) {
                    // Missing parameter so see if it has a default value or is Nullable
                    if (value == null || value.replaceAll("[{}]", "").equals(paramName)) {
                        if (argument.getDefaultValue() != null) {
                            value = argument.getDefaultValue().toString();
                        } else if (argument.isNullable()) {
                            value = "@null";
                        }
                    }
                    if (parseArgument(bundle, paramName, value, argument)) {
                        return null;
                    }
                }
            }
        } else {
            int size = mArguments.size();
            for (int index = 0; index < size; index++) {
                String argumentName = mArguments.get(index);
                String value = Uri.decode(matcher.group(index + 1));
                NavArgument argument = arguments.get(argumentName);
                if (parseArgument(bundle, argumentName, value, argument)) {
                    return null;
                }
            }
        }
        return bundle;
    }

    private boolean parseArgument(Bundle bundle, String name, String value, NavArgument argument) {
        if (argument != null) {
            NavType<?> type = argument.getType();
            try {
                type.parseAndPut(bundle, name, value);
            } catch (IllegalArgumentException e) {
                // Failed to parse means this isn't a valid deep link
                // for the given URI - i.e., the URI contains a non-integer
                // value for an integer argument
                return true;
            }
        } else {
            bundle.putString(name, value);
        }
        return false;
    }
}
