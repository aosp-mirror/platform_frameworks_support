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
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public abstract class ListenablePositionalSource<V> extends ListenableSource<Integer, V> {
    ListenablePositionalSource() {
        super(KeyType.POSITIONAL);
    }

    @Override
    final ListenableFuture<? extends BaseResult<V>> load(@NonNull Params<Integer> params) {
        if (params.type == LoadType.INITIAL) {
            PositionalDataSource.LoadInitialParams initParams =
                    new PositionalDataSource.LoadInitialParams(params.key, params.initialLoadSize,
                            params.pageSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            // TODO: test this!
            System.out.println("load range " + params.key + " size " + params.pageSize + " type " + params.type);

            if (params.key < 0) {
                // Trigger empty
                ResolvableFuture<BaseResult<V>> immediatelyEmptyFuture = ResolvableFuture.create();
                immediatelyEmptyFuture.set(
                        new BaseResult<V>(Collections.<V>emptyList(), null, null, 0, 0, 0) {});
                return immediatelyEmptyFuture;
            }

            int startIndex = params.key;
            int loadSize = params.pageSize;
            if (params.type == LoadType.START) {
                loadSize = Math.min(loadSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
            }
            System.out.println("load range actually, size " + loadSize);
            return loadRange(new PositionalDataSource.LoadRangeParams(startIndex, loadSize));
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

    public static class InitialResult<V> extends BaseResult<V> {
        // TODO: builder!
        public InitialResult(@NonNull List<V> data, int position, int totalCount) {
            // TODO: validation on receiver side!
            super(data, null, null, position, totalCount - data.size() - position, 0);
        }

        public InitialResult(@NonNull List<V> data, int position) {
            super(data, null, null, 0, 0, position);
        }
    }

    public static class RangeResult<V> extends BaseResult<V> {
        // TODO: builder!
        public RangeResult(@NonNull List<V> data) {
            // TODO: validation on receiver side!
            super(data, null, null, 0, 0, 0);
        }
    }

    @NonNull
    @Override
    final Integer getKey(int lastLoad, @NonNull V item) {
        return lastLoad;
    }
}
