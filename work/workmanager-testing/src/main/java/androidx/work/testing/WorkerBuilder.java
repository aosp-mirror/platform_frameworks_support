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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Builds instances of {@link Worker} which can be used for testing.
 */
public class WorkerBuilder extends ListenableWorkerBuilder<WorkerBuilder> {
    private Executor mExecutor;

    private WorkerBuilder(
            @NonNull Context context,
            @NonNull String workerName,
            @NonNull Executor executor) {
        super(context, workerName);
        mExecutor = executor;
    }

    @NonNull
    @Override
    WorkerBuilder getThis() {
        return this;
    }

    /**
     * @return the background {@link Executor} used to execute this unit of work.
     */
    @NonNull
    Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Builds the {@link Worker}.
     *
     * @return the instance of a {@link Worker}.
     */
    @NonNull
    public Worker build() {
        InstantWorkTaskExecutor taskExecutor = new InstantWorkTaskExecutor();
        WorkerParameters parameters =
                new WorkerParameters(
                        getId(),
                        getInputData(),
                        getTags(),
                        getRuntimeExtras(),
                        getRunAttemptCount(),
                        getExecutor(),
                        taskExecutor,
                        getWorkerFactory()
                );

        WorkerFactory defaultFactory = WorkerFactory.getDefaultWorkerFactory();
        Worker worker =
                (Worker) defaultFactory.createWorkerWithDefaultFallback(
                        getApplicationContext(),
                        getWorkerName(),
                        parameters);

        if (worker == null) {
            throw new IllegalStateException("Could not create an instance of Worker");
        }
        return worker;
    }

    /**
     * Creates a new instance of a {@link WorkerBuilder} from a {@link WorkRequest} that runs on
     * the given {@link Executor}.
     *
     * @param context     The {@link Context}
     * @param workRequest The {@link WorkRequest}
     * @param executor    The {@link Executor}
     * @return The new instance of a {@link WorkerBuilder}
     */
    @NonNull
    public static WorkerBuilder from(
            @NonNull Context context,
            @NonNull WorkRequest workRequest,
            @NonNull Executor executor) {
        WorkSpec workSpec = workRequest.getWorkSpec();
        String name = workSpec.workerClassName;
        if (!isValidWorker(name)) {
            throw new IllegalArgumentException(
                    "Invalid worker class name or class does not extend Worker");
        }
        List<String> tags = new ArrayList<>(workRequest.getTags().size());
        tags.addAll(workRequest.getTags());
        return new WorkerBuilder(context, name, executor)
                .setId(workRequest.getId())
                .setTags(tags)
                .setInputData(workSpec.input);
    }

    private static boolean isValidWorker(@NonNull String className) {
        try {
            Class<?> klass = Class.forName(className);
            return Worker.class.isAssignableFrom(klass);
        } catch (Throwable ignore) {
            return false;
        }
    }
}
