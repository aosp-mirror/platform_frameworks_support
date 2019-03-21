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

abstract class RxItemKeyedDataSource<Key, Value> extends ListenableItemKeyedDataSource<Key, Value> {
    private final Scheduler mScheduler = Schedulers.from(getExecutor());

    @Override
    @NonNull
    public final ListenableFuture<InitialResult<Value>> loadInitial(
            @NonNull LoadInitialParams<Key> params
    ) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadInitial(params), mScheduler);
    }

    @Override
    @NonNull
    public final ListenableFuture<Result<Value>> loadAfter(@NonNull LoadParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadAfter(params), mScheduler);
    }

    @Override
    @NonNull
    public final ListenableFuture<Result<Value>> loadBefore(@NonNull LoadParams<Key> params) {
        return RxDataSourceUtil.singleToListenableFuture(onLoadBefore(params), mScheduler);
    }

    /**
     * Rx-extension of the parent method:
     * {@link ListenableItemKeyedDataSource#loadInitial(
     *ListenableItemKeyedDataSource.LoadInitialParams)}.
     * <p>
     * Invoked when initial data load is requested from this DataSource, e.g., when initializing or
     * resuming state of a {@link PagedList}.
     * <p>
     * The {@link Single} returned by this method will be
     * subscribed on this DataSource's executor, which is normally supplied via
     * {@link RxPagedListBuilder#setFetchScheduler(Scheduler)} or
     * {@link LivePagedListBuilder#setFetchExecutor(Executor)}.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @return {@link Single} that receives the loaded data, its size, and any adjacent page keys.
     */
    @NonNull
    public abstract Single<InitialResult<Value>> onLoadInitial(
            @NonNull LoadInitialParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenableItemKeyedDataSource#loadAfter(ListenableItemKeyedDataSource.LoadParams)}.
     * <p>
     * Invoked when a page of data is requested to be appended to this data source with the key
     * specified by {@link ItemKeyedDataSource.LoadParams#key LoadParams.key}.
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
     * @param params Parameters for the load, including the key to load after, and requested size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<Result<Value>> onLoadAfter(@NonNull LoadParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenableItemKeyedDataSource#loadBefore(ListenableItemKeyedDataSource.LoadParams)}.
     *
     * Invoked when a page of data is requested to be prepended to this data source with the key
     * specified by {@link ItemKeyedDataSource.LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * <p class="note"><strong>Note:</strong> Data returned will be prepended just before the key
     * passed, so if you don't return a page of the requested size, ensure that the last item is
     * adjacent to the passed key.
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
     * @param params Parameters for the load, including the key to load before, and requested size.
     * @return {@link Single} that receives the loaded data.
     */
    @NonNull
    public abstract Single<Result<Value>> onLoadBefore(@NonNull LoadParams<Key> params);
}
