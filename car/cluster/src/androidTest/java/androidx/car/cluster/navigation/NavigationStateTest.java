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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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
        in.putParcelable(bundleKey, state.toParcelable());

        // Serialize it into a {@link Parcel}
        Parcel parcel = Parcel.obtain();
        in.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        // Deserialize it back to {@link Bundle}
        Bundle out = new Bundle();
        out.setClassLoader(NavigationState.class.getClassLoader());
        out.readFromParcel(parcel);

        // Recover the state and assert
        NavigationState result = NavigationState.fromParcelable(out.getParcelable(bundleKey));
        assertEquals(state, result);
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
                        .setDistance(new Distance(10, Distance.Unit.METERS, 10000))
                        .setDuration(Duration.ofSeconds(20))
                        .setLanes(lanes),
                new Step()
                        .setManeuver(new Maneuver(Maneuver.Type.TURN_NORMAL_LEFT, 0))
                        .setDistance(new Distance(15, Distance.Unit.METERS, 15000))
                        .setDuration(Duration.ofMinutes(5)));
        List<Destination> destinations = Arrays.asList(
                new Destination("Home")
                        .setDistance(new Distance(1230, Distance.Unit.KILOMETERS, 1200))
        );
        return new NavigationState()
                .setStatus(new ProducerStatus(ProducerStatus.Code.OK, ""))
                .setMode(NavigationState.NavigationMode.TURN_BY_TURN)
                .setSegment(new Segment("Main St."))
                .setSteps(steps)
                .setDestinations(destinations);
    }
}
