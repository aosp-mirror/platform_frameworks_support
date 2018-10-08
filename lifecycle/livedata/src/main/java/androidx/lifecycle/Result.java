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

package androidx.lifecycle;

import androidx.annotation.Nullable;

/**
 * Base class to declare an abstract result.
 * // TODO find a better name and make a proper API by checking what other libraries do.
 * // TODO maybe consider androidx.Optional for error vs data ?
 */
public class Result<T> {
    Result(){

    }

    public final boolean isSuccess() {
        return this instanceof Succes;
    }

    public final boolean isFailure() {
        return this instanceof Failure;
    }

    @Nullable
    public T getValueIfSuccess() {
        if (this instanceof Succes) {
            return ((Succes<T>) this).value;
        }
        return null;
    }

    @Nullable
    public Throwable getExceptionIfFailure() {
        if (this instanceof Failure) {
            return ((Failure<T>) this).throwable;
        }
        return null;
    }

    static class Succes<T> extends Result<T> {
        public final T value;

        Succes(T value) {
            this.value = value;
        }
    }

    static class Failure<T> extends Result<T> {
        public final Throwable throwable;

        Failure(Throwable throwable) {
            this.throwable = throwable;
        }
    }

    public static <T> Result<T> success(T data) {
        return new Succes<T>(data);
    }

    public static <T> Result<T> failure(Throwable throwable) {
        return new Failure<T>(throwable);
    }
}
