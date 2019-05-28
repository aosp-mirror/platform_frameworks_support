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

package androidx.versionedparcelable;

import static org.junit.Assert.assertEquals;

import androidx.annotation.Nullable;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@SmallTest
public class VersionedParcelCollectionTest {
    @Rule
    public final Backend.Rule backend = new Backend.Rule();

    private final Random mRandom = new Random();

    @Test
    public void testMatchedGeneratedParcelizers() {
        final CollectionTestContainer in = new CollectionTestContainer(mRandom);
        final CollectionTestContainer out = backend.writeThenRead(in);
        assertEquals(in, out);
    }

    @Test
    public void testAddedLastFieldOnItem() {
        final CollectionTestContainer container = new CollectionTestContainer(mRandom);
        final VersionedParcel parcel = backend.openForWriting();

        parcel.writeString(parcelizerName(CollectionTestContainer.class));
        parcel.writeInt(container.mBefore, 1);
        parcel.setOutputField(2);
        parcel.writeInt(container.mCollection.size());
        parcel.writeInt(1); // TYPE_VERSIONED_PARCELABLE

        for (CollectionTestItem item : container.mCollection) {
            final VersionedParcel subParcel = parcel.createSubParcel();
            subParcel.writeString(parcelizerName(CollectionTestItem.class));
            subParcel.writeInt(item.mFirstValue, 1);
            subParcel.writeInt(item.mSecondValue, 2);
            subParcel.writeInt(mRandom.nextInt(), 3);
            subParcel.closeField();
        }

        parcel.writeInt(container.mAfter, 3);
        parcel.closeField();
        assertEquals(container, backend.read(parcel));
    }

    private static String parcelizerName(Class clazz) {
        return String.format(
                "%s.%sParcelizer",
                clazz.getPackage().getName(),
                clazz.getSimpleName());
    }

    @VersionedParcelize(allowSerialization = true, deprecatedIds = {3})
    public static final class CollectionTestItem implements VersionedParcelable {
        public CollectionTestItem() {
        }

        private CollectionTestItem(Random random) {
            mFirstValue = random.nextInt();
            mSecondValue = random.nextInt();
        }

        @ParcelField(1)
        public int mFirstValue;

        @ParcelField(2)
        public int mSecondValue;

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof CollectionTestItem) {
                final CollectionTestItem other = (CollectionTestItem) obj;
                return this.mFirstValue == other.mFirstValue
                        && this.mSecondValue == other.mSecondValue;
            } else {
                return false;
            }
        }
    }

    @VersionedParcelize(allowSerialization = true)
    public static final class CollectionTestContainer implements VersionedParcelable {
        public CollectionTestContainer() {
        }

        private CollectionTestContainer(Random random) {
            this(random, 3);
        }

        private CollectionTestContainer(Random random, int size) {
            mBefore = random.nextInt();

            mCollection = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                mCollection.add(new CollectionTestItem(random));
            }

            mAfter = random.nextInt();
        }

        @ParcelField(1)
        public int mBefore;

        @ParcelField(2)
        public List<CollectionTestItem> mCollection;

        @ParcelField(3)
        public int mAfter;

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof CollectionTestContainer) {
                final CollectionTestContainer other = (CollectionTestContainer) obj;

                return this.mBefore == other.mBefore
                        && Objects.equals(this.mCollection, other.mCollection)
                        && this.mAfter == other.mAfter;

            } else {
                return false;
            }
        }
    }
}
