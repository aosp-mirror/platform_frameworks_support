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
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public abstract class ListenablePageKeyedDataSource<Key, Value> extends DataSource<Key, Value> {
    ListenablePageKeyedDataSource() {
        super(KeyType.PAGE_KEYED);
    }

    @Override
    final ListenableFuture<? extends BaseResult<Value>> load(@NonNull Params<Key> params) {
        if (params.type == LoadType.INITIAL) {
            PageKeyedDataSource.LoadInitialParams<Key> initParams =
                    new PageKeyedDataSource.LoadInitialParams<>(
                            params.initialLoadSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            //noinspection ConstantConditions
            PageKeyedDataSource.LoadParams<Key> loadMoreParams =
                    new PageKeyedDataSource.LoadParams<>(params.key, params.pageSize);

            if (params.type == LoadType.START) {
                return loadBefore(loadMoreParams);
            } else if (params.type == LoadType.END) {
                return loadAfter(loadMoreParams);
            }
        }
        throw new IllegalArgumentException("Unsupported type " + params.type.toString());
    }

    abstract ListenableFuture<InitialResult<Key, Value>> loadInitial(
            @NonNull PageKeyedDataSource.LoadInitialParams<Key> params);
    abstract ListenableFuture<Result<Key, Value>> loadBefore(
            @NonNull PageKeyedDataSource.LoadParams<Key> params);
    abstract ListenableFuture<Result<Key, Value>> loadAfter(
            @NonNull PageKeyedDataSource.LoadParams<Key> params);

    @Nullable
    @Override
    Key getKey(@NonNull Value item) {
        return null;
    }

    @Override
    boolean supportsPageDropping() {
        /* To support page dropping when PageKeyed, we'll need to:
         *    - Stash keys for every page we have loaded (can id by index relative to loadInitial)
         *    - Drop keys for any page not adjacent to loaded content
         *    - And either:
         *        - Allow impl to signal previous page key: onResult(data, nextPageKey, prevPageKey)
         *        - Re-trigger loadInitial, and break assumption it will only occur once.
         */
        return false;
    }

    // TODO: builder!
    static class InitialResult<Key, Value> extends BaseResult<Value> {
        InitialResult(@NonNull List<Value> data, int position, int totalCount,
                @Nullable Key previousPageKey, @Nullable Key nextPageKey) {
            // TODO: validation on receiver side!
            super(data, previousPageKey, nextPageKey,
                    position, totalCount - data.size() - position, position);
        }

        InitialResult(@NonNull List<Value> data, @Nullable Key previousPageKey,
                @Nullable Key nextPageKey) {
            super(data, previousPageKey, nextPageKey, 0, 0, 0);
        }

        <ToValue> InitialResult(@NonNull InitialResult<Key, ToValue> result,
                @NonNull Function<List<ToValue>, List<Value>> function) {
            super(result, function);

        }
    }

    // TODO: builder!
    static class Result<Key, Value> extends BaseResult<Value> {
        public Result(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
            // TODO: validation on receiver side!
            super(data, adjacentPageKey, adjacentPageKey, 0, 0, 0);
        }

        <ToValue> Result(@NonNull Result<Key, ToValue> result,
                @NonNull Function<List<ToValue>, List<Value>> function) {
            super(result, function);
        }
    }


    // TODO: REMOVE THESE - maintenance burden
    @NonNull
    @Override
    public <ToValue> ListenablePageKeyedDataSource<Key, ToValue> mapByPage(
            @NonNull Function<List<Value>, List<ToValue>> function) {
        return new WrapperListenablePageKeyedDataSource<>(this, function);
    }

    // TODO: REMOVE THESE - maintenance burden
    @NonNull
    @Override
    public <ToValue> ListenablePageKeyedDataSource<Key, ToValue> map(@NonNull Function<Value, ToValue> function) {
        return mapByPage(createListFunction(function));
    }
}
