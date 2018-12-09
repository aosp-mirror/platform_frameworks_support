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
}