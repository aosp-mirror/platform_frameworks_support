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
import android.os.Parcelable;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link NavigationState} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class NavigationStateTest {
    private static final String BUNDLE_KEY = "DATA";

    /**
     * Tests that all the objects in the androidx.car.navigation.schema package are serializable
     * and deserializable, and all content is maintained.
     */
    @Test
    public void schemaIsSerializableAndDeserializable() {
        NavigationState state = createSampleState();

        // Serialize and deserialize
        Parcelable out = deserialize(serialize(state.toParcelable()));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals(state, result);
    }

    /**
     * Test that a null message is received as an empty message after deserialization.
     */
    @Test
    public void nullMessageIsDeserializedAsEmptyState() {
        // Serialize and deserialize a null state.
        Parcelable out = deserialize(serialize(null));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals(createEmptyState(), result);
    }

    /**
     * Tests that {@link String} returning methods always return non-null values, even if they are
     * set to null.
     */
    @Test
    public void nullStringFieldDeserialization() {
        NavigationState state = createSampleState();

        // Setting an string field to null (even though this is not allowed by the API)
        state.mDestinations.get(0).mTitle = null;

        // Serialize and deserialize
        Parcelable out = deserialize(serialize(state.toParcelable()));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals("", result.getDestinations().get(0).getTitle());
    }

    /**
     * Tests that {@link Enum} returning methods always return non-null values, even if they are
     * set to null.
     */
    @Test
    public void nullEnumFieldDeserialization() {
        NavigationState state = createSampleState();

        // Setting an enum to null (even though this is not allowed by the API)
        state.mSteps.get(0).mManeuver.mType = null;

        // Serialize and deserialize
        Parcelable out = deserialize(serialize(state.toParcelable()));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals(Maneuver.Type.UNKNOWN, result.getSteps().get(0).getManeuver().getType());
    }

    /**
     * Tests that {@link List} returning methods always return non-null values, even if they are
     * set to null.
     */
    @Test
    public void nullListFieldDeserialization() {
        NavigationState state = createSampleState();

        // Setting a list to null (even though this is not allowed by the API)
        state.mDestinations = null;

        // Serialize and deserialize
        Parcelable out = deserialize(serialize(state.toParcelable()));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals(new ArrayList(), result.getDestinations());
    }

    private NavigationState createEmptyState() {
        return new NavigationState();
    }

    private NavigationState createSampleState() {
        List<Step> steps = Arrays.asList(
                new Step().setManeuver(new Maneuver(Maneuver.Type.DEPART))
                        .setDistance(new Distance(10, 10000, Distance.Unit.METERS)),
                new Step().setManeuver(new Maneuver(Maneuver.Type.TURN_NORMAL_LEFT))
                        .setDistance(new Distance(15, 15000, Distance.Unit.METERS)));
        List<Destination> destinations = Collections.singletonList(
                new Destination("Home").setDistance(new Distance(1230, 1200,
                        Distance.Unit.KILOMETERS))
        );
        return new NavigationState().setSteps(steps).setDestinations(destinations);
    }

    private Parcel serialize(Parcelable state) {
        Bundle in = new Bundle();
        Parcel parcel = Parcel.obtain();
        in.putParcelable(BUNDLE_KEY, state);
        in.writeToParcel(parcel, 0);
        return parcel;
    }

    private Parcelable deserialize(Parcel parcel) {
        Bundle out = new Bundle();
        parcel.setDataPosition(0);
        out.setClassLoader(NavigationState.class.getClassLoader());
        out.readFromParcel(parcel);
        return out.getParcelable(BUNDLE_KEY);
    }
}
