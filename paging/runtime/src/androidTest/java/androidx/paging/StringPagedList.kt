/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.paging

class StringPagedList constructor(
    leadingNulls: Int,
    trailingNulls: Int,
    vararg items: String
) : PagedList<String>(
<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
        PagedStorage<String>(),
        TestExecutor(),
        TestExecutor(),
        null,
        PagedList.Config.Builder().setPageSize(1).build()
=======
    PagedStorage(),
    TestExecutor(),
    TestExecutor(),
    null,
    PagedList.Config.Builder().setPageSize(1).build()
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
), PagedStorage.Callback {
    val list = items.toList()
    init {
<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
        @Suppress("UNCHECKED_CAST")
        val keyedStorage = mStorage as PagedStorage<String>
        keyedStorage.init(leadingNulls,
                list,
                trailingNulls,
                0,
                this)
=======
        val keyedStorage = getStorage()
        keyedStorage.init(
            leadingNulls,
            list,
            trailingNulls,
            0,
            this
        )
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
    }

<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
    internal override fun isContiguous(): Boolean {
        return true
    }
=======
    override val isContiguous = true
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)

<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
    override fun getLastKey(): Any? {
        return null
=======
    override val lastKey: Any? = null

    override val isDetached
        get() = detached

    override fun detach() {
        detached = true
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
    }

<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
    override fun dispatchUpdatesSinceSnapshot(
        storageSnapshot: PagedList<String>,
        callback: PagedList.Callback
    ) {
    }
=======
    override fun dispatchUpdatesSinceSnapshot(snapshot: PagedList<String>, callback: Callback) {}
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)

<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
=======
    override fun dispatchCurrentLoadState(listener: LoadStateListener) {}

>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
    override fun loadAroundInternal(index: Int) {}

    override fun onInitialized(count: Int) {}

    override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {}

    override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {}

    override fun onEmptyPrepend() {}

    override fun onEmptyAppend() {}

    override fun onPagePlaceholderInserted(pageIndex: Int) {}

    override fun onPageInserted(start: Int, count: Int) {}

    override val dataSource = ListDataSource(list)

    override fun onPagesRemoved(startOfDrops: Int, count: Int) = notifyRemoved(startOfDrops, count)

    override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) =
        notifyChanged(startOfDrops, count)
}
