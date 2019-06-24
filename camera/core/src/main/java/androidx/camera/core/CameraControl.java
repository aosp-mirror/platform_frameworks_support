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

package androidx.camera.core;

/**
 * The CameraControl is exposed publicly to allow apps to control camera across all use cases.
 *
 * <p>Some operations like zoom, focus and metering are affecting camera across use cases. The
 * CameraControl is to provide apps capability to control the camera.
 */
public interface CameraControl {
    /**
     * Set current zoom by multiplier.
     *
     * <p>If the multiplier is small than {@link CameraControl#getMinZoom()},
     * {@link CameraControl#getMinZoom() is set instead. If the multiplier is larger than
     * {@link CameraControl#getMaxZoom()}, {@link CameraControl#getMaxZoom()} is set instead.
     *
     * @param multiplier of zoom to be applied
     */
    void setZoom(float multiplier);

    /**
     * Returns current zoom multiplier
     */
    float getZoom();

    /**
     * Returns available maximum zoom multiplier.
     *
     * <p>For devices that don't support zoom , it will returns 1.0.
     */
    float getMaxZoom();

    /**
     * Returns minimum zoom multiplier.
     */
    float getMinZoom();


    CameraControl DEFAULT_EMPTY_CAMERACONTROL = new CameraControl() {
        @Override
        public void setZoom(float multiplier) {

        }

        @Override
        public float getZoom() {
            return 0;
        }

        @Override
        public float getMaxZoom() {
            return 0;
        }

        @Override
        public float getMinZoom() {
            return 0;
        }
    };
}
