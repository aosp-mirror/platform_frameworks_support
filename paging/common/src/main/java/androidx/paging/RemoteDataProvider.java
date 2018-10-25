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

import java.util.concurrent.Executor;

public class RemoteDataProvider<K, V> implements Pager.PageConsumer<V> {
    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;
    private final int mPageSize;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Pager<K, V> mPager;

    @Override
    public boolean onPageResult(@NonNull PagedList.LoadType type,
            @NonNull ListenableSource.BaseResult<V> pageResult) {
        return false;
    }

    @Override
    public void onStateChanged(@NonNull PagedList.LoadType type, @NonNull PagedList.LoadState state,
            @Nullable Throwable error) {
    }

    private PagedList.BoundaryCallback mBoundaryCallback = new PagedList.BoundaryCallback() {
        @Override
        public void onZeroItemsLoaded() {

        }

        @Override
        public void onItemAtFrontLoaded(@NonNull Object itemAtFront) {
            mPager.trySchedulePrepend();
        }

        @Override
        public void onItemAtEndLoaded(@NonNull Object itemAtEnd) {
            mPager.tryScheduleAppend();
        }
    };

    @SuppressWarnings("unchecked")
    public <ExternalType> PagedList.BoundaryCallback<ExternalType> getBoundaryCallback() {
        return mBoundaryCallback;
    }

    public RemoteDataProvider(
            @NonNull DataSource.Factory<K, V> dataSourceFactory,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            int pageSize) {
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mPageSize = pageSize;

        DataSource<K, V> origSource = dataSourceFactory.create();
        ContiguousDataSource<K, V> source;
        if (origSource.isContiguous()) {
            source = (ContiguousDataSource<K, V>) origSource;
        } else {
            PositionalDataSource<V> posSource = (PositionalDataSource<V>) origSource;
            //noinspection unchecked
            source = (ContiguousDataSource<K, V>) posSource.wrapAsContiguousWithoutPlaceholders();
        }
        throw new IllegalStateException("todo");
        /*
        mPager = new Pager(
                new PagedList.Config.Builder().setPageSize(pageSize).build(),
                source,
                mainThreadExecutor,
                this,
                adjacentProvider,);
        */
    }
}
