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
import androidx.paging.futures.DirectExecutor;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

class WrapperListenablePageKeyedDataSource<K, A, B> extends ListenablePageKeyedDataSource<K, B> {
    private final ListenablePageKeyedDataSource<K, A> mSource;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Function<List<A>, List<B>> mListFunction;

    WrapperListenablePageKeyedDataSource(ListenablePageKeyedDataSource<K, A> source,
            Function<List<A>, List<B>> listFunction) {
        mSource = source;
        mListFunction = listFunction;
    }

    @Override
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        mSource.addInvalidatedCallback(onInvalidatedCallback);
    }

    @Override
    public void removeInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        mSource.removeInvalidatedCallback(onInvalidatedCallback);
    }

    @Override
    public void invalidate() {
        mSource.invalidate();
    }

    @Override
    public boolean isInvalid() {
        return mSource.isInvalid();
    }
    // TODO: map load() instead. means apps can't test their wrappers, but much less code?

    @Override
    ListenableFuture<InitialResult<K, B>> loadInitial(
            @NonNull PageKeyedDataSource.LoadInitialParams<K> params) {
        return Futures.transform(
                mSource.loadInitial(params),
                new Function<InitialResult<K, A>, InitialResult<K, B>>() {
                    @Override
                    public InitialResult<K, B> apply(InitialResult<K, A> input) {
                        return new InitialResult<>(input, mListFunction);
                    }
                },
                DirectExecutor.INSTANCE);
    }

    @Override
    ListenableFuture<Result<K, B>> loadBefore(@NonNull PageKeyedDataSource.LoadParams<K> params) {
        return Futures.transform(
                mSource.loadBefore(params),
                new Function<Result<K, A>, Result<K, B>>() {
                    @Override
                    public Result<K, B> apply(Result<K, A> input) {
                        return new Result<>(input, mListFunction);
                    }
                },
                DirectExecutor.INSTANCE);
    }

    @Override
    ListenableFuture<Result<K, B>> loadAfter(@NonNull PageKeyedDataSource.LoadParams<K> params) {
        return Futures.transform(
                mSource.loadAfter(params),
                new Function<Result<K, A>, Result<K, B>>() {
                    @Override
                    public Result<K, B> apply(Result<K, A> input) {
                        return new Result<>(input, mListFunction);
                    }
                },
                DirectExecutor.INSTANCE);
    }
}
