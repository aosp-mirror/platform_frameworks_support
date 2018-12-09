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
import java.lang.Object;
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

        @NonNull
        public String getArg1() {
            return bundle.getString("arg1");
        }

        @NonNull
        public String getArg2() {
            return bundle.getString("arg2");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Previous that = (Previous) object;
            if (bundle.containsKey("arg1") != that.bundle.containsKey("arg1")) {
                return false;
            }
            if (getArg1() != null ? !getArg1().equals(that.getArg1()) : that.getArg1() != null) {
                return false;
            }
            if (bundle.containsKey("arg2") != that.bundle.containsKey("arg2")) {
                return false;
            }
            if (getArg2() != null ? !getArg2().equals(that.getArg2()) : that.getArg2() != null) {
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
            result = 31 * result + (getArg1() != null ? getArg1().hashCode() : 0);
            result = 31 * result + (getArg2() != null ? getArg2().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "Previous(actionId=" + getActionId() + "){"
                    + "arg1=" + getArg1()
                    + ", arg2=" + getArg2()
                    + "}";
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

        @NonNull
        public String getMain() {
            return bundle.getString("main");
        }

        @NonNull
        public String getOptional() {
            return bundle.getString("optional");
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
            if (bundle.containsKey("optional") != that.bundle.containsKey("optional")) {
                return false;
            }
            if (getOptional() != null ? !getOptional().equals(that.getOptional()) : that.getOptional() != null) {
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
            result = 31 * result + (getOptional() != null ? getOptional().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "Next(actionId=" + getActionId() + "){"
                    + "main=" + getMain()
                    + ", optional=" + getOptional()
                    + "}";
        }
    }
}