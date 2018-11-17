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

package com.example.android.livedataplusplus.lib

import androidx.lifecycle.LiveData
import com.example.android.livedataplusplus.cLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface LiveDataContext<T> {
    /**
     * Return true unless stopped
     */
    suspend fun waitUntilActive() : Boolean
    suspend fun yield(value : T)
}

internal class PausingLiveData<T>(
    private val block : suspend  LiveDataContext<T>.() -> Unit
) : LiveData<T>() {
    private val dispatcher = ManualPausingDispatcher()
    private val runContext = object : LiveDataContext<T> {
        override suspend fun yield(value: T) = withContext(Dispatchers.Main) {
            setValue(value)
            cLog("done setting value to $value")
        }

        override suspend fun waitUntilActive() = dispatcher.waitUntilRunning()
    }
    init {
        dispatcher.pause()
        GlobalScope.launch(dispatcher) {
            runContext.block()
        }
    }
    override fun onInactive() {
        dispatcher.pause()
    }

    override fun onActive() {
        dispatcher.resume()
        hasObservers()
    }

    protected fun finalize() {
        cLog("finalized live data")
    }
}

fun <T> liveData(block : suspend LiveDataContext<T>.() -> Unit) : LiveData<T> {
    return PausingLiveData(block)
}