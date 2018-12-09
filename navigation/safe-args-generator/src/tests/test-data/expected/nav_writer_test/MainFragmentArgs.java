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

package a.b;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.file.AccessMode;

public class MainFragmentArgs {
    private Bundle bundle = new Bundle();

    private MainFragmentArgs() {
    }

    private MainFragmentArgs(Bundle bundle) {
        this.bundle.putAll(bundle);
    }

    @NonNull
    public static MainFragmentArgs fromBundle(Bundle bundle) {
        MainFragmentArgs result = new MainFragmentArgs();
        bundle.setClassLoader(MainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("main")) {
            String main;
            main = bundle.getString("main");
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putString("main", main);
        } else {
            throw new IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("optional")) {
            int optional;
            optional = bundle.getInt("optional");
            result.bundle.putInt("optional", optional);
        }
        if (bundle.containsKey("reference")) {
            int reference;
            reference = bundle.getInt("reference");
            result.bundle.putInt("reference", reference);
        }
        if (bundle.containsKey("floatArg")) {
            float floatArg;
            floatArg = bundle.getFloat("floatArg");
            result.bundle.putFloat("floatArg", floatArg);
        }
        if (bundle.containsKey("floatArrayArg")) {
            float[] floatArrayArg;
            floatArrayArg = bundle.getFloatArray("floatArrayArg");
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putFloatArray("floatArrayArg", floatArrayArg);
        } else {
            throw new IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("objectArrayArg")) {
            ActivityInfo[] objectArrayArg;
            objectArrayArg = (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putParcelableArray("objectArrayArg", objectArrayArg);
        } else {
            throw new IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("boolArg")) {
            boolean boolArg;
            boolArg = bundle.getBoolean("boolArg");
            result.bundle.putBoolean("boolArg", boolArg);
        }
        if (bundle.containsKey("optionalParcelable")) {
            ActivityInfo optionalParcelable;
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                optionalParcelable = (ActivityInfo) bundle.get("optionalParcelable");
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || optionalParcelable == null) {
                result.bundle.putParcelable("optionalParcelable", Parcelable.class.cast(optionalParcelable));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                result.bundle.putSerializable("optionalParcelable", Serializable.class.cast(optionalParcelable));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        if (bundle.containsKey("enumArg")) {
            AccessMode enumArg;
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
                enumArg = (AccessMode) bundle.get("enumArg");
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || enumArg == null) {
                result.bundle.putParcelable("enumArg", Parcelable.class.cast(enumArg));
            } else if (Serializable.class.isAssignableFrom(AccessMode.class)) {
                result.bundle.putSerializable("enumArg", Serializable.class.cast(enumArg));
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        return result;
    }

    @NonNull
    public String getMain() {
        String main;
        main = bundle.getString("main");
        return main;
    }

    public int getOptional() {
        int optional;
        optional = bundle.getInt("optional");
        return optional;
    }

    public int getReference() {
        int reference;
        reference = bundle.getInt("reference");
        return reference;
    }

    public float getFloatArg() {
        float floatArg;
        floatArg = bundle.getFloat("floatArg");
        return floatArg;
    }

    @NonNull
    public float[] getFloatArrayArg() {
        float[] floatArrayArg;
        floatArrayArg = bundle.getFloatArray("floatArrayArg");
        return floatArrayArg;
    }

    @NonNull
    public ActivityInfo[] getObjectArrayArg() {
        ActivityInfo[] objectArrayArg;
        objectArrayArg = (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
        return objectArrayArg;
    }

    public boolean getBoolArg() {
        boolean boolArg;
        boolArg = bundle.getBoolean("boolArg");
        return boolArg;
    }

    @Nullable
    public ActivityInfo getOptionalParcelable() {
        ActivityInfo optionalParcelable;
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            optionalParcelable = (ActivityInfo) bundle.get("optionalParcelable");
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
        return optionalParcelable;
    }

    @NonNull
    public AccessMode getEnumArg() {
        AccessMode enumArg;
        if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
            enumArg = (AccessMode) bundle.get("enumArg");
        } else {
            throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
        return enumArg;
    }

    @NonNull
    public Bundle toBundle() {
        return bundle;
    }

    public static class Builder {
        private Bundle bundle = new Bundle();

        public Builder(MainFragmentArgs original) {
            this.bundle.putAll(original.bundle);
        }

        public Builder(@NonNull String main, @NonNull float[] floatArrayArg,
                @NonNull ActivityInfo[] objectArrayArg) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("main", main);
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            bundle.putFloatArray("floatArrayArg", floatArrayArg);
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            bundle.putParcelableArray("objectArrayArg", objectArrayArg);
        }

        @NonNull
        public MainFragmentArgs build() {
            MainFragmentArgs result = new MainFragmentArgs(bundle);
            return result;
        }

        @NonNull
        public Builder setMain(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("main", main);
            return this;
        }

        @NonNull
        public Builder setOptional(int optional) {
            this.bundle.putInt("optional", optional);
            return this;
        }

        @NonNull
        public Builder setReference(int reference) {
            this.bundle.putInt("reference", reference);
            return this;
        }

        @NonNull
        public Builder setFloatArg(float floatArg) {
            this.bundle.putFloat("floatArg", floatArg);
            return this;
        }

        @NonNull
        public Builder setFloatArrayArg(@NonNull float[] floatArrayArg) {
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putFloatArray("floatArrayArg", floatArrayArg);
            return this;
        }

        @NonNull
        public Builder setObjectArrayArg(@NonNull ActivityInfo[] objectArrayArg) {
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putParcelableArray("objectArrayArg", objectArrayArg);
            return this;
        }

        @NonNull
        public Builder setBoolArg(boolean boolArg) {
            this.bundle.putBoolean("boolArg", boolArg);
            return this;
        }

        @NonNull
        public Builder setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || optionalParcelable == null) {
                this.bundle.putParcelable("optionalParcelable", Parcelable.class.cast(optionalParcelable));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                this.bundle.putSerializable("optionalParcelable", Serializable.class.cast(optionalParcelable));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            return this;
        }

        @NonNull
        public Builder setEnumArg(@NonNull AccessMode enumArg) {
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || enumArg == null) {
                this.bundle.putParcelable("enumArg", Parcelable.class.cast(enumArg));
            } else if (Serializable.class.isAssignableFrom(AccessMode.class)) {
                this.bundle.putSerializable("enumArg", Serializable.class.cast(enumArg));
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            return this;
        }

        @NonNull
        public String getMain() {
            String main;
            main = bundle.getString("main");
            return main;
        }

        public int getOptional() {
            int optional;
            optional = bundle.getInt("optional");
            return optional;
        }

        public int getReference() {
            int reference;
            reference = bundle.getInt("reference");
            return reference;
        }

        public float getFloatArg() {
            float floatArg;
            floatArg = bundle.getFloat("floatArg");
            return floatArg;
        }

        @NonNull
        public float[] getFloatArrayArg() {
            float[] floatArrayArg;
            floatArrayArg = bundle.getFloatArray("floatArrayArg");
            return floatArrayArg;
        }

        @NonNull
        public ActivityInfo[] getObjectArrayArg() {
            ActivityInfo[] objectArrayArg;
            objectArrayArg = (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
            return objectArrayArg;
        }

        public boolean getBoolArg() {
            boolean boolArg;
            boolArg = bundle.getBoolean("boolArg");
            return boolArg;
        }

        @Nullable
        public ActivityInfo getOptionalParcelable() {
            ActivityInfo optionalParcelable;
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                optionalParcelable = (ActivityInfo) bundle.get("optionalParcelable");
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            return optionalParcelable;
        }

        @NonNull
        public AccessMode getEnumArg() {
            AccessMode enumArg;
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
                enumArg = (AccessMode) bundle.get("enumArg");
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            return enumArg;
        }
    }
}