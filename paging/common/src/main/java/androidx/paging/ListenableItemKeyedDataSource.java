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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 * <p>
 * Implement a DataSource using ListenableItemKeyedDataSource if you need to use data from item
 * {@code N - 1} to load item {@code N}. This is common, for example, in uniquely sorted database
 * queries where attributes of the item such just before the next query define how to execute it.
 *
 * @see ItemKeyedDataSource
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class ListenableItemKeyedDataSource<Key, Value> extends DataSource<Key, Value> {
    public ListenableItemKeyedDataSource() {
        super(KeyType.ITEM_KEYED);
    }

    @Override
    final ListenableFuture<? extends BaseResult<Value>> load(@NonNull Params<Key> params) {
        if (params.type == LoadType.INITIAL) {
            ItemKeyedDataSource.LoadInitialParams<Key> initParams =
                    new ItemKeyedDataSource.LoadInitialParams<>(params.key,
                            params.initialLoadSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            //noinspection ConstantConditions (key is known to be non-null for non-initial queries)
            ItemKeyedDataSource.LoadParams<Key> loadParams =
                    new ItemKeyedDataSource.LoadParams<>(params.key, params.pageSize);

            if (params.type == LoadType.START) {
                return loadBefore(loadParams);
            } else if (params.type == LoadType.END) {
                return loadAfter(loadParams);
            }
        }
        throw new IllegalArgumentException("Unsupported type " + params.type.toString());
    }

    /**
     * Load initial data.
     * <p>
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass {@code totalCount}
     * to the {@link InitialResult} constructor. This enables PagedLists presenting data from this
     * source to display placeholders to represent unloaded items.
     * <p>
     * {@link ItemKeyedDataSource.LoadInitialParams#requestedInitialKey} and
     * {@link ItemKeyedDataSource.LoadInitialParams#requestedLoadSize} are hints, not requirements,
     * so they may be altered or ignored. Note that ignoring the {@code requestedInitialKey} can
     * prevent subsequent PagedList/DataSource pairs from initializing at the same location. If your
     * DataSource never invalidates (for example, loading from the network without the network ever
     * signalling that old data must be reloaded), it's fine to ignore the {@code initialLoadKey}
     * and always start from the beginning of the data set.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<InitialResult<Value>> loadInitial(
            @NonNull ItemKeyedDataSource.LoadInitialParams<Key> params);


    /**
     * Load list data after the key specified in
     * {@link ItemKeyedDataSource.LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key to load after, and requested size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<Result<Value>> loadAfter(
            @NonNull ItemKeyedDataSource.LoadParams<Key> params);


    /**
     * Load list data after the key specified in
     * {@link ItemKeyedDataSource.LoadParams#key LoadParams.key}.
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
     *
     * @param params Parameters for the load, including the key to load before, and requested size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<Result<Value>> loadBefore(
            @NonNull ItemKeyedDataSource.LoadParams<Key> params);


    @Nullable
    @Override
    public abstract Key getKey(@NonNull Value item);

    /**
     * Type produced by {@link #loadInitial(ItemKeyedDataSource.LoadInitialParams)} to represent
     * initially loaded data.
     *
     * @param <V> The type of the data loaded.
     */
    public static class InitialResult<V> extends BaseResult<V> {
        public InitialResult(@NonNull List<V> data, int position, int totalCount) {
            super(data, null, null, position, totalCount - data.size() - position, position, true);
        }

        public InitialResult(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0, false);
        }
    }

    /**
     * Type produced by {@link #loadBefore(ItemKeyedDataSource.LoadParams)} and
     * {@link #loadAfter(ItemKeyedDataSource.LoadParams)} to represent a page of loaded data.
     *
     * @param <V> The type of the data loaded.
     */
    public static class Result<V> extends BaseResult<V> {
        public Result(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0, false);
        }
    }
}
