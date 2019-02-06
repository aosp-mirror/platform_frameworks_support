/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.core.text;

import android.icu.util.ULocale;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public final class ICUCompat {
    private static final String TAG = "ICUCompat";

    private static ICUCompatInternal sHelper;

    static {
        if (Build.VERSION.SDK_INT >= 24) {
            sHelper = new ICUCompat24();
        } else if (Build.VERSION.SDK_INT >= 21) {
            sHelper = new ICUCompat21();
        } else {
            sHelper = new ICUCompatPre21();
        }
    }

    private ICUCompat() {}

    /**
     * Returns the script for a given Locale.
     *
     * If the locale isn't already in its maximal form, likely subtags for the provided locale
     * ID are added before we determine the script. For further details, see the following CLDR
     * technical report :
     *
     * http://www.unicode.org/reports/tr35/#Likely_Subtags
     *
     * If locale is already in the maximal form, or there is no data available for maximization,
     * it will be just returned. For example, "und-Zzzz" cannot be maximized, since there is no
     * reasonable maximization.
     *
     * Examples:
     *
     * "en" maximizes to "en_Latn_US"
     * "de" maximizes to "de_Latn_US"
     * "sr" maximizes to "sr_Cyrl_RS"
     * "sh" maximizes to "sr_Latn_RS" (Note this will not reverse.)
     * "zh_Hani" maximizes to "zh_Hans_CN" (Note this will not reverse.)
     *
     * @return The script for a given Locale if ICU library is available, otherwise null.
     */
    @Nullable
    public static String maximizeAndGetScript(Locale locale) {
        return sHelper.maximizeAndGetScript(locale);
    }

    private interface ICUCompatInternal {
        String maximizeAndGetScript(Locale locale);
    }

    private static class ICUCompatPre21 implements ICUCompatInternal {
        private static Method sGetScriptMethod;
        private static Method sAddLikelySubtagsMethod;

        ICUCompatPre21() {
            try {
                final Class<?> clazz = Class.forName("libcore.icu.ICU");
                if (clazz != null) {
                    sGetScriptMethod = clazz.getMethod("getScript",
                            new Class[]{String.class});
                    sAddLikelySubtagsMethod = clazz.getMethod("addLikelySubtags",
                            new Class[]{String.class});
                }
            } catch (Exception e) {
                sGetScriptMethod = null;
                sAddLikelySubtagsMethod = null;

                // Nothing we can do here, we just log the exception
                Log.w(TAG, e);
            }
        }

        @java.lang.Override
        public String maximizeAndGetScript(Locale locale) {
            final String localeWithSubtags = addLikelySubtags(locale);
            if (localeWithSubtags != null) {
                return getScript(localeWithSubtags);
            }

            return null;
        }

        private static String getScript(String localeStr) {
            try {
                if (sGetScriptMethod != null) {
                    final Object[] args = new Object[]{localeStr};
                    return (String) sGetScriptMethod.invoke(null, args);
                }
            } catch (IllegalAccessException e) {
                // Nothing we can do here, we just log the exception
                Log.w(TAG, e);
            } catch (InvocationTargetException e) {
                // Nothing we can do here, we just log the exception
                Log.w(TAG, e);
            }
            return null;
        }

        private static String addLikelySubtags(Locale locale) {
            final String localeStr = locale.toString();
            try {
                if (sAddLikelySubtagsMethod != null) {
                    final Object[] args = new Object[]{localeStr};
                    return (String) sAddLikelySubtagsMethod.invoke(null, args);
                }
            } catch (IllegalAccessException e) {
                // Nothing we can do here, we just log the exception
                Log.w(TAG, e);
            } catch (InvocationTargetException e) {
                // Nothing we can do here, we just log the exception
                Log.w(TAG, e);
            }

            return localeStr;
        }
    }

    @RequiresApi(21)
    private static class ICUCompat21 implements ICUCompatInternal {
        private static Method sAddLikelySubtagsMethod;

        ICUCompat21() {
            try {
                // This class should always exist on API-21 since it's CTS tested.
                final Class<?> clazz = Class.forName("libcore.icu.ICU");
                sAddLikelySubtagsMethod = clazz.getMethod("addLikelySubtags",
                        new Class[]{Locale.class});
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @java.lang.Override
        public String maximizeAndGetScript(Locale locale) {
            try {
                final Object[] args = new Object[]{locale};
                return ((Locale) sAddLikelySubtagsMethod.invoke(null, args)).getScript();
            } catch (InvocationTargetException e) {
                Log.w(TAG, e);
            } catch (IllegalAccessException e) {
                Log.w(TAG, e);
            }
            return locale.getScript();
        }
    }

    @RequiresApi(24)
    private static class ICUCompat24 implements ICUCompatInternal {
        ICUCompat24() {
        }

        @java.lang.Override
        public String maximizeAndGetScript(Locale locale) {
            ULocale uLocale = ULocale.addLikelySubtags(ULocale.forLocale(locale));
            return uLocale.getScript();
        }
    }
}
