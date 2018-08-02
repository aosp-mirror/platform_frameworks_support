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

package androidx.car.cluster.navigation;

import static org.junit.Assert.assertEquals;

import android.os.Bundle;
import android.os.Parcel;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link NavigationState} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class NavigationStateTest {
    /**
     * Tests that all the objects in the androidx.car.navigation.schema package are serializable
     * and deserializable, and all content is maintained.
     */
    @Test
    public void schemaIsSerializableAndDeserializable() {
        String bundleKey = "DATA";
        NavigationState state = createSampleState();

        // Set the state into a {@link Bundle}
        Bundle in = new Bundle();
        in.putParcelable(bundleKey, ParcelUtils.toParcelable(state));

        // Serialize it into a {@link Parcel}
        Parcel parcel = Parcel.obtain();
        in.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        // Deserialize it back to {@link Bundle}
        Bundle out = new Bundle();
        out.readFromParcel(parcel);

        // Recover the state and assert
        NavigationState result = ParcelUtils.fromParcelable(out.getParcelable(bundleKey));
        assertEquals(state, result);
    }

    private NavigationState createSampleState() {
        return new NavigationState();
    }
}
