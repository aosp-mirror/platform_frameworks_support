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
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.internal.WebViewProviderFactory;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Manages proxy override for WebViews. This class provides functionality for the application to
 * set or clear a different set of proxy rules other than the system default.
 * <p>
 * Example usage:
 * <pre class="prettyprint">
 * ProxyController proxyController = ProxyController.getInstance();
 * ProxyRules proxyRules = new ProxyRules.ProxyRulesGlobalBuilder().addProxyUrl("myproxy.com")
 *                                                                 .addBypassRule("www.excluded.*")
 *                                                                 .build();
 * Executor executor = ...
 * Runnable listener = ...
 * proxyController.setProxyOverride(proxyRules, executor, listener);
 * ...
 * proxyController.clearProxyOverride(executor, listener);
 * </pre>
 */
public class ProxyController {
    private static ProxyController sInstance;
    public static final String DIRECT = "direct://";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String FTP = "ftp";
    public static final String WS = "ws";
    public static final String WSS = "wss";
    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @StringDef({HTTP, HTTPS, FTP, WS, WSS})
    public @interface ProxyScheme {}

    private ProxyController() {

    }

    /**
     * Returns the {@link ProxyController} instance.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#PROXY_OVERRIDE}.
     */
    public static ProxyController getInstance() {
        if (sInstance == null) {
            sInstance = new ProxyController();
        }
        return sInstance;
    }

    /**
     * Sets a proxy which will be used by all WebViews in the app. URLs that match rules in the
     * bypass list will make a direct connection instead. Network connections are not guaranteed to
     * immediately use the new proxy setting; wait for the listener before loading a page.
     *
     * @param proxyRules Proxy rules to be applied
     * @param listener Optional listener called when the proxy setting change has been applied
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setProxyOverride(@NonNull ProxyRules proxyRules, @Nullable Runnable listener) {
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        setProxyOverride(proxyRules, executor, listener);
    }

    /**
     * Sets a proxy which will be used by all WebViews in the app. URLs that match rules in the
     * bypass list will make a direct connection instead. Network connections are not guaranteed to
     * immediately use the new proxy setting; wait for the listener before loading a page.
     *
     * @param proxyRules Proxy rules to be applied
     * @param executor Executor for the listener to be executed in
     * @param listener Optional listener called when the proxy setting change has been applied
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setProxyOverride(@NonNull ProxyRules proxyRules, @NonNull Executor executor,
            @Nullable Runnable listener) {
        if (executor == null) {
            throw new NullPointerException("Executor must not be null");
        }
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.PROXY_OVERRIDE);
        if (webViewFeature.isSupportedByWebView()) {
            getFactory().getStatics().setProxyOverride(proxyRules.proxyRules(),
                    proxyRules.bypassRules(), listener, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Clears the proxy settings. Network connections are not guaranteed to immediately use the
     * new proxy setting; wait for the listener before loading a page.
     *
     * @param listener Optional listener called when the proxy setting change has been applied
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void clearProxyOverride(@Nullable Runnable listener) {
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        clearProxyOverride(executor, listener);
    }


    /**
     * Clears the proxy settings. Network connections are not guaranteed to immediately use the
     * new proxy setting; wait for the listener before loading a page.
     *
     * @param executor Executor for the listener to be executed in
     * @param listener Optional listener called when the proxy setting change has been applied
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void clearProxyOverride(@NonNull Executor executor, @Nullable Runnable listener) {
        if (executor == null) {
            throw new NullPointerException("Executor must not be null");
        }
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.PROXY_OVERRIDE);
        if (webViewFeature.isSupportedByWebView()) {
            getFactory().getStatics().clearProxyOverride(listener, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Rules for {@link ProxyController#setProxyOverride(ProxyRules, Executor, Runnable)}.
     * <p>
     * Proxy rules should be added using {@code addProxyRule} methods. Multiple rules can be used as
     * fallback if a proxy fails to respond. Bypass rules can be set for URLs that should not use
     * these settings.
     * <p>
     * For instance, the following code means that WebView would first try to use proxy1.com for all
     * URLs, if that fails, proxy2.com, and if that fails, it would make a direct connection.
     * <pre class="prettyprint">
     * ProxyRules proxyRules = new ProxyRules.ProxyRulesGlobalBuilder().addProxyRule("proxy1.com")
     *                                                                 .addProxyRule("proxy2.com")
     *                                                                 .addProxyRule("direct://")
     *                                                                 .build();
     * </pre>
     */
    public static class ProxyRules {
        private String[][] mProxyRules;
        private String[] mBypassRules;

        /*package*/ ProxyRules(String[][] proxyRules, String[] bypassRules) {
            mProxyRules = proxyRules;
            mBypassRules = bypassRules;
        }

        /*package*/ String[][] proxyRules() {
            return mProxyRules;
        }

        /*package*/ String[] bypassRules() {
            return mBypassRules;
        }

        /**
         * ProxyRulesBuilder. This is an abstract class, use {@link ProxyRulesGlobalBuilder} or
         * {@link ProxyRulesPerSchemeBuilder} instead.
         */
        public abstract static class ProxyRulesBuilder {
            private ArrayList<String[]> mProxyRules;
            private ArrayList<String> mBypassRules;

            public ProxyRulesBuilder() {
                mProxyRules = new ArrayList<>();
                mBypassRules = new ArrayList<>();
            }

            /**
             * Builds the current rules into a ProxyRules object.
             */
            public ProxyRules build() {
                return new ProxyRules(buildProxyRules(), buildBypassRules());
            }

            /*package*/ ProxyRulesBuilder addProxyRule(String proxyUrl, String schemeFilter) {
                if (proxyUrl == null) {
                    throw new NullPointerException("Proxy string cannot be null");
                }
                String[] rule = {schemeFilter, proxyUrl};
                mProxyRules.add(rule);
                return this;
            }

            /**
             * Adds a new bypass rule that describes URLs that should skip proxy override settings
             * and make a direct connection instead. Wildcards are accepted. For instance, the rule
             * {@code *example.com} would mean that requests to the following URLs would be sent to
             * DIRECT: {@code http://example.com/}, {@code https://www.example.com/}.
             *
             * @param bypassRule Rule to be added to the exclusion list
             */
            public ProxyRulesBuilder addBypassRule(@NonNull String bypassRule) {
                if (bypassRule == null) {
                    throw new NullPointerException("Rules for the exclusion list cannot be null");
                }
                mBypassRules.add(bypassRule);
                return this;
            }

            /*package*/ String[][] buildProxyRules() {
                String[][] rules = new String[mProxyRules.size()][];
                for (int i = 0; i < mProxyRules.size(); i++) {
                    rules[i] = mProxyRules.get(i);
                }
                return rules;
            }

            /*package*/ String[] buildBypassRules() {
                String[] exclusion = new String[mBypassRules.size()];
                for (int i = 0; i < mBypassRules.size(); i++) {
                    exclusion[i] = mBypassRules.get(i);
                }
                return exclusion;
            }
        }

        /**
         * ProxyRules builder that only accepts unfiltered proxy rules, that is, proxies that will
         * be used for any URLs. Use {@link ProxyRulesPerSchemeBuilder} to build rules that filter
         * proxies per scheme.
         */
        public static class ProxyRulesGlobalBuilder extends ProxyRulesBuilder {

            /**
             * Adds a proxy to be used for all URLs.
             * <p>Proxy is either {@link ProxyController#DIRECT} or a string in the format
             * {@code [scheme://]host[:port]}. Scheme is optional and defaults to HTTP; host is one
             * of an IPv6 literal with brackets, an IPv4 literal or one or more labels separated by
             * a period; port number is optional and defaults to {@code 80} for {@code HTTP},
             * {@code 443} for {@code HTTPS} and {@code QUIC}, and {@code 1080} for {@code SOCKS}.
             * <p>
             * The correct syntax for hosts is defined by
             * <a  href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986</a>
             * <p>
             * Host examples:
             * <table>
             * <tr><th> Type </th> <th> Example </th></tr>
             * <tr><td> IPv4 literal</td> <td> 192.168.1.1 </td></tr>
             * <tr><td> IPv6 literal with brackets</td> <td> [10:20:30:40:50:60:70:80] </td></tr>
             * <tr><td> Labels </td> <td> example.com </td></tr>
             * </table>
             * <p>
             * Proxy URL examples:
             * <table>
             * <tr><th> Scheme </th> <th> Host </th> <th> Port </th> <th> Proxy URL </th></tr>
             * <tr><td></td> <td>example.com</td> <td></td> <td>example.com</td> </tr>
             * <tr><td>https</td> <td>example.com</td> <td></td> <td>https://example.com</td> </tr>
             * <tr><td></td> <td>example.com</td> <td>1111</td> <td>example.com:1111</td> </tr>
             * <tr><td>https</td> <td>example.com</td> <td>1111</td> <td>https://example.com:1111</td> </tr>
             * <tr><td></td> <td>192.168.1.1</td> <td></td> <td>192.168.1.1</td> </tr>
             * <tr><td></td> <td>192.168.1.1</td> <td>2020</td> <td>192.168.1.1:2020</td> </tr>
             * <tr><td></td> <td>[10:20:30:40:50:60:70:80]</td>
             * <td></td> <td>[10:20:30:40:50:60:70:80]</td> </tr>
             * </table>
             *
             * @param proxyUrl Proxy URL
             */
            ProxyRulesGlobalBuilder addProxyRule(@NonNull String proxyUrl) {
                super.addProxyRule(proxyUrl, null);
                return this;
            }
        }

        /**
         * ProxyRules builder that only accepts proxy rules per scheme. Use
         * {@link ProxyRulesGlobalBuilder} to build rules that don't need to filter proxies per
         * scheme.
         */
        public static class ProxyRulesPerSchemeBuilder extends ProxyRulesBuilder {

            /**
             * This does everything that {@link ProxyRulesGlobalBuilder#addProxyRule(String)} does,
             * but only applies to URLs using {@code schemeFilter}. Scheme filter must be one of
             * {@link ProxyController#HTTP}, {@link ProxyController#HTTPS},
             * {@link ProxyController#FTP}, {@link ProxyController#WS} or
             * {@link ProxyController#WSS}.
             *
             * @param proxyUrl Proxy URL
             * @param schemeFilter Scheme filter
             */
            @Override
            public ProxyRulesPerSchemeBuilder addProxyRule(@NonNull String proxyUrl,
                    @NonNull @ProxyScheme String schemeFilter) {
                super.addProxyRule(proxyUrl, schemeFilter);
                return this;
            }
        }
    }

    private static WebViewProviderFactory getFactory() {
        return WebViewGlueCommunicator.getFactory();
    }
}
