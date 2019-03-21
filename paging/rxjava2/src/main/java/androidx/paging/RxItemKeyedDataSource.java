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

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

abstract class RxItemKeyedDataSource<Key, Value> extends ItemKeyedDataSource<Key, Value> {
    public static class LoadInitialResponse<Value> {
        @NonNull
        public final List<Value> data;
        @SuppressWarnings("WeakerAccess")
        public final int position;

        public LoadInitialResponse(@NonNull List<Value> data, int position) {
            this.data = data;
            this.position = position;
        }
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Key> params,
            @NonNull final LoadInitialCallback<Value> callback
    ) {
        loadInitial(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .subscribe(new SingleObserver<LoadInitialResponse<Value>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // Ignored
                    }

                    @Override
                    public void onSuccess(LoadInitialResponse<Value> response) {
                        callback.onResult(response.data, response.position, response.data.size());
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
            @NonNull final LoadCallback<Value> callback
    ) {
        loadAfter(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .subscribe(new SingleObserver<List<Value>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // Ignored
                    }

                    @Override
                    public void onSuccess(List<Value> data) {
                        callback.onResult(data);
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
            @NonNull final LoadCallback<Value> callback
    ) {
        loadBefore(params)
                .subscribeOn(Schedulers.from(getExecutor()))
                .subscribe(new SingleObserver<List<Value>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(List<Value> data) {
                        callback.onResult(data);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onError(e);
                    }
                });
    }

    public abstract Single<LoadInitialResponse<Value>> loadInitial(LoadInitialParams<Key> params);

    public abstract Single<List<Value>> loadAfter(LoadParams<Key> params);

    public abstract Single<List<Value>> loadBefore(LoadParams<Key> params);
}
