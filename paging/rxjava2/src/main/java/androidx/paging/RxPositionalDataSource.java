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

package androidx.paging;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

abstract class RxPositionalDataSource<T> extends ListenablePositionalDataSource<T> {
    private Scheduler mScheduler;

    @Override
    @NonNull
    public final ListenableFuture<InitialResult<T>> loadInitial(
            @NonNull final LoadInitialParams params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadInitial(params), getScheduler());
    }

    @Override
    @NonNull
    public final ListenableFuture<RangeResult<T>> loadRange(@NonNull final LoadRangeParams params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadRange(params), getScheduler());
    }

    /**
     * An explicit getter is necessary to lazily init mScheduler as {@link DataSource#getExecutor()}
     * throws an error if it is accessed before this DataSource is added to a {@link PagedList} and
     * assigned an {@link Executor}.
     */
    private Scheduler getScheduler() {
        if (mScheduler == null) {
            mScheduler = Schedulers.from(getExecutor());
        }

        return mScheduler;
    }

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePositionalDataSource#loadInitial(
     *ListenablePositionalDataSource.LoadInitialParams)}.
     * <p>
     * Invoked when initial data load is requested from this DataSource, e.g., when initializing or
     * resuming state of a {@link PagedList}.
     * <p>
     * This method is called to load the initial page(s) from the DataSource.
     * <p>
     * Result list must be a multiple of pageSize to enable efficient tiling.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return {@link Single} that receives the loaded data, including position and total data
     * set size.
     */
    @NonNull
    public abstract Single<InitialResult<T>> onLoadInitial(LoadInitialParams params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePositionalDataSource#loadRange(
     *ListenablePositionalDataSource.LoadRangeParams)}.
     * <p>
     * Invoked when a range of data is requested to be loaded from this DataSource.
     * <p>
     * This method is called to load additional pages from the DataSource after the
     * LoadInitialCallback passed to dispatchLoadInitial has initialized a PagedList.
     * <p>
     * Unlike {@link #loadInitial(LoadInitialParams)}, this method must return
     * the number of items requested, at the position requested.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for load, including start position and load size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<RangeResult<T>> onLoadRange(LoadRangeParams params);
}
