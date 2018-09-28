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

package androidx.lifecycle

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import java.io.Closeable
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// option 1
interface ViewModelCoroutineScope: CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() {
            if (this !is ViewModel) {
                throw IllegalStateException("You can use ViewModelCoroutineScope" +
                        " only on ViewModel objects ")
            }
            return CoroutineContext(this)
        }
}

// option 2
fun ViewModel.coroutineScope() = object : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = CoroutineContext(this@coroutineScope)
}

// option3
class ViewModelCoroutineScopeDelegate: ReadOnlyProperty<ViewModel, CoroutineScope> {
    override fun getValue(thisRef: ViewModel, property: KProperty<*>): CoroutineScope {
        return object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = CoroutineContext(thisRef)
        }
    }
}

private const val JOB_KEY = " androidx.lifecycle.ViewModelCoroutineScope.JOB_KEY"

internal fun CoroutineContext(vm: ViewModel): CoroutineContext {
    val job = vm.getTag(JOB_KEY) ?: CloseableJob().also { vm.setTag(JOB_KEY, it) }
    return job + Dispatchers.Main
}

internal class CloseableJob: Job by Job(), Closeable {
    override fun close() {
        cancel()
    }
}




