/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.versionedparcelable.testclasses;

import androidx.annotation.NonNull;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

@VersionedParcelize(allowSerialization = true)
public class ClassA_Version2 implements VersionedParcelable {
    @ParcelField(10)
    int mSomeValue;

    @ParcelField(9)
    int mSomeOtherValue;

    public ClassA_Version2() {
    }

    public ClassA_Version2(int someValue, int someOtherValue) {
        mSomeValue = someValue;
        mSomeOtherValue = someOtherValue;
    }

    public static class Builder {
        private int mSomeValue;
        private int mSomeOtherValue;

        @NonNull
        public Builder setSomeValue(int someValue) {
            mSomeValue = someValue;
            return this;
        }

        @NonNull
        public Builder setSomeOtherValue(int someOtherValue) {
            mSomeOtherValue = someOtherValue;
            return this;
        }

        @NonNull
        public ClassA_Version2 build() {
            return new ClassA_Version2(mSomeValue, mSomeOtherValue);
        }
    }

    public int getSomeValue() {
        return mSomeValue;
    }

    public int getSomeOtherValue() {
        return mSomeOtherValue;
    }
}
