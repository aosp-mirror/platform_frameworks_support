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

package androidx.work.impl.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.Logger;
import androidx.work.ProgressUpdater;
import androidx.work.WorkInfo.State;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkProgress;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;

/**
 * Persists {@link androidx.work.ListenableWorker} progress in a {@link WorkDatabase}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkProgressUpdater implements ProgressUpdater {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("WorkProgressUpdater");

    @NonNull
    @Override
    public ListenableFuture<Void> updateProgress(
            @NonNull final Context context,
            @NonNull final UUID id,
            @NonNull final Data data) {
        final SettableFuture<Void> future = SettableFuture.create();
        final WorkManagerImpl workManager = WorkManagerImpl.getInstance(context);
        workManager.getWorkTaskExecutor().executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                String workSpecId = id.toString();
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                Logger.get().info(TAG, String.format("Updating progress for %s (%s)", id, data));
                workDatabase.beginTransaction();
                try {
                    State state = workDatabase.workSpecDao().getState(workSpecId);
                    if (!state.isFinished()) {
                        WorkProgress progress = new WorkProgress(workSpecId, data);
                        workDatabase.workProgressDao().insert(progress);
                    } else {
                        Logger.get().warning(TAG,
                                String.format(
                                        "Ignoring setProgress(...). WorkSpec (%s) has finished "
                                                + "execution.",
                                        workSpecId));
                    }
                    future.set(null);
                } catch (Throwable throwable) {
                    Logger.get().error(TAG, "Error updating Worker progress", throwable);
                    future.setException(throwable);
                } finally {
                    workDatabase.endTransaction();
                }
            }
        });
        return future;
    }
}
