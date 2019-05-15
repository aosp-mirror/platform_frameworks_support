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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

/**
 * An {@link Enum} wrapper that implements {@link VersionedParcelable} and provides backwards and
 * forward compatibility by allowing the data producer to provide an optional set of "fallback"
 * values. When third-party navigation apps introduce a new value, they can implement
 * {@link WithFallback#getFallbacks()} to specify what alternative values should be returned if the
 * OEM cluster rendering service is using an older version of the API. Fallbacks will be applied
 * in breadth first search order.

 * @param <T> Enum type to be wrapped.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@VersionedParcelize
final class EnumWrapper<T extends Enum<T>> implements VersionedParcelable {

    /**
     * Used to declare that a certain {@link Enum} can provide fallback values.
     */
    public interface WithFallback<T extends Enum<T>> {
        /**
         * Returns the fallback values to use in case the consumer doesn't know
         */
        T[] getFallbacks();
    }

    @ParcelField(1)
    List<String> mValues = new ArrayList<>();

    /**
     * Used by {@link VersionedParcelable}
     */
    EnumWrapper() {
    }

    @SuppressWarnings("unchecked")
    EnumWrapper(@NonNull T value) {
        mValues.add(Preconditions.checkNotNull(value).name());
        if (value instanceof WithFallback) {
            Stack<WithFallback<T>> valuesWithFallback = new Stack<>();
            valuesWithFallback.push((WithFallback<T>) value);
            while (!valuesWithFallback.isEmpty()) {
                WithFallback<T> valueWithFallback = valuesWithFallback.pop();
                for (T fallback : valueWithFallback.getFallbacks()) {
                    if (fallback instanceof WithFallback) {
                        valuesWithFallback.push((WithFallback<T>) fallback);
                    }
                    mValues.add(fallback.name());
                }
            }
        }
    }

    /**
     * Returns the first value wrapped by the given {@link VersionedParcelable}, that is known to
     * this OEM cluster rendering service, or a default value if none of the fallback alternatives
     * is known, or the provided wrapper is null.
     *
     * @param defaultValue Value to return if none of the reported values is known to the OEM
     *                     cluster rendering service.
     */
    @NonNull
    public static <T extends Enum<T>> T getValue(@Nullable EnumWrapper<T> wrapper,
            @NonNull T defaultValue) {
        if (wrapper != null) {
            for (String value : wrapper.mValues) {
                T result = getEnumByName(defaultValue.getDeclaringClass(), value);
                if (result != null) {
                    return result;
                }
            }
        }
        return defaultValue;
    }

    @Nullable
    private static <T extends Enum<T>> T getEnumByName(@NonNull Class<T> clazz,
            @NonNull String name) {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnumWrapper<?> that = (EnumWrapper<?>) o;
        return Objects.equals(mValues, that.mValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mValues);
    }

    @Override
    public String toString() {
        return String.format("{values: %s}", mValues);
    }

    /**
     * Wraps the given value and an optional list of fallback values.
     *
     * @param value Value to be wrapped.
     */
    @NonNull
    public static <T extends Enum<T>> EnumWrapper<T> of(@NonNull T value) {
        return new EnumWrapper<>(Preconditions.checkNotNull(value));
    }
}
