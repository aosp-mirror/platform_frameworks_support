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

import androidx.arch.core.util.Function
import androidx.paging.futures.DirectExecutor
import androidx.paging.futures.Futures

import com.google.common.util.concurrent.ListenableFuture

import java.util.IdentityHashMap

/**
 * @param <Key>       DataSource key type, same for original and wrapped.
 * @param <ValueFrom> Value type of original DataSource.
 * @param <ValueTo>   Value type of new DataSource.
</ValueTo></ValueFrom></Key> */
internal open class WrapperDataSource<Key, ValueFrom, ValueTo>(
    private val mSource: DataSource<Key, ValueFrom>,
    val mListFunction: (List<ValueFrom>) -> List<ValueTo> /* synthetic access */
) : DataSource<Key, ValueTo>(mSource.type) {


    private val mKeyMap: IdentityHashMap<ValueTo, Key>?

    init {
        mKeyMap = if (mSource.type === KeyType.ITEM_KEYED)
            IdentityHashMap()
        else
            null
    }

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        mSource.addInvalidatedCallback(onInvalidatedCallback)
    }

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        mSource.removeInvalidatedCallback(onInvalidatedCallback)
    }

    override fun invalidate() {
        mSource.invalidate()
    }

    override fun isInvalid() = mSource.isInvalid()

    override fun getKey(item: ValueTo): Key? {
        if (mKeyMap != null) {
            synchronized(mKeyMap) {
                return mKeyMap[item]
            }
        }
        // positional / page-keyed
        return null
    }

    fun stashKeysIfNeeded(source: List<ValueFrom>, dest: List<ValueTo>) {
        if (mKeyMap != null) {
            synchronized(mKeyMap) {
                for (i in dest.indices) {
                    mKeyMap[dest[i]] = mSource.getKey(source[i])
                }
            }
        }
    }/* synthetic access */

    override fun load(params: Params<Key>): ListenableFuture<out BaseResult<ValueTo>> {
        return Futures.transform(
            mSource.load(params),
            Function { input ->
                val result = BaseResult.convert(input, mListFunction)
                stashKeysIfNeeded(input.data, result.data)
                result
            },
            DirectExecutor.INSTANCE
        )
    }
}
