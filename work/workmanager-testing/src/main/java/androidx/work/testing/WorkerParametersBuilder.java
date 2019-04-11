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

package androidx.work.testing;

import android.net.Network;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * A Builder for {@link WorkerParameters}.
 */
public class WorkerParametersBuilder {

    private UUID mId;
    private int mRunAttemptCount;
    private Data mInputData;
    private List<String> mTags;
    private WorkerParameters.RuntimeExtras mRuntimeExtras;
    private Executor mExecutor;
    private TaskExecutor mTaskExecutor;
    private WorkerFactory mWorkerFactory;

    WorkerParametersBuilder() {
        mTags = Collections.emptyList();
        mInputData = Data.EMPTY;
        mRuntimeExtras = new WorkerParameters.RuntimeExtras();
    }

    /**
     * Sets the id to be used when executing a {@link androidx.work.ListenableWorker}.
     *
     * @param id The {@link UUID}
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @NonNull
    public WorkerParametersBuilder setId(@NonNull UUID id) {
        mId = id;
        return this;
    }

    /**
     * Sets the inputData to be used when executing a {@link androidx.work.ListenableWorker}.
     *
     * @param inputData The input {@link Data} when executing the
     *                  {@link androidx.work.ListenableWorker}.
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @NonNull
    public WorkerParametersBuilder setInputData(@NonNull Data inputData) {
        mInputData = inputData;
        return this;
    }

    /**
     * Sets the list of tags to be used when executing a {@link androidx.work.ListenableWorker}.
     *
     * @param tags The {@link List} of tags to be used
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @NonNull
    public WorkerParametersBuilder setTags(@NonNull List<String> tags) {
        mTags = tags;
        return this;
    }

    /**
     * Sets the run attempt count of the {@link androidx.work.ListenableWorker}.
     *
     * @param count The run attempt count
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @NonNull
    public WorkerParametersBuilder setRunAttemptCount(int count) {
        mRunAttemptCount = count;
        return this;
    }

    /**
     * Sets the triggered content uris to the used when executing a
     * {@link androidx.work.ListenableWorker}.
     *
     * @param contentUris The {@link List} of content {@link Uri}'s
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @RequiresApi(24)
    @NonNull
    public WorkerParametersBuilder setTriggeredContentUris(@NonNull List<Uri> contentUris) {
        mRuntimeExtras.triggeredContentUris = contentUris;
        return this;
    }

    /**
     * Sets the authorities for content {@link Uri}'s to the used when executing a
     * {@link androidx.work.ListenableWorker}.
     *
     * @param authorities The {@link List} of authorities
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @RequiresApi(24)
    @NonNull
    public WorkerParametersBuilder setTriggeredContentAuthorities(
            @NonNull List<String> authorities) {
        mRuntimeExtras.triggeredContentAuthorities = authorities;
        return this;
    }

    /**
     * Sets the {@link Network} to be used when executing a {@link androidx.work.ListenableWorker}.
     *
     * @param network The {@link Network} being used
     * @return The instance of {@link WorkerParametersBuilder}
     */
    @RequiresApi(28)
    public WorkerParametersBuilder setNetwork(@NonNull Network network) {
        mRuntimeExtras.network = network;
        return this;
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    WorkerParametersBuilder setBackgroundExector(@NonNull Executor exector) {
        mExecutor = exector;
        return this;
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    WorkerParametersBuilder setTaskExecutor(@NonNull TaskExecutor taskExecutor) {
        mTaskExecutor = taskExecutor;
        return this;
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    WorkerParametersBuilder setWorkerFactory(@NonNull WorkerFactory workerFactory) {
        mWorkerFactory = workerFactory;
        return this;
    }

    /**
     * Builds the {@link WorkerParameters}.
     *
     * @return The built {@link WorkerParameters}
     */
    @NonNull
    public WorkerParameters build() {
        return new WorkerParameters(mId, mInputData, mTags, mRuntimeExtras, mRunAttemptCount,
                mExecutor, mTaskExecutor, mWorkerFactory);
    }
}
