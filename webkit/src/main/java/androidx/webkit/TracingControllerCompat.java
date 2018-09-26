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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.webkit.TracingConfig;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.TracingControllerImpl;

import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Manages Service Workers used by WebView.
 */
public abstract class TracingControllerCompat {
    /** @hide */
    @SuppressLint("InlinedApi")
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
    public @interface TracingControllerPredefinedCategories {}

    /** @hide */
    @SuppressLint("InlinedApi")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            TracingConfig.RECORD_CONTINUOUSLY,
            TracingConfig.RECORD_UNTIL_FULL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TracingControllerMode {}

    /**
     *
     * @hide Don't allow apps to sub-class this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public TracingControllerCompat() {}

    /**
     * Returns a TracingController instance.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static TracingControllerCompat getInstance() {
        return new TracingControllerImpl();
    }

    /**
     * @return Whether the WebView framework is tracing.
     */
    @NonNull
    public abstract boolean isTracing();

    /**
     * Starts tracing all webviews.
     * Depending on the trace mode in traceConfig specifies how the trace events are recorded.
     * For tracing modes TracingConfig.RECORD_UNTIL_FULL and TracingConfig.RECORD_CONTINUOUSLY
     * the events are recorded using an internal buffer and flushed
     * to the outputStream when stop(OutputStream, Executor) is called.
     *
     * @param predefinedCategories A bitmask of predefined category sets.
     * @param customIncludedCategories A list of category patterns. A category pattern can contain
     *                                 wildcards, e.g. "blink*" or full category name
     *                                 e.g. "renderer.scheduler".
     * @param mode The tracing mode to use, one of {@link TracingConfig#RECORD_UNTIL_FULL} or
     *             {@link TracingConfig#RECORD_CONTINUOUSLY}
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public abstract void start(int predefinedCategories,
            Collection<String> customIncludedCategories, @TracingControllerMode int mode)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Starts tracing all webviews with default options:
     * {@link TracingConfig#CATEGORIES_NONE}, {@link TracingConfig#RECORD_CONTINUOUSLY}.
     *
     * {@see TracingControllerCompat#start(int, Collection, int)}
     */
    public void start() throws IllegalStateException, IllegalArgumentException {
        start(TracingConfig.CATEGORIES_NONE, new ArrayList<String>(),
                TracingConfig.RECORD_CONTINUOUSLY);
    }

    /**
     * Stops tracing and flushes tracing data to the specified outputStream.
     * The data is sent to the specified output stream in json format typically in chunks
     * by invoking {@link OutputStream#write(byte[])}.
     * On completion the {@link OutputStream#close()} method is called.
     *
     * @param outputStream The output stream the tracing data will be sent to.
     *                     If null the tracing data will be discarded.
     * @param executor The Executor on which the outputStream {@link OutputStream#write(byte[])} and
     *                 {@link OutputStream#close()} methods will be invoked.
     *                 This value must never be null.
     *
     *                 Callback and listener events are dispatched through this Executor,
     *                 providing an easy way to control which thread is used.
     *                 To dispatch events through the main thread of your application,
     *                 you can use {@link Context#getMainExecutor()}.
     *                 To dispatch events through a shared thread pool,
     *                 you can use {@link AsyncTask#THREAD_POOL_EXECUTOR}.
     *
     * @return False if the WebView was not tracing at the time of the call,
     * true otherwise.
     */
    public abstract boolean stop(@Nullable OutputStream outputStream, Executor executor);
}
