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
import androidx.arch.core.util.Function;
import androidx.paging.futures.DirectExecutor;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.IdentityHashMap;
import java.util.List;

class WrapperDataSource<K, A, B> extends DataSource<K, B> {
    private final DataSource<K, A> mSource;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Function<List<A>, List<B>> mListFunction;


    private final IdentityHashMap<B, K> mKeyMap;

    WrapperDataSource(DataSource<K, A> source, Function<List<A>, List<B>> listFunction) {
        super(source.mType);
        mSource = source;
        mListFunction = listFunction;
        mKeyMap = source.mType == KeyType.ITEM_KEYED ? new IdentityHashMap<B, K>() : null;
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

    @Nullable
    @Override
    K getKey(@NonNull B item) {
        if (mKeyMap != null) {
            synchronized (mKeyMap) {
                return mKeyMap.get(item);
            }
        }
        // positional / page-keyed
        return null;
    }

    void stashKeysIfNeeded(@NonNull List<A> source, @NonNull List<B> dest) {
        if (mKeyMap != null) {
            synchronized (mKeyMap) {
                for (int i = 0; i < dest.size(); i++) {
                    mKeyMap.put(dest.get(i), mSource.getKey(source.get(i)));
                }
            }
        }
    }

    @Override
    ListenableFuture<? extends BaseResult> load(@NonNull Params params) {
        //noinspection unchecked
        return Futures.transform(
                mSource.load(params),
                new Function<BaseResult<A>, BaseResult<B>>() {
                    @Override
                    public BaseResult<B> apply(BaseResult<A> input) {
                        BaseResult<B> result = new BaseResult<>(input, mListFunction);
                        stashKeysIfNeeded(input.data, result.data);
                        return result;
                    }
                },
                DirectExecutor.INSTANCE);
    }
}
