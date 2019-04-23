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

package androidx.camera.testing;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;

import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;

/** Utility functions to obtain fake {@link CameraCharacteristics} for testing. */
public final class CameraCharacteristicsUtil {

    /**
     * Builds a fake {@lkink CameraCharacteristics}.
     */
    public static final class Builder {
        private int mLensFacing = CameraCharacteristics.LENS_FACING_BACK;
        private Rect mSensorSize = new Rect(0, 0, 4000, 3000);
        private int mSensorOrientation = 90;
        private int mHardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
        private StreamConfigurationMap mStreamConfigurationMap =
                StreamConfigurationMapUtil.generateFakeStreamConfigurationMap();

        /**
         * Sets the lens facing.
         *
         * @param lensFacing for the camera
         * @return the modified builder
         * @see CameraCharacteristics#LENS_FACING
         */
        public Builder setLensFacing(int lensFacing) {
            mLensFacing = lensFacing;
            return this;
        }

        /**
         * Gets the lens facing.
         *
         * @return the lens facing
         */
        public int getLensFacing() {
            return mLensFacing;
        }

        /**
         * Sets the sensor size.
         *
         * @param sensorSize the sensor size
         * @return the modified builder
         * @see CameraCharacteristics#SENSOR_INFO_ACTIVE_ARRAY_SIZE
         */
        public Builder setSensorSize(Rect sensorSize) {
            mSensorSize = sensorSize;
            return this;
        }

        /**
         * Gets the sensor size.
         *
         * @return the sensor size
         */
        public Rect getSensorSize() {
            return mSensorSize;
        }

        /**
         * Sets the sensor orientation.
         *
         * @param sensorOrientation the sensor orientation
         * @return the modified builder
         * @see CameraCharacteristics#SENSOR_ORIENTATION
         */
        public Builder setSensorOrientation(int sensorOrientation) {
            mSensorOrientation = sensorOrientation;
            return this;
        }

        /**
         * Gets the sensor orientation.
         *
         * @return the sensor orientation
         */
        public int getSensorOrientation() {
            return mSensorOrientation;
        }

        /**
         * Sets the hardware level.
         *
         * @param hardwareLevel the hardware level
         * @return the modified builder
         * @see CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL
         */
        public Builder setHardwareLevel(int hardwareLevel) {
            mHardwareLevel = hardwareLevel;
            return this;
        }

        /**
         * Gets the hardware level.
         *
         * @return the hardware level
         */
        public int getHardwareLevel() {
            return mHardwareLevel;
        }

        /**
         * Sets the stream configuration map.
         *
         * @param streamConfigurationMap the stream configuration map
         * @return the modified builder
         * @see CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP
         */
        public Builder setStreamConfigurationMap(StreamConfigurationMap streamConfigurationMap) {
            mStreamConfigurationMap = streamConfigurationMap;
            return this;
        }

        /**
         * Gets the stream configuration map.
         *
         * @return the stream configuration map
         */
        public StreamConfigurationMap getStreamConfigurationMap() {
            return mStreamConfigurationMap;
        }

        /**
         * Builds a {@link CameraCharacteristics} from the set values.
         *
         * <p>The {@link CameraCharacteristics} is tied to a {@link ShadowCameraCharacteristics}, so
         * it is intended to be used for Robolectric tests which require a fake
         * {@link CameraCharacteristics} instance.
         *
         * @return the built {@link CameraCharacteristics}
         */
        public CameraCharacteristics build() {
            CameraCharacteristics characteristics =
                    ShadowCameraCharacteristics.newCameraCharacteristics();

            ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
            shadowCharacteristics.set(
                    CameraCharacteristics.LENS_FACING, mLensFacing);
            shadowCharacteristics.set(
                    CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                    mSensorSize);
            shadowCharacteristics.set(CameraCharacteristics.SENSOR_ORIENTATION,
                    mSensorOrientation);
            shadowCharacteristics.set(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                    mHardwareLevel);
            shadowCharacteristics.set(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                    mStreamConfigurationMap);

            return characteristics;
        }
    }
}
