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
 * <p>A NavArgument has a name and type, that are used to read/write it in a Bundle. It can
 * also be nullable (if the type supports it) and it can have a defaultValue.</p>
 * @param <T> the type of the data that is supported by this NavArgument
 */
public class NavArgument<T> {
    private final NavType<T> mType;
    private final boolean mIsNullable;
    private final boolean mHasDefaultValue;
    private final T mDefaultValue;
    private final String mName;

    private NavArgument(@NonNull String name,
            @NonNull NavType<T> type,
            boolean isNullable,
            T defaultValue,
            boolean hasDefaultValue) {
        if (!type.isAllowsNullable() && isNullable) {
            throw new IllegalArgumentException(type.getName() + " does not allow nullable values");
        }

        if (!isNullable && hasDefaultValue && defaultValue == null) {
            throw new IllegalArgumentException("Argument " + name
                    + " has null value but is not nullable.");
        }

        this.mName = name;
        this.mType = type;
        this.mIsNullable = isNullable;
        this.mDefaultValue = defaultValue;
        this.mHasDefaultValue = hasDefaultValue;
    }

    public boolean isHasDefaultValue() {
        return mHasDefaultValue;
    }

    public NavType<T> getType() {
        return mType;
    }

    public boolean isNullable() {
        return mIsNullable;
    }

    public T getDefaultValue() {
        return mDefaultValue;
    }

    public String getName() {
        return mName;
    }

    void putDefaultValue(Bundle bundle) {
        if (mHasDefaultValue) {
            mType.put(bundle, mName, mDefaultValue);
        }
    }

    boolean verify(@NonNull Bundle bundle) {
        if (!mIsNullable && bundle.containsKey(mName) && bundle.get(mName) == null) {
            return false;
        }
        try {
            mType.get(bundle, mName);
        } catch (ClassCastException e) {
            return false;
        }
        return true;
    }

    /**
     * A builder for constructing {@link NavArgument} instances.
     * @param <T> the type of the data that is held in NavArgument
     */
    public static class Builder<T> {
        private String mName;
        private NavType<T> mType;
        private boolean mIsNullable = false;
        private T mDefaultValue;
        private boolean mHasDefaultValue = false;

        /**
         * Set the name of the argument
         * @param name
         * @return
         */
        public Builder setName(@NonNull String name) {
            mName = name;
            return this;
        }

        /**
         * Set the type of the argument
         * @param type
         * @return
         */
        public Builder setType(@NonNull NavType<T> type) {
            mType = type;
            return this;
        }

        /**
         * Specify if the argument is nullable
         * @param isNullable
         * @return
         */
        public Builder setIsNullable(boolean isNullable) {
            mIsNullable = isNullable;
            return this;
        }

        /**
         * Specify the default value for an argument. Calling this at least once will cause the
         * argument to have a default value, even if that default value is null.
         * @param defaultValue
         * @return
         */
        public Builder setDefaultValue(@Nullable T defaultValue) {
            mDefaultValue = defaultValue;
            mHasDefaultValue = true;
            return this;
        }

        /**
         * Build the NavArgument specified by this builder.
         * @return
         */
        public NavArgument<T> build() {
            if (mName == null || mType == null) {
                throw new IllegalStateException("Name and type are required for NavArgument.");
            }
            return new NavArgument<T>(mName, mType, mIsNullable, mDefaultValue, mHasDefaultValue);
        }
    }
}
