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

public class RemoteDataProvider<K, V> {

    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;
    private final int mPageSize;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ContiguousPager<K, V> mPager;

    class RemotePager extends ContiguousPager<K, V> {
        RemotePager(@NonNull ContiguousDataSource<K, V> dataSource,
                @NonNull Executor mainThreadExecutor,
                @NonNull Executor backgroundThreadExecutor,
                @Nullable AdjacentProvider<V> adjacentProvider,
                int pageSize) {
            super(dataSource, mainThreadExecutor, backgroundThreadExecutor, adjacentProvider,
                    pageSize);
        }

        @Override
        int onPageResult(int resultType, @NonNull PageResult<V> pageResult) {
            return 0;
        }

        @Override
        void dispatchStateChange(@NonNull PagedList.LoadType type,
                @NonNull PagedList.LoadState state, @Nullable Throwable error) {

        }

        @Override
        boolean isDetached() {
            // TODO - handle invalidation
            return false;
        }

        @Override
        void detach() {
            // TODO - handle invalidation
        }
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
        mPager = new RemotePager(source, mMainThreadExecutor, mBackgroundThreadExecutor,
                null, mPageSize);
    }
}
