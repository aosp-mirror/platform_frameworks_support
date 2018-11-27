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
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * NavType denotes the type that can be used in a {@link NavArgument}.
 *
 * <p>There are built-in NavTypes for primitive types, such as int, long, boolean, float, and
 * strings, parcelable/serializable classes, as well as arrays of each supported type.</p>
 * <p>You should normally use one of the static NavType instances defined in this class.</p>
 *
 * @param <T> the type of the data that is supported by this NavType
 */
public abstract class NavType<T> {
    private final boolean mNullableAllowed;

    NavType(boolean nullableAllowed) {
        this.mNullableAllowed = nullableAllowed;
    }

    /**
     * Check if an argument with this type can hold a `null` value.
     * @return Returns `true` if this type allows `null` values, false otherwise.
     */
    public boolean isNullableAllowed() {
        return mNullableAllowed;
    }

    /**
     * Put a value of this type in he {@code bundle}
     *
     * @param bundle bundle to put value in
     * @param key    bundle key
     * @param value  value of this type
     */
    public abstract void put(@NonNull Bundle bundle, @NonNull String key, @Nullable T value);

    /**
     * Get a value of this type from the {@code bundle}
     *
     * @param bundle bundle to get value from
     * @param key    bundle key
     * @return value of this type
     */
    @Nullable
    public abstract T get(@NonNull Bundle bundle, @NonNull String key);

    /**
     * Parse a value of this type from a String
     *
     * @param value string representation of a value of this type
     */
    @Nullable
    public abstract T parseValue(@NonNull String value);

    /**
     * Parse a value of this type from a String and put it in a {@code bundle}
     *
     * @param bundle bundle to put value in
     * @param key    bundle key under which to put the value
     * @param value  parsed value
     */
    @Nullable
    T parseAndPut(@NonNull Bundle bundle, @NonNull String key, @NonNull String value) {
        T parsedValue = parseValue(value);
        put(bundle, key, parsedValue);
        return parsedValue;
    }

    /**
     * Returns the name of this type
     *
     * @return name of this type
     */
    @NonNull
    public abstract String getName();

    @Override
    @NonNull
    public String toString() {
        return getName();
    }

    /**
     * Parse an argType string into a NavType
     *
     * @param type        argType string, usually parsed from the navigation XML file
     * @param packageName package name of the R file,
     *                    used for parsing relative class names starting with a dot.
     * @return a NavType representing the type indicated by the argType string
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static NavType<?> fromArgType(@Nullable String type, @Nullable String packageName) {
        if (IntType.getName().equals(type) || "reference".equals(type)) {
            return IntType;
        } else if (IntArrayType.getName().equals(type)) {
            return IntArrayType;
        } else if (LongType.getName().equals(type)) {
            return LongType;
        } else if (LongArrayType.getName().equals(type)) {
            return LongArrayType;
        } else if (BoolType.getName().equals(type)) {
            return BoolType;
        } else if (BoolArrayType.getName().equals(type)) {
            return BoolArrayType;
        } else if (StringType.getName().equals(type)) {
            return StringType;
        } else if (StringArrayType.getName().equals(type)) {
            return StringArrayType;
        } else if (FloatType.getName().equals(type)) {
            return FloatType;
        } else if (FloatArrayType.getName().equals(type)) {
            return FloatArrayType;
        } else if (type != null && !type.isEmpty()) {
            try {
                String className;
                if (type.startsWith(".") && packageName != null) {
                    className = packageName + type;
                } else {
                    className = type;
                }

                if (type.endsWith("[]")) {
                    className = className.substring(0, className.length() - 2);
                    return new ObjectArrayType(Class.forName("[L" + className + ";"));
                } else {
                    return new ObjectType(Class.forName(className));
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return StringType;
    }

    //TODO: make identical to safeargs version
    @NonNull
    static NavType inferFromValue(@NonNull String value) {
        Integer intValue = IntType.parseValue(value);
        if (intValue != null) {
            return IntType;
        }
        Long longValue = LongType.parseValue(value);
        if (longValue != null) {
            return LongType;
        }
        Float floatValue = FloatType.parseValue(value);
        if (floatValue != null) {
            return FloatType;
        }
        Boolean boolValue = BoolType.parseValue(value);
        if (boolValue != null) {
            return BoolType;
        }
        return StringType;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    static NavType inferFromValueType(@Nullable Object value) {
        if (value instanceof Integer) {
            return IntType;
        } else if (value instanceof int[]) {
            return IntArrayType;
        } else if (value instanceof Long) {
            return LongType;
        } else if (value instanceof long[]) {
            return LongArrayType;
        } else if (value instanceof Float) {
            return FloatType;
        } else if (value instanceof float[]) {
            return FloatArrayType;
        } else if (value instanceof Boolean) {
            return BoolType;
        } else if (value instanceof boolean[]) {
            return BoolArrayType;
        } else if (value instanceof String) {
            return StringType;
        } else if (value instanceof String[]) {
            return StringArrayType;
        } else if (value != null && value.getClass().isArray() && (
                Parcelable.class.isAssignableFrom(value.getClass().getComponentType())
                        || Serializable.class.isAssignableFrom(value.getClass().getComponentType())
            )) {
            return new ObjectArrayType(value.getClass());
        } else if (value instanceof Parcelable || value instanceof Serializable) {
            return new ObjectType(value.getClass());
        }
        return StringType;
    }

    //TODO: make identical to safeargs version
    public static final NavType<Integer> IntType = new NavType<Integer>(false) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, Integer value) {
            bundle.putInt(key, value);
        }

        @Override
        public Integer get(@NonNull Bundle bundle, @NonNull String key) {
            return (Integer) bundle.get(key);
        }

        @Override
        public Integer parseValue(@NonNull String value) {
            try {
                if (value.startsWith("0x")) {
                    return Integer.parseInt(value.substring(2), 16);
                } else {
                    return Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @NonNull
        @Override
        public String getName() {
            return "integer";
        }
    };

    public static final NavType<int[]> IntArrayType = new NavType<int[]>(true) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, int[] value) {
            bundle.putIntArray(key, value);
        }

        @Override
        public int[] get(@NonNull Bundle bundle, @NonNull String key) {
            return (int[]) bundle.get(key);
        }

        @Override
        public int[] parseValue(@NonNull String value) {
            throw new UnsupportedOperationException("Arrays don't support string values.");
        }

        @NonNull
        @Override
        public String getName() {
            return "integer[]";
        }
    };

    //TODO: make identical to safeargs version
    public static final NavType<Long> LongType = new NavType<Long>(false) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, Long value) {
            bundle.putLong(key, value);
        }

        @Override
        public Long get(@NonNull Bundle bundle, @NonNull String key) {
            return (Long) bundle.get(key);
        }

        @Nullable
        public Long parseValue(@NonNull String value) {
            if (value.endsWith("L")) {
                value = value.substring(0, value.length() - 1);
            }
            try {
                if (value.startsWith("0x")) {
                    return Long.parseLong(value.substring(2), 16);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @NonNull
        @Override
        public String getName() {
            return "long";
        }
    };

    public static final NavType<long[]> LongArrayType = new NavType<long[]>(true) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, long[] value) {
            bundle.putLongArray(key, value);
        }

        @Override
        public long[] get(@NonNull Bundle bundle, @NonNull String key) {
            return (long[]) bundle.get(key);
        }

        @Override
        public long[] parseValue(@NonNull String value) {
            throw new UnsupportedOperationException("Arrays don't support string values.");
        }

        @NonNull
        @Override
        public String getName() {
            return "long[]";
        }
    };

    //TODO: make identical to safeargs version
    public static final NavType<Float> FloatType = new NavType<Float>(false) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, Float value) {
            bundle.putFloat(key, value);
        }

        @Override
        public Float get(@NonNull Bundle bundle, @NonNull String key) {
            return (Float) bundle.get(key);
        }

        @Nullable
        public Float parseValue(@NonNull String value) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @NonNull
        @Override
        public String getName() {
            return "float";
        }
    };

    public static final NavType<float[]> FloatArrayType = new NavType<float[]>(true) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, float[] value) {
            bundle.putFloatArray(key, value);
        }

        @Override
        public float[] get(@NonNull Bundle bundle, @NonNull String key) {
            return (float[]) bundle.get(key);
        }

        @Override
        public float[] parseValue(@NonNull String value) {
            throw new UnsupportedOperationException("Arrays don't support string values.");
        }

        @NonNull
        @Override
        public String getName() {
            return "float[]";
        }
    };

    public static final NavType<Boolean> BoolType = new NavType<Boolean>(false) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, Boolean value) {
            bundle.putBoolean(key, value);
        }

        @Override
        public Boolean get(@NonNull Bundle bundle, @NonNull String key) {
            return (Boolean) bundle.get(key);
        }

        @Override
        public Boolean parseValue(@NonNull String value) {
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            }
            return null;
        }

        @NonNull
        @Override
        public String getName() {
            return "boolean";
        }
    };

    public static final NavType<boolean[]> BoolArrayType = new NavType<boolean[]>(true) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, boolean[] value) {
            bundle.putBooleanArray(key, value);
        }

        @Override
        public boolean[] get(@NonNull Bundle bundle, @NonNull String key) {
            return (boolean[]) bundle.get(key);
        }

        @Override
        public boolean[] parseValue(@NonNull String value) {
            throw new UnsupportedOperationException("Arrays don't support string values.");
        }

        @NonNull
        @Override
        public String getName() {
            return "boolean[]";
        }
    };

    public static final NavType<String> StringType = new NavType<String>(true) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, String value) {
            bundle.putString(key, value);
        }

        @Override
        public String get(@NonNull Bundle bundle, @NonNull String key) {
            return (String) bundle.get(key);
        }

        @Override
        public String parseValue(@NonNull String value) {
            return value;
        }

        @NonNull
        @Override
        public String getName() {
            return "string";
        }
    };

    public static final NavType<String[]> StringArrayType = new NavType<String[]>(true) {
        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, String[] value) {
            bundle.putStringArray(key, value);
        }

        @Override
        public String[] get(@NonNull Bundle bundle, @NonNull String key) {
            return (String[]) bundle.get(key);
        }

        @Override
        public String[] parseValue(@NonNull String value) {
            throw new UnsupportedOperationException("Arrays don't support string values.");
        }

        @NonNull
        @Override
        public String getName() {
            return "string[]";
        }
    };

    /**
     * ObjectType is used for Parcelable/Serializable @{link NavArgument}s.
     *
     * @param <D> the Parcelable or Serializable class
     */
    public static final class ObjectType<D> extends NavType<D> {
        @NonNull private final Class<D> mType;

        public ObjectType(@NonNull Class<D> type) {
            super(true);
            if (!Parcelable.class.isAssignableFrom(type)
                    && !Serializable.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        type + " does not implement Parcelable or Serializable.");
            }
            this.mType = type;
        }

        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, @Nullable D value) {
            mType.cast(value);
            if (value == null || value instanceof Parcelable) {
                bundle.putParcelable(key, (Parcelable) value);
            } else if (value instanceof Serializable) {
                bundle.putSerializable(key, (Serializable) value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        @Nullable
        public D get(@NonNull Bundle bundle, @NonNull String key) {
            return (D) bundle.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        @Nullable
        public D parseValue(@NonNull String value) {
            if (!mType.isEnum()) {
                return null;
            }
            for (Object constant : mType.getEnumConstants()) {
                if (((Enum) constant).name().equals(value)) {
                    return (D) constant;
                }
            }
            return null;
        }

        @Override
        @NonNull
        public String getName() {
            return mType.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ObjectType<?> that = (ObjectType<?>) o;

            return mType.equals(that.mType);
        }

        @Override
        public int hashCode() {
            return mType.hashCode();
        }
    }

    /**
     * ObjectArrayType is used for arrays of Parcelable/Serializable @{link NavArgument}s.
     *
     * @param <D> the array type of a Parcelable or Serializable class
     */
    public static final class ObjectArrayType<D> extends NavType<D> {
        @NonNull private final Class<D> mType;

        public ObjectArrayType(@NonNull Class<D> arrayType) {
            super(true);
            if (!arrayType.isArray()) {
                throw new IllegalArgumentException(arrayType + " must be an array type.");
            }
            Class type = arrayType.getComponentType();
            assert type != null;
            if (!Parcelable.class.isAssignableFrom(type)
                    && !Serializable.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        type + " does not implement Parcelable or Serializable.");
            }
            this.mType = arrayType;
        }

        @Override
        public void put(@NonNull Bundle bundle, @NonNull String key, @Nullable D value) {
            mType.cast(value);
            if (value == null || Parcelable.class.isAssignableFrom(
                    value.getClass().getComponentType())) {
                bundle.putParcelableArray(key, (Parcelable[]) value);
            } else if (Serializable.class.isAssignableFrom(value.getClass().getComponentType())) {
                bundle.putSerializable(key, (Serializable) value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        @Nullable
        public D get(@NonNull Bundle bundle, @NonNull String key) {
            return (D) bundle.get(key);
        }

        @Override
        @Nullable
        public D parseValue(@NonNull String value) {
            throw new UnsupportedOperationException("Arrays don't support string values.");
        }

        @Override
        @NonNull
        public String getName() {
            return mType.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ObjectArrayType<?> that = (ObjectArrayType<?>) o;

            return mType.equals(that.mType);
        }

        @Override
        public int hashCode() {
            return mType.hashCode();
        }
    }
}
