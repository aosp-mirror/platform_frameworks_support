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

public abstract class ListenableItemKeyedDataSource<Key, Value> extends DataSource<Key, Value> {
    ListenableItemKeyedDataSource() {
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
            //noinspection ConstantConditions
            ItemKeyedDataSource.LoadParams<Key> loadMoreParams =
                    new ItemKeyedDataSource.LoadParams<>(params.key, params.pageSize);

            if (params.type == LoadType.START) {
                return loadBefore(loadMoreParams);
            } else if (params.type == LoadType.END) {
                return loadAfter(loadMoreParams);
            }
        }
        throw new IllegalArgumentException("Unsupported type " + params.type.toString());
    }

    @NonNull
    public abstract ListenableFuture<InitialResult<Value>> loadInitial(
            @NonNull ItemKeyedDataSource.LoadInitialParams<Key> params);
    @NonNull
    public abstract ListenableFuture<Result<Value>> loadBefore(
            @NonNull ItemKeyedDataSource.LoadParams<Key> params);
    @NonNull
    public abstract ListenableFuture<Result<Value>> loadAfter(
            @NonNull ItemKeyedDataSource.LoadParams<Key> params);

    @Nullable
    @Override
    public abstract Key getKey(@NonNull Value item);

    public static class InitialResult<V> extends BaseResult<V> {
        public InitialResult(@NonNull List<V> data, int position, int totalCount) {
            super(data, null, null, position, totalCount - data.size() - position, position, true);
        }

        public InitialResult(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0, false);
        }
    }

    public static class Result<V> extends BaseResult<V> {
        public Result(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0, false);
        }
    }
}
