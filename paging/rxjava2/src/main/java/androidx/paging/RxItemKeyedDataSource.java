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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.schedulers.Schedulers;

abstract class RxItemKeyedDataSource<Key, Value> extends ListenableItemKeyedDataSource<Key, Value> {
    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<InitialResult<Value>> loadInitial(
            @NonNull LoadInitialParams<Key> params
    ) {
        final ResolvableFuture<InitialResult<Value>> future = ResolvableFuture.create();
        onLoadInitial(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<InitialResult<Value>, Throwable>() {
                    @Override
                    public void accept(InitialResult<Value> result,
                            Throwable throwable) {
                        if (throwable != null) {
                            future.setException(throwable);
                        }

                        future.set(result);
                    }
                });
        return future;
    }

    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<Result<Value>> loadAfter(@NonNull LoadParams<Key> params) {
        final ResolvableFuture<Result<Value>> future = ResolvableFuture.create();
        onLoadAfter(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<Result<Value>, Throwable>() {
                    @Override
                    public void accept(Result<Value> result, Throwable throwable) {
                        if (throwable != null) {
                            future.setException(throwable);
                        }

                        future.set(result);
                    }
                });
        return future;
    }

    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<Result<Value>> loadBefore(@NonNull LoadParams<Key> params) {
        final ResolvableFuture<Result<Value>> future = ResolvableFuture.create();
        onLoadBefore(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<Result<Value>, Throwable>() {
                    @Override
                    public void accept(Result<Value> result, Throwable throwable) {
                        if (throwable != null) {
                            future.setException(throwable);
                        }

                        future.set(result);
                    }
                });
        return future;
    }

    /**
     * Rx-extension of the parent method:
     * {@link ListenableItemKeyedDataSource#loadInitial(
     *ListenableItemKeyedDataSource.LoadInitialParams)}.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @return {@link Single} that receives the loaded data, its size, and any adjacent page keys.
     */
    public abstract Single<InitialResult<Value>> onLoadInitial(
            @NonNull LoadInitialParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenableItemKeyedDataSource#loadAfter(ListenableItemKeyedDataSource.LoadParams)}.
     *
     * @param params Parameters for the load, including the key to load after, and requested size.
     * @return {@link Single} that receives the loaded data.
     */
    public abstract Single<Result<Value>> onLoadAfter(@NonNull LoadParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenableItemKeyedDataSource#loadBefore(ListenableItemKeyedDataSource.LoadParams)}.
     *
     * @param params Parameters for the load, including the key to load before, and requested size.
     * @return {@link Single} that receives the loaded data.
     */
    public abstract Single<Result<Value>> onLoadBefore(@NonNull LoadParams<Key> params);
}
