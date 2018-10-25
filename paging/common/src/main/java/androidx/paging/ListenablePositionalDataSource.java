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
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

public abstract class ListenablePositionalDataSource<Value> extends DataSource<Integer, Value> {
    ListenablePositionalDataSource() {
        super(KeyType.POSITIONAL);
    }

    @Override
    final ListenableFuture<? extends BaseResult<Value>> load(@NonNull Params<Integer> params) {
        if (params.type == LoadType.INITIAL) {
            int initialPosition = 0;
            if (params.key != null) {
                initialPosition = Math.max(0, params.key - params.initialLoadSize / 2);
            }
            PositionalDataSource.LoadInitialParams initParams =
                    new PositionalDataSource.LoadInitialParams(initialPosition, params.initialLoadSize,
                            params.pageSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            if (params.key < 0) {
                // no remaining data, immediately return empty data
                ResolvableFuture<BaseResult<Value>> future = ResolvableFuture.create();
                future.set(new BaseResult<Value>());
                return future;
            }

            int startIndex = params.key;
            int loadSize = params.pageSize;
            if (params.type == LoadType.START) {
                loadSize = Math.min(loadSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
            }
            return loadRange(new PositionalDataSource.LoadRangeParams(startIndex, loadSize));
        }
    }

    abstract ListenableFuture<InitialResult<Value>> loadInitial(
            @NonNull PositionalDataSource.LoadInitialParams params);
    abstract ListenableFuture<RangeResult<Value>> loadRange(
            @NonNull PositionalDataSource.LoadRangeParams params);

    @Nullable
    @Override
    Integer getKey(@NonNull Value item) {
        return null;
    }

    public static class InitialResult<V> extends BaseResult<V> {
        // TODO: builder!
        public InitialResult(@NonNull List<V> data, int position, int totalCount) {
            super(data, null, null, position, totalCount - data.size() - position, 0);
        }

        public InitialResult(@NonNull List<V> data, int position) {
            super(data, null, null, 0, 0, position);
            if (data.isEmpty() && position != 0) {
                throw new IllegalArgumentException(
                        "Initial result cannot be empty if items are present in data set.");
            }
        }
    }

    public static class RangeResult<V> extends BaseResult<V> {
        // TODO: builder!
        public RangeResult(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }
}
