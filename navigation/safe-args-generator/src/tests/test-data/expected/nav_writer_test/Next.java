/*
 * Copyright 2017 The Android Open Source Project
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
import androidx.navigation.NavDirections;
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public static class Next implements NavDirections {
    private Bundle bundle = new Bundle();

    public Next(@NonNull String main, int mainInt, @NonNull ActivityInfo parcelable) {
        if (main == null) {
            throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
        }
        bundle.putString("main", main);
        bundle.putInt("mainInt", mainInt);
        if (parcelable == null) {
            throw new IllegalArgumentException("Argument \"parcelable\" is marked as non-null but was passed a null value.");
        }
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || parcelable == null) {
            bundle.putParcelable("parcelable", Parcelable.class.cast(parcelable));
        } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            bundle.putSerializable("parcelable", Serializable.class.cast(parcelable));
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
    }

    @NonNull
    public Next setMain(@NonNull String main) {
        if (main == null) {
            throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
        }
        this.bundle.putString("main", main);
        return this;
    }

    @NonNull
    public Next setMainInt(int mainInt) {
        this.bundle.putInt("mainInt", mainInt);
        return this;
    }

    @NonNull
    public Next setOptional(@NonNull String optional) {
        if (optional == null) {
            throw new IllegalArgumentException("Argument \"optional\" is marked as non-null but was passed a null value.");
        }
        this.bundle.putString("optional", optional);
        return this;
    }

    @NonNull
    public Next setOptionalInt(int optionalInt) {
        this.bundle.putInt("optionalInt", optionalInt);
        return this;
    }

    @NonNull
    public Next setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
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
    public Next setParcelable(@NonNull ActivityInfo parcelable) {
        if (parcelable == null) {
            throw new IllegalArgumentException("Argument \"parcelable\" is marked as non-null but was passed a null value.");
        }
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || parcelable == null) {
            this.bundle.putParcelable("parcelable", Parcelable.class.cast(parcelable));
        } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            this.bundle.putSerializable("parcelable", Serializable.class.cast(parcelable));
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
        return this;
    }

    @Override
    @NonNull
    public Bundle getArguments() {
        return bundle;
    }

    @Override
    public int getActionId() {
        return a.b.R.id.next;
    }

    @NonNull
    public String getMain() {
        return bundle.getString("main");
    }

    public int getMainInt() {
        return bundle.getInt("mainInt");
    }

    @NonNull
    public String getOptional() {
        return bundle.getString("optional");
    }

    public int getOptionalInt() {
        return bundle.getInt("optionalInt");
    }

    @Nullable
    public ActivityInfo getOptionalParcelable() {
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            return (ActivityInfo) bundle.get("optionalParcelable");
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
    }

    @NonNull
    public ActivityInfo getParcelable() {
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            return (ActivityInfo) bundle.get("parcelable");
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Next that = (Next) object;
        if (bundle.containsKey("main") != that.bundle.containsKey("main")) {
            return false;
        }
        if (getMain() != null ? !getMain().equals(that.getMain()) : that.getMain() != null) {
            return false;
        }
        if (bundle.containsKey("mainInt") != that.bundle.containsKey("mainInt")) {
            return false;
        }
        if (getMainInt() != that.getMainInt()) {
            return false;
        }
        if (bundle.containsKey("optional") != that.bundle.containsKey("optional")) {
            return false;
        }
        if (getOptional() != null ? !getOptional().equals(that.getOptional()) : that.getOptional() != null) {
            return false;
        }
        if (bundle.containsKey("optionalInt") != that.bundle.containsKey("optionalInt")) {
            return false;
        }
        if (getOptionalInt() != that.getOptionalInt()) {
            return false;
        }
        if (bundle.containsKey("optionalParcelable") != that.bundle.containsKey("optionalParcelable")) {
            return false;
        }
        if (getOptionalParcelable() != null ? !getOptionalParcelable().equals(that.getOptionalParcelable()) : that.getOptionalParcelable() != null) {
            return false;
        }
        if (bundle.containsKey("parcelable") != that.bundle.containsKey("parcelable")) {
            return false;
        }
        if (getParcelable() != null ? !getParcelable().equals(that.getParcelable()) : that.getParcelable() != null) {
            return false;
        }
        if (getActionId() != that.getActionId()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getMain() != null ? getMain().hashCode() : 0);
        result = 31 * result + getMainInt();
        result = 31 * result + (getOptional() != null ? getOptional().hashCode() : 0);
        result = 31 * result + getOptionalInt();
        result = 31 * result + (getOptionalParcelable() != null ? getOptionalParcelable().hashCode() : 0);
        result = 31 * result + (getParcelable() != null ? getParcelable().hashCode() : 0);
        result = 31 * result + getActionId();
        return result;
    }

    @Override
    public String toString() {
        return "Next(actionId=" + getActionId() + "){"
                + "main=" + getMain()
                + ", mainInt=" + getMainInt()
                + ", optional=" + getOptional()
                + ", optionalInt=" + getOptionalInt()
                + ", optionalParcelable=" + getOptionalParcelable()
                + ", parcelable=" + getParcelable()
                + "}";
    }
}