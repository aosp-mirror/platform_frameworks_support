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

abstract class ListenablePositionalSource<V> extends ListenableSource<Integer, V> {

    ListenablePositionalSource() {
        super(KeyType.POSITIONAL);
    }

    @Override
    ListenableFuture<? extends BaseResult<V>> load(Params<Integer> params) {
        if (params.type == LoadType.INITIAL) {
            PositionalDataSource.LoadInitialParams initParams =
                    new PositionalDataSource.LoadInitialParams(params.key, params.requestedLoadSize,
                            params.pageSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            PositionalDataSource.LoadRangeParams rangeParams =
                    new PositionalDataSource.LoadRangeParams(params.key, params.requestedLoadSize);
            return loadRange(rangeParams);
        }
    }

    abstract ListenableFuture<InitialResult<V>> loadInitial(
            @NonNull PositionalDataSource.LoadInitialParams params);
    abstract ListenableFuture<RangeResult<V>> loadRange(
            @NonNull PositionalDataSource.LoadRangeParams params);

    @Nullable
    @Override
    Integer getKey(@NonNull V item) {
        return null;
    }

    static class InitialResult<V> extends BaseResult<V> {
        InitialResult(@NonNull List<V> data, int position, int totalCount) {
            // TODO: validation!
            super(data, null, null, position, totalCount - data.size() - position, position);
        }

        InitialResult(@NonNull List<V> data, int position) {
            super(data, null, null, 0, 0, position);
        }
    }

    static class RangeResult<V> extends BaseResult<V> {
        public RangeResult(@NonNull List<V> data) {
            // TODO: validation!
            super(data, null, null, 0, 0, 0);
        }
    }
}
