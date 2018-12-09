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

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.lang.String;

public class SanitizedMainFragmentDirections {
    @NonNull
    public static PreviousAction previousAction(@NonNull String arg1, @NonNull String arg2) {
        return new PreviousAction(arg1, arg2);
    }

    @NonNull
    public static NextAction nextAction(@NonNull String mainArg) {
        return new NextAction(mainArg);
    }

    public static class PreviousAction implements NavDirections {
        private Bundle bundle = new Bundle();

        public PreviousAction(@NonNull String arg1, @NonNull String arg2) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg_1\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("arg_1", arg1);
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg.2\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("arg.2", arg2);
        }

        @NonNull
        public PreviousAction setArg1(@NonNull String arg1) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg_1\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("arg_1", arg1);
            return this;
        }

        @NonNull
        public PreviousAction setArg2(@NonNull String arg2) {
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg.2\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("arg.2", arg2);
            return this;
        }

        @Override
        @NonNull
        public Bundle getArguments() {
            return bundle;
        }

        @Override
        public int getActionId() {
            return a.b.R.id.previous_action;
        }
    }

    public static class NextAction implements NavDirections {
        private Bundle bundle = new Bundle();

        public NextAction(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"main_arg\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("main_arg", mainArg);
        }

        @NonNull
        public NextAction setMainArg(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"main_arg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("main_arg", mainArg);
            return this;
        }

        @NonNull
        public NextAction setOptionalArg(@NonNull String optionalArg) {
            if (optionalArg == null) {
                throw new IllegalArgumentException("Argument \"optional.arg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("optional.arg", optionalArg);
            return this;
        }

        @Override
        @NonNull
        public Bundle getArguments() {
            return bundle;
        }

        @Override
        public int getActionId() {
            return a.b.R.id.next_action;
        }
    }
}