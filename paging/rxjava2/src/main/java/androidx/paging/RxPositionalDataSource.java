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

abstract class RxPositionalDataSource<T> extends ListenablePositionalDataSource<T> {
    @Override
    @NonNull
    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public final ListenableFuture<InitialResult<T>> loadInitial(
            @NonNull final LoadInitialParams params) {
        final ResolvableFuture<InitialResult<T>> future = ResolvableFuture.create();
        onLoadInitial(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<InitialResult<T>, Throwable>() {
                    @Override
                    public void accept(InitialResult<T> result, Throwable throwable) {
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
    public final ListenableFuture<RangeResult<T>> loadRange(@NonNull final LoadRangeParams params) {
        final ResolvableFuture<RangeResult<T>> future = ResolvableFuture.create();
        onLoadRange(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        future.cancel(true);
                    }
                })
                .subscribe(new BiConsumer<RangeResult<T>, Throwable>() {
                    @Override
                    public void accept(RangeResult<T> result, Throwable throwable) {
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
     * {@link ListenablePositionalDataSource#loadInitial(
     *ListenablePositionalDataSource.LoadInitialParams)}.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return {@link Single} that receives the loaded data, including position and total data
     * set size.
     */
    public abstract Single<InitialResult<T>> onLoadInitial(LoadInitialParams params);

    /**
     * Rx-extension of the parent method:
     * {@link ListenablePositionalDataSource#loadRange(
     *ListenablePositionalDataSource.LoadRangeParams)}.
     *
     * @param params Parameters for load, including start position and load size.
     * @return {@link Single} that receives the loaded data.
     */
    public abstract Single<RangeResult<T>> onLoadRange(LoadRangeParams params);
}
