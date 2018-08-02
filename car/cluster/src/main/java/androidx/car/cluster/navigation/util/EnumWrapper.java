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

package androidx.car.cluster.navigation.util;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

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

/**
 * An {@link Enum} wrapper that implements {@link VersionedParcelable} and provides backwards and
 * forward compatibility by allowing the data producer to provide an optional set of "fallback"
 * values. If a value provided by the producer is not known by the data consumer (e.g.: a new value
 * was introduced, but the consumer still is using a older version of the API), then this
 * class would return the first "fallback" value that is known to the consumer.

 * @param <T> Enum type to be wrapped.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@VersionedParcelize
public class EnumWrapper<T extends Enum<T>> implements VersionedParcelable {
    @ParcelField(1)
    List<String> mValue = new ArrayList<>();

    /**
     * Used by {@link VersionedParcelable}
     */
    EnumWrapper() {
    }

    @SafeVarargs
    private EnumWrapper(@Nullable T value, @NonNull T ... fallbacks) {
        if (value != null) {
            mValue.add(value.name());
            for (T fallback : fallbacks) {
                mValue.add(fallback.name());
            }
        }
    }

    /**
     * Returns the first value wrapped by this {@link VersionedParcelable} that is known to this
     * consumer, or null if none of the fallback alternatives is known.
     *
     * @param clazz Class used to convert the serialized values into {@link Enum}.
     */
    public @Nullable T getValue(@NonNull Class<T> clazz) {
        for (String value : mValue) {
            T result = getEnumByName(clazz, value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumWrapper<?> that = (EnumWrapper<?>) o;
        return Objects.equals(mValue, that.mValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mValue);
    }

    /**
     * Convenient method to create a {@link EnumWrapper} of null.
     */
    public static @NonNull <T extends Enum<T>> EnumWrapper<T> of() {
        return new EnumWrapper<>();
    }

    /**
     * Wraps the given value and an optional list of fallback values.
     *
     * @param value Value to be wrapped, or null.
     * @param fallbacks An optional list of fallback values, in order of preference, to be used in
     *                  case the consumer of this API doesn't know the value provided. This will be
     *                  used only if {@code value} is not null.
     */
    @SafeVarargs @NonNull
    public static <T extends Enum<T>> EnumWrapper<T> of(@Nullable T value,
            @NonNull T ... fallbacks) {
        return new EnumWrapper<>(value, fallbacks);
    }

    /**
     * Similar to {@link #of(Enum, Enum[])}, but enforcing non-null precondition.
     */
    @SafeVarargs @NonNull
    public static <T extends Enum<T>> EnumWrapper<T> ofNonNull(@NonNull T value,
            @NonNull T ... fallbacks) {
        return new EnumWrapper<>(Preconditions.checkNotNull(value), fallbacks);
    }
}
