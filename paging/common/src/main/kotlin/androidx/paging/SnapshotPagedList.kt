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

package androidx.paging

internal class SnapshotPagedList<T : Any>(private val pagedList: PagedList<T>) : PagedList<T>(
    pagedList.storage.snapshot(),
    pagedList.mainThreadExecutor,
    pagedList.backgroundThreadExecutor,
    null,
    pagedList.getConfig()
) {
    override val lastKey: Any?

    init {
        lastLoad = pagedList.lastLoad
        lastKey = pagedList.lastKey
    }

    override fun isContiguous() = pagedList.isContiguous()

    override val dataSource: DataSource<*, T> = pagedList.dataSource
    override val isImmutable = true
    override fun isDetached() = true

    override fun detach() {}

    override fun dispatchUpdatesSinceSnapshot(snapshot: PagedList<T>, callback: Callback) {}

    override fun dispatchCurrentLoadState(listener: LoadStateListener) {}

    override fun loadAroundInternal(index: Int) {}
}
