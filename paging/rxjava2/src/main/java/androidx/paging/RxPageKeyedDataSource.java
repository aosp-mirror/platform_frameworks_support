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

abstract class RxPageKeyedDataSource<Key, Value> extends ListenablePageKeyedDataSource<Key, Value> {
    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<InitialResult<Key, Value>> loadInitial(
            @NonNull LoadInitialParams<Key> params) {
        final ResolvableFuture<InitialResult<Key, Value>> future = ResolvableFuture.create();
        onLoadInitial(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<InitialResult<Key, Value>, Throwable>() {
                    @Override
                    public void accept(InitialResult<Key, Value> keyValueInitialResult,
                            Throwable throwable) {
                        if (throwable != null) {
                            future.setException(throwable);
                        }

                        future.set(keyValueInitialResult);
                    }
                });
        return future;
    }

    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<Result<Key, Value>> loadAfter(@NonNull LoadParams<Key> params) {
        final ResolvableFuture<Result<Key, Value>> future = ResolvableFuture.create();
        onLoadAfter(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<Result<Key, Value>, Throwable>() {
                    @Override
                    public void accept(Result<Key, Value> keyValueInitialResult,
                            Throwable throwable) {
                        if (throwable != null) {
                            future.setException(throwable);
                        }

                        future.set(keyValueInitialResult);
                    }
                });
        return future;
    }

    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<Result<Key, Value>> loadBefore(@NonNull LoadParams<Key> params) {
        final ResolvableFuture<Result<Key, Value>> future = ResolvableFuture.create();
        onLoadBefore(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<Result<Key, Value>, Throwable>() {
                    @Override
                    public void accept(Result<Key, Value> keyValueInitialResult,
                            Throwable throwable) {
                        if (throwable != null) {
                            future.setException(throwable);
                        }

                        future.set(keyValueInitialResult);
                    }
                });
        return future;
    }

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadInitial(
     *ListenablePageKeyedDataSource.LoadInitialParams)}.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return {@link Single} that receives the loaded data, its size, and any adjacent page keys.
     */
    public abstract Single<InitialResult<Key, Value>> onLoadInitial(LoadInitialParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadBefore(ListenablePageKeyedDataSource.LoadParams)}.
     *
     * @param params Parameters for load, including page key and load size.
     * @return {@link Single} that receives the loaded data.
     */
    public abstract Single<Result<Key, Value>> onLoadBefore(LoadParams<Key> params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePageKeyedDataSource#loadAfter(ListenablePageKeyedDataSource.LoadParams)}.
     *
     * @param params Parameters for load, including page key and load size.
     * @return {@link Single} that receives the loaded data.
     */
    public abstract Single<Result<Key, Value>> onLoadAfter(LoadParams<Key> params);
}
