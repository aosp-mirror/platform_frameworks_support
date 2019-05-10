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
public class ClassB_Version1 implements VersionedParcelable {
    @ParcelField(1)
    ClassA_Version1[] mItems;

    public ClassB_Version1() {
    }

    public ClassB_Version1(ClassA_Version1[] items) {
        mItems = items;
    }

    public static class Builder {
        private ClassA_Version1[] mItems;

        @NonNull
        public Builder setItems(ClassA_Version1[] items) {
            mItems = items;
            return this;
        }

        @NonNull
        public ClassB_Version1 build() {
            return new ClassB_Version1(mItems);
        }
    }

    public ClassA_Version1[] getItems() {
        return mItems;
    }
}
