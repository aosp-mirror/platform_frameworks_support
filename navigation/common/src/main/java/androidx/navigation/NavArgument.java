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

package androidx.navigation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * NavArgument denotes an argument that is supported by a {@link NavDestination}.
 *
 * <p>A NavArgument has a type and optionally default Value, that are used to read/write
 * it in a Bundle. It can also be nullable if the type supports it. </p>
 */
public final class NavArgument {
    @NonNull private final NavType mType;
    private final boolean mIsNullable;
    private final boolean mHasDefaultValue;
    @Nullable private final Object mDefaultValue;

    NavArgument(@NonNull NavType<?> type,
            boolean isNullable,
            @Nullable Object defaultValue,
            boolean hasDefaultValue) {
        if (!type.isAllowsNullable() && isNullable) {
            throw new IllegalArgumentException(type.getName() + " does not allow nullable values");
        }

        if (!isNullable && hasDefaultValue && defaultValue == null) {
            throw new IllegalArgumentException("Argument with type " + type.getName()
                    + " has null value but is not nullable.");
        }

        this.mType = type;
        this.mIsNullable = isNullable;
        this.mDefaultValue = defaultValue;
        this.mHasDefaultValue = hasDefaultValue;
    }

    public boolean isHasDefaultValue() {
        return mHasDefaultValue;
    }

    @NonNull
    public NavType<?> getType() {
        return mType;
    }

    public boolean isNullable() {
        return mIsNullable;
    }

    @Nullable
    public Object getDefaultValue() {
        return mDefaultValue;
    }

    @SuppressWarnings("unchecked")
    void putDefaultValue(@NonNull String name, @NonNull Bundle bundle) {
        if (mHasDefaultValue) {
            mType.put(bundle, name, mDefaultValue);
        }
    }

    boolean verify(@NonNull String name, @NonNull Bundle bundle) {
        if (!mIsNullable && bundle.containsKey(name) && bundle.get(name) == null) {
            return false;
        }
        try {
            mType.get(bundle, name);
        } catch (ClassCastException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NavArgument that = (NavArgument) o;

        if (mIsNullable != that.mIsNullable) return false;
        if (mHasDefaultValue != that.mHasDefaultValue) return false;
        if (!mType.equals(that.mType)) return false;
        return mDefaultValue != null ? mDefaultValue.equals(that.mDefaultValue)
                : that.mDefaultValue == null;
    }

    @Override
    public int hashCode() {
        int result = mType.hashCode();
        result = 31 * result + (mIsNullable ? 1 : 0);
        result = 31 * result + (mHasDefaultValue ? 1 : 0);
        result = 31 * result + (mDefaultValue != null ? mDefaultValue.hashCode() : 0);
        return result;
    }

    /**
     * A builder for constructing {@link NavArgument} instances.
     */
    public static final class Builder {
        @Nullable private NavType<?> mType;
        private boolean mIsNullable = false;
        @Nullable private Object mDefaultValue;
        private boolean mHasDefaultValue = false;

        /**
         * Set the type of the argument.
         * @param type Type of the argument.
         * @return This builder.
         */
        @NonNull
        public Builder setType(@NonNull NavType<?> type) {
            mType = type;
            return this;
        }

        /**
         * Specify if the argument is nullable.
         * @param isNullable Argument will be nullable if true.
         * @return This builder.
         */
        @NonNull
        public Builder setIsNullable(boolean isNullable) {
            mIsNullable = isNullable;
            return this;
        }

        /**
         * Specify the default value for an argument. Calling this at least once will cause the
         * argument to have a default value, even if it is set to null.
         * @param defaultValue Default value for this argument.
         *                     Must match NavType if it is specified.
         * @return This builder.
         */
        @NonNull
        public Builder setDefaultValue(@Nullable Object defaultValue) {
            mDefaultValue = defaultValue;
            mHasDefaultValue = true;
            return this;
        }

        /**
         * Build the NavArgument specified by this builder.
         * If the type is not set, the builder will infer the type from the default argument value.
         * If there is no default value, the type will be unspecified.
         * @return the newly constructed NavArgument.
         */
        @NonNull
        public NavArgument build() {
            if (mType == null) {
                mType = NavType.inferFromValueType(mDefaultValue);
            }
            return new NavArgument(mType, mIsNullable, mDefaultValue, mHasDefaultValue);
        }
    }
}
