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

package androidx.enterprise.feedback;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration values for the {@link KeyedAppStatesReporter} instance.
 */
@AutoValue
public abstract class Configuration {
    private static final int EXECUTOR_IDLE_ALIVE_TIME_SECS = 20;
    // Create a no-args constructor so it doesn't appear in current.txt
    Configuration() { }

    /** Create a {@link ConfigurationBuilder}. */
    @NonNull
    public static ConfigurationBuilder builder() {
        return new AutoValue_Configuration.Builder();
    }

    static Configuration createDefaultConfiguration() {
        return builder()
            .setExecutor(createExecutorService())
            .build();
    }

    /**
     * Creates an {@link ExecutorService} which has no persistent background thread, and ensures
     * tasks will run in submit order.
     */
    private static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(
            /* corePoolSize= */ 0,
            /* maximumPoolSize= */ 1,
            EXECUTOR_IDLE_ALIVE_TIME_SECS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>() /* Not used */);
    }

    /**
     * The executor to use.
     *
     * <p>To set a custom executor, the executor must run all {@link Runnable} instances on the same
     * thread, serially.
     */
    @NonNull
    public abstract Executor executor();

    /** The builder for {@link Configuration}. */
    @AutoValue.Builder
    public abstract static class ConfigurationBuilder {
        // Create a no-args constructor so it doesn't appear in current.txt
        ConfigurationBuilder() { }

        /** Set {@link Configuration#executor()}. */
        @NonNull
        public abstract ConfigurationBuilder setExecutor(@NonNull Executor executor);

        /** Instantiate the {@link Configuration}. */
        @NonNull
        public abstract Configuration build();
    }
}
