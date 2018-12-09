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
import java.lang.IllegalArgumentException;
import java.lang.String;

public class MainFragment$InnerFragmentArgs {
    private Bundle bundle = new Bundle();

    private MainFragment$InnerFragmentArgs() {
    }

    private MainFragment$InnerFragmentArgs(Bundle bundle) {
        this.bundle.putAll(bundle);
    }

    @NonNull
    public static MainFragment$InnerFragmentArgs fromBundle(Bundle bundle) {
        MainFragment$InnerFragmentArgs result = new MainFragment$InnerFragmentArgs();
        bundle.setClassLoader(MainFragment$InnerFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("mainArg")) {
            String mainArg;
            mainArg = bundle.getString("mainArg");
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putString("mainArg", mainArg);
        } else {
            throw new IllegalArgumentException("Required argument \"mainArg\" is missing and does not have an android:defaultValue");
        }
        return result;
    }

    @NonNull
    public String getMainArg() {
        String mainArg;
        mainArg = bundle.getString("mainArg");
        return mainArg;
    }

    @NonNull
    public Bundle toBundle() {
        return bundle;
    }

    public static class Builder {
        private Bundle bundle = new Bundle();

        public Builder(MainFragment$InnerFragmentArgs original) {
            this.bundle.putAll(original.bundle);
        }

        public Builder(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("mainArg", mainArg);
        }

        @NonNull
        public MainFragment$InnerFragmentArgs build() {
            MainFragment$InnerFragmentArgs result = new MainFragment$InnerFragmentArgs(bundle);
            return result;
        }

        @NonNull
        public Builder setMainArg(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("mainArg", mainArg);
            return this;
        }

        @NonNull
        public String getMainArg() {
            String mainArg;
            mainArg = bundle.getString("mainArg");
            return mainArg;
        }
    }
}