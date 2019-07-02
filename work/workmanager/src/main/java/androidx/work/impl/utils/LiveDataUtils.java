/*
 * Copyright 2017 The Android Open Source Project
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


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for {@link LiveData}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LiveDataUtils {

    /**
     * Creates a new {@link LiveData} object that maps the values of {@code inputLiveData} using
     * {@code mappingMethod} on a background thread, but only triggers its observers when the mapped
     * values actually change.
     *
     * @param inputLiveData An input {@link LiveData}
     * @param mappingMethod A {@link Function} that maps input of type {@code In} to output of type
     *                      {@code Out}
     * @param workTaskExecutor The {@link TaskExecutor} that will run this operation on a background
     *                         thread
     * @param <In> The type of data for {@code inputLiveData}
     * @param <Out> The type of data to output
     * @return A new {@link LiveData} of type {@code Out}
     */
    public static <In, Out> LiveData<Out> dedupedMappedLiveDataFor(
            @NonNull LiveData<In> inputLiveData,
            @NonNull final Function<In, Out> mappingMethod,
            @NonNull final TaskExecutor workTaskExecutor) {

        final Object lock = new Object();
        final MediatorLiveData<Out> outputLiveData = new MediatorLiveData<>();

        outputLiveData.addSource(inputLiveData, new Observer<In>() {

            Out mCurrentOutput = null;

            @Override
            public void onChanged(@Nullable final In input) {
                workTaskExecutor.executeOnBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            Out newOutput = mappingMethod.apply(input);
                            if (mCurrentOutput == null && newOutput != null) {
                                mCurrentOutput = newOutput;
                                outputLiveData.postValue(newOutput);
                            } else if (mCurrentOutput != null
                                    && !mCurrentOutput.equals(newOutput)) {
                                mCurrentOutput = newOutput;
                                outputLiveData.postValue(newOutput);
                            }
                        }
                    }
                });
            }
        });
        return outputLiveData;
    }

    /**
     * Flatten {@link LiveData<WorkInfo>} with a {@link LiveData<Object>} for progress tracking.
     *
     * @param input    The {@link LiveData<WorkInfo>} to be flattened
     * @param progress The {@link LiveData<Object>} progress to be flattened
     * @return The flattened {@link LiveData<WorkInfo>}
     */
    @NonNull
    @MainThread
    public static LiveData<WorkInfo> flatten(
            @NonNull final LiveData<WorkInfo> input,
            @Nullable final LiveData<Object> progress) {

        final MediatorLiveData<WorkInfo> flattened = new MediatorLiveData<>();
        final Observer<Object> observer = new Observer<Object>() {
            private WorkInfo mLastWorkInfo = input.getValue();
            // If they called setProgress(...) before observation we want to make sure
            // those values carry over to WorkInfo.
            private Object mLastProgress = progress != null ? progress.getValue() : null;

            @Override
            public void onChanged(Object object) {
                if (object == null) {
                    flattened.postValue(null);
                } else if (object instanceof WorkInfo) {
                    mLastWorkInfo = (WorkInfo) object;
                } else {
                    mLastProgress = object;
                }

                List<String> tags = new ArrayList<>(mLastWorkInfo.getTags());
                // We are on the main thread so it's safe to call setValue() here.
                flattened.setValue(new WorkInfo(
                        mLastWorkInfo.getId(),
                        mLastWorkInfo.getState(),
                        mLastWorkInfo.getOutputData(),
                        tags,
                        mLastWorkInfo.getRunAttemptCount(),
                        mLastProgress));
            }
        };

        flattened.addSource(input, observer);
        if (progress != null) {
            flattened.addSource(progress, observer);
        }

        return flattened;
    }

    /**
     * Converts a {@link List<LiveData>} to a {@link LiveData<List>}.
     *
     * @param input The input {@link List<LiveData>}
     * @param <T>   The input {@link LiveData} type
     * @return The {@link List<LiveData>}
     */
    @NonNull
    @MainThread
    public static <T> LiveData<List<T>> flattenList(@NonNull List<LiveData<T>> input) {
        final List<T> slots = new ArrayList<>(input.size());
        final MediatorLiveData<List<T>> flattened = new MediatorLiveData<>();
        for (int i = 0; i < slots.size(); i++) {
            LiveData<T> source = input.get(i);
            if (source != null) {
                flattened.addSource(source, new PositionAwareObserver<>(flattened, slots, i));
            }
        }
        return flattened;
    }

    /**
     * An observer that helps convert a a {@link List<LiveData>} to a {@link LiveData<List>}.
     */
    static class PositionAwareObserver<T> implements Observer<T> {
        private final MediatorLiveData<List<T>> mMediator;
        private final List<T> mList;
        private final int mIndex;

        PositionAwareObserver(
                @NonNull MediatorLiveData<List<T>> mediator,
                @NonNull List<T> list,
                int index) {

            mMediator = mediator;
            mList = list;
            mIndex = index;
        }

        @Override
        public void onChanged(T t) {
            mList.set(mIndex, t);
            mMediator.setValue(mList);
        }
    }

    private LiveDataUtils() {
    }
}
