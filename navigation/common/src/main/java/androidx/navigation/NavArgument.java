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
 */
public final class NavArgument {
    @NonNull private final NavType mType;
    private final boolean mIsNullable;
    private final boolean mHasDefaultValue;
    @Nullable private final Object mDefaultValue;
    @NonNull private final String mName;

    NavArgument(@NonNull String name,
            @NonNull NavType<?> type,
            boolean isNullable,
            @Nullable Object defaultValue,
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

    @NonNull
    public String getName() {
        return mName;
    }

    @SuppressWarnings("unchecked")
    void putDefaultValue(@NonNull Bundle bundle) {
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
     */
    public static final class Builder {
        @NonNull private String mName;
        @Nullable private NavType<?> mType;
        private boolean mIsNullable = false;
        @Nullable private Object mDefaultValue;
        private boolean mHasDefaultValue = false;

        public Builder(@NonNull String name) {
            mName = name;
        }

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
                if (mHasDefaultValue && mDefaultValue != null) {
                    mType = NavType.inferFromValueType(mDefaultValue);
                } else {
                    mType = NavType.UnspecifiedType;
                }
            }
            return new NavArgument(mName, mType, mIsNullable, mDefaultValue, mHasDefaultValue);
        }
    }
}
