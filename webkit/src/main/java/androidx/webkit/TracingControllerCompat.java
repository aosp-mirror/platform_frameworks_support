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

import android.content.Context;
import android.os.AsyncTask;
import android.webkit.TracingConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.TracingControllerImpl;

import java.io.OutputStream;
import java.util.concurrent.Executor;

/**
 * Manages tracing of WebViews. In particular provides functionality for the app
 * to enable/disable tracing of parts of code and to collect tracing data.
 * This is useful for profiling performance issues, debugging and memory usage
 * analysis in production and real life scenarios.
 * <p>
 * The resulting trace data is sent back as a byte sequence in json format. This
 * file can be loaded in "chrome://tracing" for further analysis.
 * <p>
 * Example usage:
 * <pre class="prettyprint">
 * TracingControllerCompat tracingController = TracingControllerCompat.getInstance();
 * tracingController.start(TracingConfig.CATEGORIES_WEB_DEVELOPER, new ArrayList<>(),
 *                         TracingConfig.RECORD_CONTINUOUSLY);
 * ...
 * tracingController.stop(new FileOutputStream("trace.json"),
 *                        Executors.newSingleThreadExecutor());
 * </pre>
 */
public abstract class TracingControllerCompat {
    /**
     *
     * @hide Don't allow apps to sub-class this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public TracingControllerCompat() {}

    /**
     * Returns the default {@link TracingControllerCompat} instance. At present there is
     * only one TracingController instance for all WebView instances.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static TracingControllerCompat getInstance() {
        return LAZY_HOLDER.INSTANCE;
    }

    private static class LAZY_HOLDER {
        static final TracingControllerCompat INSTANCE = new TracingControllerImpl();
    }

    /**
     * @return Whether the WebView framework is tracing.
     */
    public abstract boolean isTracing();

    /**
     * Starts tracing all webviews. Depending on the trace mode in traceConfig
     * specifies how the trace events are recorded.
     *
     * <p>
     * For tracing modes {@link TracingConfig#RECORD_UNTIL_FULL} and
     * {@link TracingConfig#RECORD_CONTINUOUSLY} the events are recorded
     * using an internal buffer and flushed to the outputStream when
     * {@link #stop(OutputStream, Executor)} is called.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     * @param tracingConfig Configuration options to use for tracing.
     *
     * @throws IllegalStateException If the system is already tracing.
     * @throws IllegalArgumentException If the configuration is invalid (e.g.
     *         invalid category pattern or invalid tracing mode).
     */
    public abstract void start(@NonNull TracingConfigCompat tracingConfig)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Stops tracing and flushes tracing data to the specified outputStream.
     * The data is sent to the specified output stream in json format typically in chunks
     * by invoking {@link OutputStream#write(byte[])}.
     * On completion the {@link OutputStream#close()} method is called.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     * @param outputStream The output stream the tracing data will be sent to.
     *                     If {@code null} the tracing data will be discarded.
     * @param executor The Executor on which the outputStream {@link OutputStream#write(byte[])} and
     *                 {@link OutputStream#close()} methods will be invoked.
     *
     *                 Callback and listener events are dispatched through this Executor,
     *                 providing an easy way to control which thread is used.
     *                 To dispatch events through the main thread of your application,
     *                 you can use {@link Context#getMainExecutor()}.
     *                 To dispatch events through a shared thread pool,
     *                 you can use {@link AsyncTask#THREAD_POOL_EXECUTOR}.
     *
     * @return {@code false} if the WebView was not tracing at the time of the call,
     * {@code true} otherwise.
     */
    public abstract boolean stop(@Nullable OutputStream outputStream, @NonNull Executor executor);
}
