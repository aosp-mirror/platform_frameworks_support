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

package androidx.paging.futures

import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import kotlin.test.assertFails

/**
 * Helper to unwrap the cause of an expected exception and returns a [ThrowableSubject] of the cause
 * to allow further assertions.
 *
 * E.g., [ExecutionException.cause] thrown from
 * [androidx.concurrent.futures.AbstractResolvableFuture.setException]
 */
internal inline fun <reified T : Throwable> assertFailsWithCause(
    body: () -> Unit
): ThrowableSubject {
    val exception = assertFails { body() }
    val throwableSubject = assertThat(exception.cause)
    throwableSubject.isInstanceOf(T::class.java)
    return throwableSubject
}
