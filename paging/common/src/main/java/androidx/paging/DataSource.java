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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.arch.core.util.Function;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for loading pages of snapshot data into a {@link PagedList}.
 * <p>
 * DataSource is queried to load pages of content into a {@link PagedList}. A PagedList can grow as
 * it loads more data, but the data loaded cannot be updated. If the underlying data set is
 * modified, a new PagedList / DataSource pair must be created to represent the new data.
 * <h4>Loading Pages</h4>
 * PagedList queries data from its DataSource in response to loading hints. {@link PagedListAdapter}
 * calls {@link PagedList#loadAround(int)} to load content as the user scrolls in a RecyclerView.
 * <p>
 * To control how and when a PagedList queries data from its DataSource, see
 * {@link PagedList.Config}. The Config object defines things like load sizes and prefetch distance.
 * <h4>Updating Paged Data</h4>
 * A PagedList / DataSource pair are a snapshot of the data set. A new pair of
 * PagedList / DataSource must be created if an update occurs, such as a reorder, insert, delete, or
 * content update occurs. A DataSource must detect that it cannot continue loading its
 * snapshot (for instance, when Database query notices a table being invalidated), and call
 * {@link #invalidate()}. Then a new PagedList / DataSource pair would be created to load data from
 * the new state of the Database query.
 * <p>
 * To page in data that doesn't update, you can create a single DataSource, and pass it to a single
 * PagedList. For example, loading from network when the network's paging API doesn't provide
 * updates.
 * <p>
 * To page in data from a source that does provide updates, you can create a
 * {@link DataSource.Factory}, where each DataSource created is invalidated when an update to the
 * data set occurs that makes the current snapshot invalid. For example, when paging a query from
 * the Database, and the table being queried inserts or removes items. You can also use a
 * DataSource.Factory to provide multiple versions of network-paged lists. If reloading all content
 * (e.g. in response to an action like swipe-to-refresh) is required to get a new version of data,
 * you can connect an explicit refresh signal to call {@link #invalidate()} on the current
 * DataSource.
 * <p>
 * If you have more granular update signals, such as a network API signaling an update to a single
 * item in the list, it's recommended to load data from network into memory. Then present that
 * data to the PagedList via a DataSource that wraps an in-memory snapshot. Each time the in-memory
 * copy changes, invalidate the previous DataSource, and a new one wrapping the new state of the
 * snapshot can be created.
 * <h4>Implementing a DataSource</h4>
 * To implement, extend one of the subclasses: {@link PageKeyedDataSource},
 * {@link ItemKeyedDataSource}, or {@link PositionalDataSource}.
 * <p>
 * Use {@link PageKeyedDataSource} if pages you load embed keys for loading adjacent pages. For
 * example a network response that returns some items, and a next/previous page links.
 * <p>
 * Use {@link ItemKeyedDataSource} if you need to use data from item {@code N-1} to load item
 * {@code N}. For example, if requesting the backend for the next comments in the list
 * requires the ID or timestamp of the most recent loaded comment, or if querying the next users
 * from a name-sorted database query requires the name and unique ID of the previous.
 * <p>
 * Use {@link PositionalDataSource} if you can load pages of a requested size at arbitrary
 * positions, and provide a fixed item count. PositionalDataSource supports querying pages at
 * arbitrary positions, so can provide data to PagedLists in arbitrary order. Note that
 * PositionalDataSource is required to respect page size for efficient tiling. If you want to
 * override page size (e.g. when network page size constraints are only known at runtime), use one
 * of the other DataSource classes.
 * <p>
 * Because a {@code null} item indicates a placeholder in {@link PagedList}, DataSource may not
 * return {@code null} items in lists that it loads. This is so that users of the PagedList
 * can differentiate unloaded placeholder items from content that has been paged in.
 *
 * @param <Key> Unique identifier for item loaded from DataSource. Often an integer to represent
 *             position in data set. Note - this is distinct from e.g. Room's {@code @PrimaryKey}.
 * @param <Value> Value type loaded by the DataSource.
 */
@SuppressWarnings("unused") // suppress warning to remove Key/Value, needed for subclass type safety
public abstract class DataSource<Key, Value> {
    /**
     * Factory for DataSources.
     * <p>
     * Data-loading systems of an application or library can implement this interface to allow
     * {@code LiveData<PagedList>}s to be created. For example, Room can provide a
     * DataSource.Factory for a given SQL query:
     *
     * <pre>
     * {@literal @}Dao
     * interface UserDao {
     *    {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
     *    public abstract DataSource.Factory&lt;Integer, User> usersByLastName();
     * }
     * </pre>
     * In the above sample, {@code Integer} is used because it is the {@code Key} type of
     * PositionalDataSource. Currently, Room uses the {@code LIMIT}/{@code OFFSET} SQL keywords to
     * page a large query with a PositionalDataSource.
     *
     * @param <Key> Key identifying items in DataSource.
     * @param <Value> Type of items in the list loaded by the DataSources.
     */
    public abstract static class Factory<Key, Value> {
        /**
         * Create a DataSource.
         * <p>
         * The DataSource should invalidate itself if the snapshot is no longer valid. If a
         * DataSource becomes invalid, the only way to query more data is to create a new DataSource
         * from the Factory.
         * <p>
         * {@link LivePagedListBuilder} for example will construct a new PagedList and DataSource
         * when the current DataSource is invalidated, and pass the new PagedList through the
         * {@code LiveData<PagedList>} to observers.
         *
         * @return the new DataSource.
         */
        @NonNull
        public abstract DataSource<Key, Value> create();

        /**
         * Applies the given function to each value emitted by DataSources produced by this Factory.
         * <p>
         * Same as {@link #mapByPage(Function)}, but operates on individual items.
         *
         * @param function Function that runs on each loaded item, returning items of a potentially
         *                  new type.
         * @param <ToValue> Type of items produced by the new DataSource, from the passed function.
         *
         * @return A new DataSource.Factory, which transforms items using the given function.
         *
         * @see #mapByPage(Function)
         * @see DataSource#map(Function)
         * @see DataSource#mapByPage(Function)
         */
        @NonNull
        public <ToValue> DataSource.Factory<Key, ToValue> map(
                @NonNull Function<Value, ToValue> function) {
            return mapByPage(createListFunction(function));
        }

        /**
         * Applies the given function to each value emitted by DataSources produced by this Factory.
         * <p>
         * Same as {@link #map(Function)}, but allows for batch conversions.
         *
         * @param function Function that runs on each loaded page, returning items of a potentially
         *                  new type.
         * @param <ToValue> Type of items produced by the new DataSource, from the passed function.
         *
         * @return A new DataSource.Factory, which transforms items using the given function.
         *
         * @see #map(Function)
         * @see DataSource#map(Function)
         * @see DataSource#mapByPage(Function)
         */
        @NonNull
        public <ToValue> DataSource.Factory<Key, ToValue> mapByPage(
                @NonNull final Function<List<Value>, List<ToValue>> function) {
            return new Factory<Key, ToValue>() {
                @Override
                public DataSource<Key, ToValue> create() {
                    return Factory.this.create().mapByPage(function);
                }
            };
        }
    }

    @NonNull
    static <X, Y> Function<List<X>, List<Y>> createListFunction(
            final @NonNull Function<X, Y> innerFunc) {
        return new Function<List<X>, List<Y>>() {
            @Override
            public List<Y> apply(@NonNull List<X> source) {
                List<Y> out = new ArrayList<>(source.size());
                for (int i = 0; i < source.size(); i++) {
                    out.add(innerFunc.apply(source.get(i)));
                }
                return out;
            }
        };
    }

    static <A, B> List<B> convert(Function<List<A>, List<B>> function, List<A> source) {
        List<B> dest = function.apply(source);
        if (dest.size() != source.size()) {
            throw new IllegalStateException("Invalid Function " + function
                    + " changed return size. This is not supported.");
        }
        return dest;
    }


    /**
     * Applies the given function to each value emitted by the DataSource.
     * <p>
     * Same as {@link #map(Function)}, but allows for batch conversions.
     *
     * @param function Function that runs on each loaded page, returning items of a potentially
     *                  new type.
     * @param <ToValue> Type of items produced by the new DataSource, from the passed function.
     *
     * @return A new DataSource, which transforms items using the given function.
     *
     * @see #map(Function)
     * @see DataSource.Factory#map(Function)
     * @see DataSource.Factory#mapByPage(Function)
     */
    @NonNull
    public <ToValue> DataSource<Key, ToValue> mapByPage(
            @NonNull Function<List<Value>, List<ToValue>> function) {
        return new WrapperDataSource<>(this, function);
    }

    /**
     * Applies the given function to each value emitted by the DataSource.
     * <p>
     * Same as {@link #mapByPage(Function)}, but operates on individual items.
     *
     * @param function Function that runs on each loaded item, returning items of a potentially
     *                  new type.
     * @param <ToValue> Type of items produced by the new DataSource, from the passed function.
     *
     * @return A new DataSource, which transforms items using the given function.
     *
     * @see #mapByPage(Function)
     * @see DataSource.Factory#map(Function)
     * @see DataSource.Factory#mapByPage(Function)
     */
    @NonNull
    public <ToValue> DataSource<Key, ToValue> map(
            @NonNull Function<Value, ToValue> function) {
        return mapByPage(createListFunction(function));
    }

    /**
     * Returns true if the data source guaranteed to produce a contiguous set of items,
     * never producing gaps.
     */
    boolean isContiguous() {
        return true;
    }

    boolean supportsPageDropping() {
        return true;
    }

    /**
     * Invalidation callback for DataSource.
     * <p>
     * Used to signal when a DataSource a data source has become invalid, and that a new data source
     * is needed to continue loading data.
     */
    public interface InvalidatedCallback {
        /**
         * Called when the data backing the list has become invalid. This callback is typically used
         * to signal that a new data source is needed.
         * <p>
         * This callback will be invoked on the thread that calls {@link #invalidate()}. It is valid
         * for the data source to invalidate itself during its load methods, or for an outside
         * source to invalidate it.
         */
        @AnyThread
        void onInvalidated();
    }

    private AtomicBoolean mInvalid = new AtomicBoolean(false);

    private CopyOnWriteArrayList<InvalidatedCallback> mOnInvalidatedCallbacks =
            new CopyOnWriteArrayList<>();

    /**
     * Add a callback to invoke when the DataSource is first invalidated.
     * <p>
     * Once invalidated, a data source will not become valid again.
     * <p>
     * A data source will only invoke its callbacks once - the first time {@link #invalidate()}
     * is called, on that thread.
     *
     * @param onInvalidatedCallback The callback, will be invoked on thread that
     *                              {@link #invalidate()} is called on.
     */
    @AnyThread
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        if (onInvalidatedCallback == null) {
            throw new NullPointerException();
        }
        mOnInvalidatedCallbacks.add(onInvalidatedCallback);
    }

    /**
     * Remove a previously added invalidate callback.
     *
     * @param onInvalidatedCallback The previously added callback.
     */
    @AnyThread
    public void removeInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        mOnInvalidatedCallbacks.remove(onInvalidatedCallback);
    }

    /**
     * Signal the data source to stop loading, and notify its callback.
     * <p>
     * If invalidate has already been called, this method does nothing.
     */
    @AnyThread
    public void invalidate() {
        if (mInvalid.compareAndSet(false, true)) {
            for (InvalidatedCallback callback : mOnInvalidatedCallbacks) {
                callback.onInvalidated();
            }
        }
    }

    /**
     * Returns true if the data source is invalid, and can no longer be queried for data.
     *
     * @return True if the data source is invalid, and can no longer return data.
     */
    @WorkerThread
    public boolean isInvalid() {
        return mInvalid.get();
    }

    enum LoadType {
        INITIAL,
        START,
        END
    }

    static class Params<K> {
        @NonNull
        final LoadType type;
        /* can be NULL for init, otherwise non-null */
        @Nullable
        final K key;
        final int initialLoadSize;
        final boolean placeholdersEnabled;
        final int pageSize;

        Params(@NonNull LoadType type, @Nullable K key, int initialLoadSize,
                boolean placeholdersEnabled, int pageSize) {
            this.type = type;
            this.key = key;
            this.initialLoadSize = initialLoadSize;
            this.placeholdersEnabled = placeholdersEnabled;
            this.pageSize = pageSize;
        }
    }

    // NOTE: keeping all data in base class for now, to enable public members
    // TODO: reconsider, only providing accessors to minimize subclass complexity
    static class BaseResult<Value> {
        final List<Value> data;
        final Object prevKey;
        final Object nextKey;
        final int leadingNulls;
        final int trailingNulls;
        final int offset;

        protected BaseResult(List<Value> data, Object prevKey, Object nextKey, int leadingNulls,
                int trailingNulls, int offset) {
            this.data = data;
            this.prevKey = prevKey;
            this.nextKey = nextKey;
            this.leadingNulls = leadingNulls;
            this.trailingNulls = trailingNulls;
            this.offset = offset;
            validate();
        }

        public <ToValue> BaseResult(@NonNull BaseResult<ToValue> result,
                @NonNull Function<List<ToValue>, List<Value>> function) {
            data = convert(function, result.data);
            prevKey = result.prevKey;
            nextKey = result.nextKey;
            leadingNulls = result.leadingNulls;
            trailingNulls = result.trailingNulls;
            offset = result.offset;
        }

        void validate() {
            if (leadingNulls < 0 || offset < 0) {
                throw new IllegalArgumentException("Position must be non-negative");
            }
            if (data.isEmpty() && (leadingNulls != 0 || trailingNulls != 0)) {
                throw new IllegalArgumentException("Initial result cannot be empty if items are present in data set.");
            }
            if (trailingNulls < 0) {
                throw new IllegalArgumentException(
                        "List size + position too large, last item in list beyond totalCount.");
            }
        }
    }

    enum KeyType {
        POSITIONAL,
        PAGE_KEYED,
        ITEM_KEYED,
    }

    @NonNull
    final KeyType mType;

    // Since we currently rely on implementation details of two implementations,
    // prevent external subclassing, except through exposed subclasses
    DataSource(@NonNull KeyType type) {
        mType = type;
    }

    abstract ListenableFuture<? extends BaseResult<Value>> load(@NonNull Params<Key> params);

    @Nullable
    abstract Key getKey(@NonNull Value item);

    @Nullable
    final Key getKey(int lastLoad, @Nullable Value item) {
        if (item == null) {
            return null;
        }
        if (mType == KeyType.POSITIONAL) {
            //noinspection unchecked
            return (Key)((Integer) lastLoad);
        }
        return getKey(item);
    }

    public boolean isRetryableError(@NonNull Throwable error) {
        return false;
    }

    void initExecutor(@NonNull Executor executor) {
        mExecutor = executor;
    }

    /**
     * In reality, this is null until loadInitial is called
     */
    @Nullable
    private Executor mExecutor;

    @NonNull
    public Executor getExecutor() {
        if (mExecutor == null) {
            throw new IllegalStateException("too soon, executor!");
        }
        return mExecutor;
    }
}
