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

import static androidx.paging.PagedList.LoadState.DONE;
import static androidx.paging.PagedList.LoadState.IDLE;
import static androidx.paging.PagedList.LoadState.RETRYABLE_ERROR;
import static androidx.paging.PagedList.LoadType.END;
import static androidx.paging.PagedList.LoadType.START;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

abstract class ContiguousPager<K, V> {

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final ContiguousDataSource<K, V> mDataSource;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final Executor mMainThreadExecutor;

    @NonNull
    private final Executor mBackgroundThreadExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final AdjacentProvider<V> mAdjacentProvider;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final int mPageSize;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final PagedList.LoadStateManager mLoadStateManager = new PagedList.LoadStateManager() {
        @Override
        protected void onStateChanged(
                @NonNull final PagedList.LoadType type,
                @NonNull final PagedList.LoadState state,
                @Nullable final Throwable error) {
            // new state, dispatch to listeners
            // Post, since UI will want to react immediately
            mMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    dispatchStateChange(type, state, error);
                }
            });
        }
    };

    static final int STORED = 1;
    static final int FETCH_MORE = 1<<1;

    ContiguousPager(
            @NonNull ContiguousDataSource<K, V> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @Nullable AdjacentProvider<V> adjacentProvider,
            int pageSize) {
        mDataSource = dataSource;
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        if (adjacentProvider == null) {
            adjacentProvider = new SimpleAdjacentProvider<>();
        }
        mAdjacentProvider = adjacentProvider;
        mPageSize = pageSize;
    }

    @IntDef(flag=true, value = {
            STORED,
            FETCH_MORE
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface PageResultResolution{}

    PageResult.Receiver<V> mReceiver = new PageResult.Receiver<V>() {
        @Override
        public void onPageResult(
                @PageResult.ResultType int resultType,
                @NonNull PageResult<V> pageResult) {
            if (pageResult.isInvalid()) {
                detach();
                return;
            }

            if (isDetached()) {
                // No op, have detached
                return;
            }

            @PageResultResolution int resolution =
                    ContiguousPager.this.onPageResult(resultType, pageResult);
            mAdjacentProvider.onPageResultResolution(resolution, resultType, pageResult);
            if ((resolution & FETCH_MORE) != 0) {
                if (resultType == PageResult.APPEND) {
                    scheduleAppend();
                } else if (resultType == PageResult.PREPEND) {
                    schedulePrepend();
                } else {
                    throw new IllegalStateException("Can only fetch more during append/prepend");
                }
            } else {
                PagedList.LoadState state = pageResult.page.isEmpty() ? DONE : IDLE;
                if (resultType == PageResult.PREPEND) {
                    mLoadStateManager.setState(START, state, null);
                } else if (resultType == PageResult.APPEND) {
                    mLoadStateManager.setState(END, state, null);
                } else {
                    // TODO: pass init signal through to *previous* list
                }
            }
        }

        @Override
        public void onPageError(@PageResult.ResultType int resultType,
                @NonNull Throwable error, boolean retryable) {
            PagedList.LoadState errorState =
                    retryable ? RETRYABLE_ERROR : PagedList.LoadState.ERROR;
            if (resultType == PageResult.PREPEND) {
                mLoadStateManager.setState(START, errorState, error);
            } else if (resultType == PageResult.APPEND) {
                mLoadStateManager.setState(END, errorState, error);
            } else {
                // TODO: pass init signal through to *previous* list
                throw new IllegalStateException("TODO: Initial load error support");
            }
        }
    };

    void trySchedulePrepend() {
        if (mLoadStateManager.getStart() == IDLE) {
            schedulePrepend();
        }
    }
    void tryScheduleAppend() {
        if (mLoadStateManager.getEnd() == IDLE) {
            scheduleAppend();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void schedulePrepend() {
        mLoadStateManager.setState(START, PagedList.LoadState.LOADING, null);

        final int position = mAdjacentProvider.getFirstLoadedItemIndex();
        final V item = mAdjacentProvider.getFirstLoadedItem();
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (isDetached()) {
                    return;
                }
                if (mDataSource.isInvalid()) {
                    detach();
                } else {
                    mDataSource.dispatchLoadBefore(position, item, mPageSize,
                            mMainThreadExecutor, mReceiver);
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void scheduleAppend() {
        mLoadStateManager.setState(END,
                PagedList.LoadState.LOADING, null);

        final int position = mAdjacentProvider.getLastLoadedItemIndex();
        final V item = mAdjacentProvider.getLastLoadedItem();
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (isDetached()) {
                    return;
                }
                if (mDataSource.isInvalid()) {
                    detach();
                } else {
                    mDataSource.dispatchLoadAfter(position, item, mPageSize,
                            mMainThreadExecutor, mReceiver);
                }
            }
        });
    }

    void retry() {
        if (mLoadStateManager.getStart() == RETRYABLE_ERROR) {
            schedulePrepend();
        }
        if (mLoadStateManager.getEnd() == RETRYABLE_ERROR) {
            scheduleAppend();
        }
    }

    abstract @PageResultResolution int onPageResult(
            @PageResult.ResultType int resultType,
            @NonNull PageResult<V> pageResult);

    /**
     * Provides next index and next item used to load adjacent pages.
     *
     * @see SimpleAdjacentProvider
     */
    interface AdjacentProvider<V> {
        V getFirstLoadedItem();

        V getLastLoadedItem();

        int getFirstLoadedItemIndex();

        int getLastLoadedItemIndex();

        void onPageResultResolution(
                @PageResultResolution int resolution,
                @PageResult.ResultType int resultType,
                @NonNull PageResult<V> pageResult);
    }

    /**
     * Default provider assumes no page dropping, so the most recent item/index loaded inform
     * the next load in either direction.
     */
    static class SimpleAdjacentProvider<V> implements AdjacentProvider<V> {
        private int mFirstIndex;
        private int mLastIndex;

        private V mFirstItem;
        private V mLastItem;

        @Override
        public V getFirstLoadedItem() {
            return mFirstItem;
        }

        @Override
        public V getLastLoadedItem() {
            return mLastItem;
        }

        @Override
        public int getFirstLoadedItemIndex() {
            return mFirstIndex;
        }

        @Override
        public int getLastLoadedItemIndex() {
            return mLastIndex;
        }

        @Override
        public void onPageResultResolution(int resolution, int resultType,
                @NonNull PageResult<V> pageResult) {
            if (resultType == PageResult.PREPEND) {
                mFirstIndex -= pageResult.page.size();
                mFirstItem = pageResult.page.get(0);
            } else if (resultType == PageResult.APPEND) {
                mLastIndex += pageResult.page.size();
                mLastItem = pageResult.page.get(pageResult.page.size() - 1);
            } else {
                mFirstIndex = pageResult.leadingNulls;
                mLastIndex = pageResult.leadingNulls + pageResult.page.size();
                mFirstItem = pageResult.page.get(0);
                mLastItem = pageResult.page.get(pageResult.page.size() - 1);
            }
        }
    }


    abstract void dispatchStateChange(
            @NonNull PagedList.LoadType type,
            @NonNull PagedList.LoadState state,
            @Nullable Throwable error);

    abstract boolean isDetached();
    abstract void detach();
}
