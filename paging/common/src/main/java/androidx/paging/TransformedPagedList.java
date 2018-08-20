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
import androidx.arch.core.util.Function;

import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class TransformedPagedList<T> extends PagedList<T> {

    final PagedList mSrc;
    final TransformedDataSource<T> mDataSource;

    public TransformedPagedList(PagedList src, List<Transform> transforms) {
        super(new PagedStorage<T>(), src.mMainThreadExecutor, src.mBackgroundThreadExecutor,
                null, src.mConfig);
        mSrc = src;
        mDataSource = new TransformedDataSource<T>(src.getDataSource());
    }

    int mLeadingNullCount;
    int mTrailingNullCount;
    int mStorageCount;
    List<TransformedPagedList<T>> mPages;
    @Nullable T mTrailingSeparator;
    int mSize;

    @Nullable
    @Override
    public T get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }

        int localIndex = index - mLeadingNullCount;
        if (localIndex < 0 || localIndex >= mStorageCount) {
            return null;
        }

        
    }

    @Override
    public void loadAround(int index) {
        throw new NotImplementedException();
    }

    @Override
    public int size() {
        throw new NotImplementedException();
    }

    @Override
    public int getLoadedCount() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isImmutable() {
        throw new NotImplementedException();
    }

    @NonNull
    @Override
    public List<T> snapshot() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isDetached() {
        throw new NotImplementedException();
    }

    @Override
    public void detach() {
        throw new NotImplementedException();
    }

    @Override
    public int getPositionOffset() {
        throw new NotImplementedException();
    }

    @Override
    boolean isContiguous() {
        return mSrc.isContiguous();
    }

    @NonNull
    @Override
    public DataSource<?, T> getDataSource() {
        return mDataSource;
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return mSrc.getLastKey();
    }

    @Override
    void dispatchUpdatesSinceSnapshot(@NonNull PagedList<T> snapshot, @NonNull Callback callback) {
        throw new NotImplementedException();
    }

    @Override
    void loadAroundInternal(int index) {
        throw new NotImplementedException();
    }

    public static class TransformedDataSource<T> extends DataSource<Object, T> {
        final DataSource<?, ?> mDataSource;

        public TransformedDataSource(DataSource<?, ?> dataSource) {
            mDataSource = dataSource;
        }

        // TODO: figure out how to expose this, so folks can cast to their DataSource impl
        // Note: similar problem with mapped data sources exists
        // Need to recurse
        public DataSource<?, ?> getDataSource() {
            return mDataSource;
        }

        @NonNull
        @Override
        public <ToValue> DataSource<Object, ToValue> mapByPage(
                @NonNull Function<List<T>, List<ToValue>> function) {
            throw new UnsupportedOperationException("This is a wrapper, cannot be mapped");
        }

        @NonNull
        @Override
        public <ToValue> DataSource<Object, ToValue> map(@NonNull Function<T, ToValue> function) {
            throw new UnsupportedOperationException("This is a wrapper, cannot be mapped");
        }

        @Override
        boolean isContiguous() {
            return false;
        }
    }
}
