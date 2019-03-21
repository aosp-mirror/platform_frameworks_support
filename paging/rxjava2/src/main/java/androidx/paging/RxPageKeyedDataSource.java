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

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

abstract class RxPageKeyedDataSource<Key, Value> extends PageKeyedDataSource<Key, Value> {
    public static class LoadInitialResponse<Key, Value> {
        @NonNull
        @SuppressWarnings("WeakerAccess")
        public final List<Value> result;
        @SuppressWarnings("WeakerAccess")
        public final int position;
        @Nullable
        @SuppressWarnings("WeakerAccess")
        public final Key previousPageKey;
        @Nullable
        @SuppressWarnings("WeakerAccess")
        public final Key nextPageKey;

        public LoadInitialResponse(
                @NonNull List<Value> result,
                int position,
                @Nullable Key previousPageKey,
                @Nullable Key nextPageKey) {
            this.result = result;
            this.position = position;
            this.previousPageKey = previousPageKey;
            this.nextPageKey = nextPageKey;
        }
    }

    public static class LoadBeforeResponse<Key, Value> {
        @NonNull
        public final List<Value> data;
        @Nullable
        @SuppressWarnings("WeakerAccess")
        public final Key adjacentPageKey;

        public LoadBeforeResponse(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
            this.data = data;
            this.adjacentPageKey = adjacentPageKey;
        }
    }

    public static class LoadAfterResponse<Key, Value> {
        @NonNull
        public final List<Value> data;
        @Nullable
        @SuppressWarnings("WeakerAccess")
        public final Key adjacentPageKey;

        public LoadAfterResponse(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
            this.data = data;
            this.adjacentPageKey = adjacentPageKey;
        }
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Key> params,
            @NonNull final LoadInitialCallback<Key, Value> callback
    ) {
        loadInitial(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .subscribe(new SingleObserver<LoadInitialResponse<Key, Value>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // Ignored
                    }

                    @Override
                    public void onSuccess(LoadInitialResponse<Key, Value> response) {
                        callback.onResult(
                                response.result,
                                response.position,
                                response.result.size(),
                                response.previousPageKey,
                                response.nextPageKey
                        );
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onError(e);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Key> params,
            @NonNull final LoadCallback<Key, Value> callback
    ) {
        loadBefore(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .subscribe(new SingleObserver<LoadBeforeResponse<Key, Value>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // Ignored
                    }

                    @Override
                    public void onSuccess(LoadBeforeResponse<Key, Value> response) {
                        callback.onResult(response.data, response.adjacentPageKey);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onError(e);
                    }
                });
    }

    @Override
    public void loadAfter(
            @NonNull LoadParams<Key> params,
            @NonNull final LoadCallback<Key, Value> callback
    ) {
        loadAfter(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .subscribe(new SingleObserver<LoadAfterResponse<Key, Value>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(LoadAfterResponse<Key, Value> response) {
                        callback.onResult(response.data, response.adjacentPageKey);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onError(e);
                    }
                });
    }

    public abstract Single<LoadInitialResponse<Key, Value>> loadInitial(
            LoadInitialParams<Key> params);

    public abstract Single<LoadBeforeResponse<Key, Value>> loadBefore(LoadParams<Key> params);

    public abstract Single<LoadAfterResponse<Key, Value>> loadAfter(LoadParams<Key> params);
}
