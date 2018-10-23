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

import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Accessory information to help the driver successfully complete a maneuver (e.g.: "On exit 45").
 * At most one of the fields of this object will be not-null at a time
 */
@VersionedParcelize
public class StepCue {
    @ParcelField(1)
    Segment mRoadTo;
    @ParcelField(3)
    Image mRoadSign;
    @ParcelField(4)
    Segment mIntersection;


}
