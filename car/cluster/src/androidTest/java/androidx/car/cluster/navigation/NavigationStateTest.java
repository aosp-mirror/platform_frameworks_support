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

    @Test
    public void nullMessageIsDeserializedAsEmptyState() {
        // Serialize and deserialize a null state.
        Parcelable out = deserialize(serialize(null));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals(createEmptyState(), result);
    }

    @Test
    public void nullsOnNonNullsFieldDeserialization() {
        NavigationState state = createSampleState();

        // Setting an string field to null (even though this is not allowed by the API)
        state.mStatus.mDescription = null;
        // Setting an enum to null (even though this is not allowed by the API)
        state.mMode = null;
        // Setting a list to null (even though this is not allowed by the API)
        state.mSteps = null;

        // Serialize and deserialize
        Parcelable out = deserialize(serialize(state.toParcelable()));

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out);
        assertEquals("", result.getStatus().getDescription());
        assertEquals(NavigationState.NavigationMode.NOT_NAVIGATING, result.getMode());
        assertEquals(new ArrayList(), result.getSteps());
    }

    private NavigationState createEmptyState() {
        return new NavigationState();
    }

    private NavigationState createSampleState() {
        List<Lane> lanes = Arrays.asList(
                new Lane()
                        .setHightlightedDirection(
                                new LaneDirection(LaneDirection.Shape.U_TURN, Side.LEFT))
                        .setDirections(Arrays.asList(
                                new LaneDirection(LaneDirection.Shape.NORMAL_TURN,
                                        Side.LEFT),
                                new LaneDirection(LaneDirection.Shape.STRAIGHT,
                                        Side.NO_SIDE))),
                new Lane()
                        .setDirections(Arrays.asList(
                                new LaneDirection(LaneDirection.Shape.STRAIGHT,
                                        Side.NO_SIDE))));
        List<Step> steps = Arrays.asList(
                new Step()
                        .setManeuver(new Maneuver(Maneuver.Type.DEPART, 0))
                        .setDistance(new Distance(10, 10000, Distance.Unit.METERS))
                        .setLanes(lanes),
                new Step()
                        .setManeuver(new Maneuver(Maneuver.Type.TURN_NORMAL_LEFT, 0))
                        .setDistance(new Distance(15, 15000, Distance.Unit.METERS)));
        List<Destination> destinations = Arrays.asList(
                new Destination("Home")
                        .setDistance(new Distance(1230, 1200, Distance.Unit.KILOMETERS))
        );
        return new NavigationState()
                .setStatus(new ProducerStatus(ProducerStatus.Code.OK, ""))
                .setMode(NavigationState.NavigationMode.TURN_BY_TURN)
                .setSegment(new Segment("Main St."))
                .setSteps(steps)
                .setDestinations(destinations);
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
