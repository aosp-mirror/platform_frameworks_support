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

public class SanitizedMainFragmentArgs {
    private Bundle bundle = new Bundle();

    private SanitizedMainFragmentArgs() {
    }

    private SanitizedMainFragmentArgs(Bundle bundle) {
        this.bundle.putAll(bundle);
    }

    @NonNull
    public static SanitizedMainFragmentArgs fromBundle(Bundle bundle) {
        SanitizedMainFragmentArgs result = new SanitizedMainFragmentArgs();
        bundle.setClassLoader(SanitizedMainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("name.with.dot")) {
            int nameWithDot;
            nameWithDot = bundle.getInt("name.with.dot");
            result.bundle.putInt("name.with.dot", nameWithDot);
        } else {
            throw new IllegalArgumentException("Required argument \"name.with.dot\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name_with_underscore")) {
            int nameWithUnderscore;
            nameWithUnderscore = bundle.getInt("name_with_underscore");
            result.bundle.putInt("name_with_underscore", nameWithUnderscore);
        } else {
            throw new IllegalArgumentException("Required argument \"name_with_underscore\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name with spaces")) {
            int nameWithSpaces;
            nameWithSpaces = bundle.getInt("name with spaces");
            result.bundle.putInt("name with spaces", nameWithSpaces);
        } else {
            throw new IllegalArgumentException("Required argument \"name with spaces\" is missing and does not have an android:defaultValue");
        }
        return result;
    }

    public int getNameWithDot() {
        int nameWithDot;
        nameWithDot = bundle.getInt("name.with.dot");
        return nameWithDot;
    }

    public int getNameWithUnderscore() {
        int nameWithUnderscore;
        nameWithUnderscore = bundle.getInt("name_with_underscore");
        return nameWithUnderscore;
    }

    public int getNameWithSpaces() {
        int nameWithSpaces;
        nameWithSpaces = bundle.getInt("name with spaces");
        return nameWithSpaces;
    }

    @NonNull
    public Bundle toBundle() {
        return bundle;
    }

    public static class Builder {
        private Bundle bundle = new Bundle();

        public Builder(SanitizedMainFragmentArgs original) {
            this.bundle.putAll(original.bundle);
        }

        public Builder(int nameWithDot, int nameWithUnderscore, int nameWithSpaces) {
            bundle.putInt("name.with.dot", nameWithDot);
            bundle.putInt("name_with_underscore", nameWithUnderscore);
            bundle.putInt("name with spaces", nameWithSpaces);
        }

        @NonNull
        public SanitizedMainFragmentArgs build() {
            SanitizedMainFragmentArgs result = new SanitizedMainFragmentArgs(bundle);
            return result;
        }

        @NonNull
        public Builder setNameWithDot(int nameWithDot) {
            this.bundle.putInt("name.with.dot", nameWithDot);
            return this;
        }

        @NonNull
        public Builder setNameWithUnderscore(int nameWithUnderscore) {
            this.bundle.putInt("name_with_underscore", nameWithUnderscore);
            return this;
        }

        @NonNull
        public Builder setNameWithSpaces(int nameWithSpaces) {
            this.bundle.putInt("name with spaces", nameWithSpaces);
            return this;
        }

        public int getNameWithDot() {
            int nameWithDot;
            nameWithDot = bundle.getInt("name.with.dot");
            return nameWithDot;
        }

        public int getNameWithUnderscore() {
            int nameWithUnderscore;
            nameWithUnderscore = bundle.getInt("name_with_underscore");
            return nameWithUnderscore;
        }

        public int getNameWithSpaces() {
            int nameWithSpaces;
            nameWithSpaces = bundle.getInt("name with spaces");
            return nameWithSpaces;
        }
    }
}