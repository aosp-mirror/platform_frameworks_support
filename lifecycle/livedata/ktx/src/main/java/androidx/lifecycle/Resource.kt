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

import java.util.Objects

sealed class Resource<T>() {
    data class Error<T>(val error : Throwable, val data : T? = null) : Resource<T>() {
        override fun toString() = "[ERROR] error:$error data:$data"
    }
    data class Success<T>(val data : T) : Resource<T>() {
        override fun toString() = "[SUCCESS] data:$data"
    }
    data class Loading<T>(val data : T? = null, val progress : Int? = null) : Resource<T>() {
        override fun toString() = "[LOADING] data:$data progress:$progress"
    }

    companion object {
        fun <T> success(data : T) = Success(data)
        fun <T> error(error : Throwable, data: T? = null) = Error(error, data)
        fun <T> loading(data  : T? = null, progress: Int? = null) = Loading(data, progress)
    }
}