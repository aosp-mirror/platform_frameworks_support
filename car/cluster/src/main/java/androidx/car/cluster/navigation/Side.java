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

/**
 * Possible driving side (e.g. {@link #RIGHT} in USA, and {@link #LEFT} in Australia), or turning
 * direction.
 */
public enum Side {
    /**
     * The side is unknown to the consumer.
     */
    UNKNOWN,
    /**
     * Driving or turning side is not relevant (e.g.: for {@link Maneuver.Type#STRAIGHT},
     * turning direction is not relevant).
     */
    NO_SIDE,
    /**
     * Left-hand driving (e.g.: Australian driving side), or left turn.
     */
    LEFT,
    /**
     * Right-hand driving (e.g.: USA driving side), or right turn.
     */
    RIGHT,
}
