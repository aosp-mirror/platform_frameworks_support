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

package androidx.webkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.StringDef;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.internal.WebViewProviderFactory;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * API class for proxy override methods
 */
public class WebViewProxyCompat {
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    @StringDef({HTTP, HTTPS})
    public @interface ProxyScheme {}

    /**
     * Sets a proxy which will be used by all WebViews in the app. URLs that match rules in the
     * exclusion list will be sent to DIRECT instead. Network connections are not guaranteed to
     * immediately use the new proxy setting; wait for the listener before loading a page.
     *
     * @param proxyRules Proxy rules to be applied
     * @param listener Optional listener called when the proxy setting change has been applied
     * @param executor Optional executor for the listener to be executed in
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setProxyOverride(@NonNull ProxyRules proxyRules, @Nullable Runnable listener,
            @Nullable Executor executor) {
        setProxyOverrideInternal(proxyRules.buildProxyRules(), proxyRules.buildExclusionList(),
                listener, executor);
    }

    private static void setProxyOverrideInternal(@NonNull String[][] rules, String[] exclusionList,
            @Nullable final Runnable listener, final @Nullable Executor executor) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.PROXY_OVERRIDE);
        if (webviewFeature.isSupportedByWebView()) {
            getFactory().getStatics().setProxyOverride(rules, exclusionList, listener, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Clears the proxy settings. Network connections are not guaranteed to immediately use the
     * new proxy setting; wait for the listener before loading a page.
     *
     * @param listener Optional listener called when the proxy setting change has been applied
     * @param executor Optional executor for the listener to be executed in
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void clearProxyOverride(@Nullable Runnable listener,
            @Nullable Executor executor) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.PROXY_OVERRIDE);
        if (webviewFeature.isSupportedByWebView()) {
            getFactory().getStatics().clearProxyOverride(listener, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Rules for proxy override API
     */
    public static class ProxyRules {
        private ArrayList<String[]> mProxyRules;
        private ArrayList<String> mExclusionList;

        public ProxyRules() {
            mProxyRules = new ArrayList<>();
            mExclusionList = new ArrayList<>();
        }

        /*package*/ String[][] buildProxyRules() {
            String[][] rules = new String[mProxyRules.size()][];
            for (int i = 0; i < mProxyRules.size(); i++) {
                rules[i] = mProxyRules.get(i);
            }
            return rules;
        }

        /*package*/ String[] buildExclusionList() {
            String[] exclusion = new String[mExclusionList.size()];
            for (int i = 0; i < mExclusionList.size(); i++) {
                exclusion[i] = mExclusionList.get(i);
            }
            return exclusion;
        }

        /**
         * Adds a proxy to be used for all URIs.
         * <p>Proxy is either “direct://” or a string in the format [scheme://]host[:port]. Scheme
         * is optional and defaults to HTTP; host is one of an IPv6 literal with brackets,
         * an IPv4 literal or one or more labels separated by a period; port number is optional
         * and defaults to 80 if HTTP and 443 if HTTPS.
         * <p>The correct syntax for hosts is defined by
         * <a  href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986</a>
         *
         * @param proxy Proxy to be added
         */
        public ProxyRules addRule(@NonNull String proxy) {
            return addRuleInternal(null, proxy);
        }

        /**
         * This does everything that {@link #addRule(String)} does, but filters URLs by scheme.
         *
         * @param scheme Scheme for which URLs should use this rule. One of {@link #HTTP} or
         *               {@link #HTTPS}.
         * @param proxy Proxy to be added
         */
        public ProxyRules addRule(@NonNull @ProxyScheme String scheme, @NonNull String proxy) {
            return addRuleInternal(scheme, proxy);
        }

        private ProxyRules addRuleInternal(String scheme, String proxy) {
            if (proxy == null) {
                throw new NullPointerException("Proxy string cannot be null");
            }
            String[] rule = {scheme, proxy};
            mProxyRules.add(rule);
            return this;
        }

        /**
         * Adds a new rule that describes URLs that should skip proxy override settings and be sent
         * to DIRECT instead. Wildcards are accepted. For instance, the rule “*.google.com” would
         * mean that requests to the following URLs would be sent to DIRECT:
         * <p>http://photos.google.com/, https://www.google.com/.
         *
         * @param rule Rule to be added to the exclusion list
         */
        public ProxyRules addExclusionRule(@NonNull String rule) {
            if (rule == null) {
                throw new NullPointerException("Rules for the exclusion list cannot be null");
            }
            mExclusionList.add(rule);
            return this;
        }
    }

    private static WebViewProviderFactory getFactory() {
        return WebViewGlueCommunicator.getFactory();
    }
}
