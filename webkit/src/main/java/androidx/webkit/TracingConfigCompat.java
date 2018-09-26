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

import android.webkit.TracingConfig;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Holds tracing configuration information and predefined settings.
 *
 * This class is functionally equivalent to {@link TracingConfig}.
 */
public class TracingConfigCompat {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            TracingConfig.CATEGORIES_ALL,
            TracingConfig.CATEGORIES_ANDROID_WEBVIEW,
            TracingConfig.CATEGORIES_FRAME_VIEWER,
            TracingConfig.CATEGORIES_INPUT_LATENCY,
            TracingConfig.CATEGORIES_JAVASCRIPT_AND_RENDERING,
            TracingConfig.CATEGORIES_NONE,
            TracingConfig.CATEGORIES_RENDERING,
            TracingConfig.CATEGORIES_WEB_DEVELOPER
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PredefinedCategories {}

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            TracingConfig.RECORD_CONTINUOUSLY,
            TracingConfig.RECORD_UNTIL_FULL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TracingMode {}

    private @PredefinedCategories int mPredefinedCategories;
    private final List<String> mCustomIncludedCategories = new ArrayList<>();
    private @TracingMode int mTracingMode;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public TracingConfigCompat(int predefinedCategories, List<String> customIncludedCategories,
                             int tracingMode) {
        mPredefinedCategories = predefinedCategories;
        mCustomIncludedCategories.addAll(customIncludedCategories);
        mTracingMode = tracingMode;
    }

    /**
     * Returns a bitmask of the predefined category sets of this configuration.
     *
     * @return Bitmask of predefined category sets.
     */
    @PredefinedCategories
    public int getPredefinedCategories() {
        return mPredefinedCategories;
    }

    /**
     * Returns the list of included custom category patterns for this configuration.
     *
     * @return Empty list if no custom category patterns are specified.
     */
    @NonNull
    public List<String> getCustomIncludedCategories() {
        return mCustomIncludedCategories;
    }

    /**
     * Returns the tracing mode of this configuration.
     *
     * @return The tracing mode of this configuration.
     */
    @TracingMode
    public int getTracingMode() {
        return mTracingMode;
    }

    /**
     * Builder used to create {@link TracingConfigCompat} objects.
     */
    public static class Builder {
        private @PredefinedCategories int mPredefinedCategories = TracingConfig.CATEGORIES_NONE;
        private final List<String> mCustomIncludedCategories = new ArrayList<>();
        private @TracingMode int mTracingMode = TracingConfig.RECORD_CONTINUOUSLY;

        /**
         * Default constructor for Builder.
         */
        public Builder() {
        }

        /**
         * Build {@link TracingConfigCompat} using the current settings.
         *
         * @return The {@link TracingConfigCompat} with the current settings.
         */
        public TracingConfigCompat build() {
            return new TracingConfigCompat(mPredefinedCategories, mCustomIncludedCategories,
                    mTracingMode);
        }

        /**
         * Adds predefined sets of categories to be included in the trace output.
         *
         * A predefined category set can be one of {@link TracingConfig#CATEGORIES_NONE},
         * {@link TracingConfig#CATEGORIES_ALL}, {@link TracingConfig#CATEGORIES_ANDROID_WEBVIEW},
         * {@link TracingConfig#CATEGORIES_WEB_DEVELOPER},
         * {@link TracingConfig#CATEGORIES_INPUT_LATENCY},
         * {@link TracingConfig#CATEGORIES_RENDERING},
         * {@link TracingConfig#CATEGORIES_JAVASCRIPT_AND_RENDERING} or
         * {@link TracingConfig#CATEGORIES_FRAME_VIEWER}.
         *
         * @param predefinedCategories A list or bitmask of predefined category sets.
         * @return The builder to facilitate chaining.
         */
        public Builder addCategories(@PredefinedCategories int... predefinedCategories) {
            for (int categorySet : predefinedCategories) {
                mPredefinedCategories |= categorySet;
            }
            return this;
        }

        /**
         * Adds custom categories to be included in trace output.
         *
         * Note that the categories are defined by the currently-in-use version of WebView. They
         * live in chromium code and are not part of the Android API.
         * See <a href="https://www.chromium.org/developers/how-tos/trace-event-profiling-tool">
         * chromium documentation on tracing</a> for more details.
         *
         * @param categories A list of category patterns. A category pattern can contain wildcards,
         *        e.g. "blink*" or full category name e.g. "renderer.scheduler".
         * @return The builder to facilitate chaining.
         */
        public Builder addCategories(String... categories) {
            mCustomIncludedCategories.addAll(Arrays.asList(categories));
            return this;
        }

        /**
         * Adds custom categories to be included in trace output.
         *
         * Same as {@link #addCategories(String...)} but allows to pass a Collection as a parameter.
         *
         * @param categories A list of category patterns.
         * @return The builder to facilitate chaining.
         */
        public Builder addCategories(Collection<String> categories) {
            mCustomIncludedCategories.addAll(categories);
            return this;
        }

        /**
         * Sets the tracing mode for this configuration.
         * When tracingMode is not set explicitly,
         * the default is {@link TracingConfig#RECORD_CONTINUOUSLY}.
         *
         * @param tracingMode The tracing mode to use, one of
         *                    {@link TracingConfig#RECORD_UNTIL_FULL} or
         *                    {@link TracingConfig#RECORD_CONTINUOUSLY}.
         * @return The builder to facilitate chaining.
         */
        public Builder setTracingMode(@TracingMode int tracingMode) {
            mTracingMode = tracingMode;
            return this;
        }
    }
}
