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

public abstract class ListenableItemKeyedSource<K, V> extends ListenableSource<K, V> {
    ListenableItemKeyedSource() {
        super(KeyType.ITEM_KEYED);
    }

    @Override
    ListenableFuture<? extends BaseResult<V>> load(@NonNull Params<K> params) {
        if (params.type == LoadType.INITIAL) {
            ItemKeyedDataSource.LoadInitialParams<K> initParams =
                    new ItemKeyedDataSource.LoadInitialParams<>(params.key,
                            params.requestedLoadSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            ItemKeyedDataSource.LoadParams<K> loadMoreParams =
                    new ItemKeyedDataSource.LoadParams<>(params.key, params.requestedLoadSize);

            if (params.type == LoadType.START) {
                return loadBefore(loadMoreParams);
            } else if (params.type == LoadType.END) {
                return loadAfter(loadMoreParams);
            }
        }
        throw new IllegalArgumentException("Unsupported type " + params.type.toString());
    }

    abstract ListenableFuture<InitialResult<V>> loadInitial(
            @NonNull ItemKeyedDataSource.LoadInitialParams params);
    abstract ListenableFuture<Result<V>> loadBefore(
            @NonNull ItemKeyedDataSource.LoadParams params);
    abstract ListenableFuture<Result<V>> loadAfter(
            @NonNull ItemKeyedDataSource.LoadParams params);

    @Nullable
    @Override
    abstract public K getKey(@NonNull V item);

    static class InitialResult<V> extends BaseResult<V> {
        InitialResult(@NonNull List<V> data, int position, int totalCount) {
            // TODO: validation!
            super(data, null, null, position, totalCount - data.size() - position, position);
        }

        InitialResult(@NonNull List<V> data, int position) {
            super(data, null, null, 0, 0, position);
        }
    }

    static class Result<V> extends BaseResult<V> {
        public Result(@NonNull List<V> data) {
            // TODO: validation!
            super(data, null, null, 0, 0, 0);
        }
    }
}
