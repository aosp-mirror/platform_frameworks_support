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

package androidx.work;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The default {@link WorkerFactory} for WorkManager.  To specify your own WorkerFactory, initialize
 * WorkManager manually (see {@link WorkManager#initialize(Context, Configuration)}) and use
 * {@link Configuration.Builder#setWorkerFactory(WorkerFactory)}.
 */
public class DefaultWorkerFactory implements WorkerFactory {

    private static final String TAG = "DefaultWorkerFactory";

    @Nullable
    @Override
    public <T extends NonBlockingWorker> T createWorker(
            @NonNull Class<T> workerClass,
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        String workerClassName = workerClass.getName();
        try {
            T worker;
            try {
                Constructor<T> constructor =
                        workerClass.getDeclaredConstructor(Context.class, WorkerParameters.class);
                worker = constructor.newInstance(
                        appContext.getApplicationContext(),
                        workerParameters);
            } catch (NoSuchMethodException e) {
                worker = workerClass.getDeclaredConstructor().newInstance();
                Method internalInitMethod = NonBlockingWorker.class.getDeclaredMethod(
                        "internalInit",
                        Context.class,
                        WorkerParameters.class);
                internalInitMethod.setAccessible(true);
                internalInitMethod.invoke(
                        worker,
                        appContext.getApplicationContext(),
                        workerParameters);
            }
            return worker;
        } catch (Exception e) {
            Logger.error(TAG, "Could not instantiate " + workerClassName, e);
        }
        return null;
    }
}
