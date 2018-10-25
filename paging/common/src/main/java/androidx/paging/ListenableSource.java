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

abstract class ListenableSource<K, V> {
    enum LoadType {
        INITIAL,
        START,
        END,
        // TODO: only used for positional. could use 'inner' :P
        TILE,
    }

    static class Params<K> {
        final @NonNull LoadType type;
        // TODO: can be NULL for init, otherwise non-null :P
        final K key;
        final int requestedLoadSize;
        final boolean placeholdersEnabled;

        // TODO: only needed for positional initial load :P
        final int pageSize;

        Params(@NonNull LoadType type, K key, int requestedLoadSize, boolean placeholdersEnabled,
                int pageSize) {
            this.type = type;
            this.key = key;
            this.requestedLoadSize = requestedLoadSize;
            this.placeholdersEnabled = placeholdersEnabled;
            this.pageSize = pageSize;
        }
    }

    // NOTE: keeping all data in subclasses for now, to enable public members
    // TODO: reconsider, only providing accessors to minimize subclass complexity
    static abstract class BaseResult<V> {
        final List<V> mData;
        final Object mNextKey;
        final Object mPrevKey;
        final int mLeadingNulls;
        final int mTrailingNulls;
        final int mOffset;

        protected BaseResult(List<V> data, Object nextKey, Object prevKey, int leadingNulls,
                int trailingNulls, int offset) {
            mData = data;
            mNextKey = nextKey;
            mPrevKey = prevKey;
            mLeadingNulls = leadingNulls;
            mTrailingNulls = trailingNulls;
            mOffset = offset;
        }
    }

    enum KeyType {
        // TODO: PAGE_INDEX?,
        POSITIONAL,
        PAGE_KEYED,
        ITEM_KEYED,
    }

    @NonNull
    final KeyType mType;

    ListenableSource(@NonNull KeyType type) {
        mType = type;
    }

    abstract ListenableFuture<? extends BaseResult<V>> load(Params<K> params);

    @Nullable
    abstract K getKey(@NonNull V item);

    boolean isRetryableError(@NonNull Throwable error) {
        return false;
    }
}
