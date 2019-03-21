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

import java.util.concurrent.Executor;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

abstract class RxPageKeyedDataSource<Key, Value> extends ListenablePageKeyedDataSource<Key, Value> {
    private final Scheduler mScheduler = Schedulers.from(getExecutor());

    @Override
    @NonNull
    public final ListenableFuture<InitialResult<Key, Value>> loadInitial(
            @NonNull LoadInitialParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadInitial(params), mScheduler);
    }

    @Override
    @NonNull
    public final ListenableFuture<Result<Key, Value>> loadAfter(@NonNull LoadParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadAfter(params), mScheduler);
    }

    @Override
    @NonNull
    public final ListenableFuture<Result<Key, Value>> loadBefore(@NonNull LoadParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadBefore(params), mScheduler);
    }


    /**
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadInitial(
     *ListenablePageKeyedDataSource.LoadInitialParams)}.
     * <p>
     * Invoked when initial data load is requested from this DataSource, e.g., when initializing or
     * resuming state of a {@link PagedList}.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return {@link Single} that receives the loaded data, its size, and any adjacent page keys.
     */
    @NonNull
    public abstract Single<InitialResult<Key, Value>> onLoadInitial(LoadInitialParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadAfter(ListenablePageKeyedDataSource.LoadParams)}.
     * <p>
     * Invoked when a page of data is requested to be appended to this data source with the key
     * specified by {@link PageKeyedDataSource.LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<Result<Key, Value>> onLoadAfter(LoadParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadBefore(ListenablePageKeyedDataSource.LoadParams)}.
     * <p>
     * Invoked when a page of data is requested to be prepended to this data source with the key
     * specified by {@link PageKeyedDataSource.LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<Result<Key, Value>> onLoadBefore(LoadParams<Key> params);
}
