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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param inputLiveData    An input {@link LiveData}
     * @param mappingMethod    A {@link Function} that maps input of type {@code In} to output of
     *                        type
     *                         {@code Out}
     * @param workTaskExecutor The {@link TaskExecutor} that will run this operation on a background
     *                         thread
     * @param <In>             The type of data for {@code inputLiveData}
     * @param <Out>            The type of data to output
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

                flattened.setValue(workInfoWithProgress(mLastWorkInfo, mLastProgress));
            }
        };

        flattened.addSource(input, observer);
        if (progress != null) {
            flattened.addSource(progress, observer);
        }

        // Set initial value
        WorkInfo workInfo = input.getValue();
        if (workInfo != null) {
            Object value = progress != null ? progress.getValue() : null;
            flattened.setValue(workInfoWithProgress(workInfo, value));
        }
        return flattened;
    }

    /**
     * Combines live data from 2 sources to and merges it into a single LiveData.
     *
     * @param input    The {@link LiveData<WorkInfo>} to be flattened
     * @param progress The {@link LiveData} to a map of workSpecId to progress
     * @return The merged {@link LiveData<WorkInfo>}
     */
    @MainThread
    @NonNull
    @SuppressWarnings("unchecked")
    public static LiveData<List<WorkInfo>> flattenList(
            @NonNull final LiveData<List<WorkInfo>> input,
            @NonNull final LiveData<Map<String, Object>> progress) {

        final MediatorLiveData<List<WorkInfo>> flattened = new MediatorLiveData<>();
        final Observer<Object> observer = new Observer<Object>() {
            List<WorkInfo> mWorkInfoList = input.getValue();
            Map<String, Object> mProgress = progress.getValue();

            @Override
            public void onChanged(Object object) {
                if (object != null) {
                    if (object instanceof List) {
                        mWorkInfoList = (List<WorkInfo>) object;
                    } else if (object instanceof Map) {
                        mProgress = (Map<String, Object>) object;
                    }
                }

                if (mWorkInfoList == null) {
                    flattened.setValue(null);
                } else if (mWorkInfoList.isEmpty() || mProgress.isEmpty()) {
                    flattened.setValue(mWorkInfoList);
                } else {
                    flattened.setValue(listOfWorkInfoWithProgress(mWorkInfoList, mProgress));
                }
            }
        };
        flattened.addSource(input, observer);
        flattened.addSource(progress, observer);

        // Set initial value
        List<WorkInfo> workInfoList = input.getValue();
        Map<String, Object> progressMap = progress.getValue();
        if (workInfoList != null && progressMap != null) {
            flattened.setValue(listOfWorkInfoWithProgress(workInfoList, progressMap));
        }

        return flattened;
    }


    /**
     * Converts a {@link List<LiveData>} and a {@link List<String>} WorkSpec ids to a
     * {@link LiveData<Map>}.
     *
     * @param ids   The input {@link List} of WorkSpec ids.
     * @param input The input {@link List<LiveData>}
     * @param <T>   The input {@link LiveData} type
     * @return The {@link List<LiveData>}
     */
    @NonNull
    @MainThread
    public static <T> LiveData<Map<String, T>> flattenToMap(
            @NonNull List<String> ids,
            @NonNull List<LiveData<T>> input) {

        final Map<String, T> slots = new HashMap<>();
        final MediatorLiveData<Map<String, T>> flattened = new MediatorLiveData<>();
        for (int i = 0; i < input.size(); i++) {
            String id = ids.get(i);
            LiveData<T> source = input.get(i);
            if (source != null) {
                slots.put(id, source.getValue());
                flattened.addSource(source, new IdAwareObserver<>(flattened, id, slots));
            }
        }
        // Set initial value
        flattened.setValue(slots);
        return flattened;
    }

    /**
     * An observer that's WorkSpec id aware.
     */
    static class IdAwareObserver<T> implements Observer<T> {
        private final MediatorLiveData<Map<String, T>> mMediator;
        private final String mId;
        private final Map<String, T> mMap;

        IdAwareObserver(
                @NonNull MediatorLiveData<Map<String, T>> mediator,
                @NonNull String id,
                @NonNull Map<String, T> map) {
            mMediator = mediator;
            mMap = map;
            mId = id;
        }

        @Override
        public void onChanged(T t) {
            mMap.put(mId, t);
            mMediator.setValue(mMap);
        }
    }

    // Synthetic accessor
    @NonNull
    static WorkInfo workInfoWithProgress(@NonNull WorkInfo workInfo, @Nullable Object progress) {
        if (progress == null) {
            return workInfo;
        }

        List<String> tags = new ArrayList<>(workInfo.getTags());
        return new WorkInfo(workInfo.getId(),
                workInfo.getState(),
                workInfo.getOutputData(),
                tags,
                workInfo.getRunAttemptCount(),
                progress);
    }

    // Synthetic accessor
    @NonNull
    static List<WorkInfo> listOfWorkInfoWithProgress(
            @NonNull List<WorkInfo> infoList,
            @NonNull Map<String, Object> progress) {

        List<WorkInfo> newList = new ArrayList<>(infoList.size());
        for (WorkInfo info : infoList) {
            String id = info.getId().toString();
            newList.add(workInfoWithProgress(info, progress.get(id)));
        }
        return newList;
    }

    private LiveDataUtils() {
    }
}
