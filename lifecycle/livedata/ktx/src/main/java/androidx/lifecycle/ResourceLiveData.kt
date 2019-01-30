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

package androidx.lifecycle

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * DANGER
 * DANGER
 * DANGER
 * DANGER
 * DANGER
 * DANGER
 * This is probably a bad idea. just experimenting
 */
@UseExperimental(ExperimentalTypeInference::class)
@JvmName("doNotUse")
fun <T, R> LiveData<Resource<T>>.switchMap(
    context: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
) : LiveData<Resource<R>> {
    return switchMapLaunch(context, createResultUnwrapper(block))
}

@UseExperimental(ExperimentalTypeInference::class)
@JvmName("doNotUse2")
fun <T, R> LiveData<Resource<T>>.flatMap(
    context: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
) : LiveData<Resource<R>> {
    // LiveDataScope<R>.(T) -> Unit
    return flatMapLaunch(context, this.createResultUnwrapper(block) )
}


private fun <T, R> LiveData<Resource<T>>.createResultUnwrapper(
    block: suspend LiveDataScope<R>.(T) -> Unit
) : suspend LiveDataScope<Resource<R>>.(Resource<T>) -> Unit {
    val f : suspend LiveDataScope<Resource<R>>.(Resource<T>)->Unit = { input ->
        val resultScope = this
        val inputData = when(input) {
            is Resource.Success -> input.data
            is Resource.Loading -> input.data
            else -> null
        }
        if (inputData != null) {
            val res = runCatching {
                block(object : LiveDataScope<R> {
                    override suspend fun yield(out: R) {
                        resultScope.yield(Resource.success(out))
                    }
                }, inputData)
            }
            if (res.isFailure) {
                resultScope.yield(Resource.error(res.exceptionOrNull()!!))
            }
        } else {
            val next : Resource<R> = when(input) {
                is Resource.Loading -> Resource.loading(null, input.progress)
                is Resource.Error -> Resource.error<R>(input.error)
                // TODO what to do w/ nulls , how about a nullable transformation?
                // TODO should we recommend using Unit for those cases?
                else -> Resource.error<R>(IllegalArgumentException("Success input cannot be null"))
            }
            resultScope.yield(next)
        }
    }
    return f
}

inline fun <T> LiveData<T>.toResource() : LiveData<Resource<T>> = this.map {
    Resource.success(it)
}