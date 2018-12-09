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

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.lang.String;

public class MainFragmentDirections {
    @NonNull
    public static Previous previous(@NonNull String arg1, @NonNull String arg2) {
        return new Previous(arg1, arg2);
    }

    @NonNull
    public static Next next(@NonNull String main) {
        return new Next(main);
    }

    public static class Previous implements NavDirections {
        private Bundle bundle = new Bundle();

        public Previous(@NonNull String arg1, @NonNull String arg2) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg1\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("arg1", arg1);
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg2\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("arg2", arg2);
        }

        @NonNull
        public Previous setArg1(@NonNull String arg1) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg1\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("arg1", arg1);
            return this;
        }

        @NonNull
        public Previous setArg2(@NonNull String arg2) {
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg2\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("arg2", arg2);
            return this;
        }

        @Override
        @NonNull
        public Bundle getArguments() {
            return bundle;
        }

        @Override
        public int getActionId() {
            return a.b.R.id.previous;
        }
    }

    public static class Next implements NavDirections {
        private Bundle bundle = new Bundle();

        public Next(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("main", main);
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
        public Next setOptional(@NonNull String optional) {
            if (optional == null) {
                throw new IllegalArgumentException("Argument \"optional\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("optional", optional);
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
}