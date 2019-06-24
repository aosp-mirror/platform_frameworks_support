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

package androidx.camera.camera2.impl;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import androidx.annotation.RestrictTo;

/**
 * Zoom control implementations
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ZoomControl {
    public static final float DEFAULT_ZOOM = 1.0f;
    public static final float MIN_ZOOM = DEFAULT_ZOOM;
    public static final float UNSUPPORTED_ZOOM = DEFAULT_ZOOM;
    private volatile float mZoomMultiplier = DEFAULT_ZOOM;

    private final Camera2CameraControl mCamera2CameraControl;
    private final CameraManager mCameraManager;
    private final String mCameraId;

    ZoomControl(Camera2CameraControl camera2CameraControl, CameraManager cameraManager,
            String cameraId) {
        mCamera2CameraControl = camera2CameraControl;
        mCameraManager = cameraManager;
        mCameraId = cameraId;
    }

    void setZoom(float multiplier) {
        Rect sensorRect = mCamera2CameraControl.getSensorRect();
        if (sensorRect == null) {
            return;
        }

        if (multiplier > getMaxZoom()) {
            multiplier = getMaxZoom();
        } else if (multiplier < getMinZoom()) {
            multiplier = getMinZoom();
        }

        mZoomMultiplier = multiplier;
        int cropWidth = (int) ((sensorRect.width() / multiplier));
        int cropHeight = (int) ((sensorRect.height() / multiplier));
        int left = (int) ((sensorRect.width() - cropWidth) / 2.0f);
        int top = (int) ((sensorRect.height() - cropHeight) / 2.0f);

        Rect cropRegion = new Rect(left, top, left + cropWidth, top + cropHeight);
        mCamera2CameraControl.setCropRegion(cropRegion);
    }

    float getZoom() {
        return mZoomMultiplier;
    }

    float getMaxZoom() {
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(
                    mCameraId);
            Float maxZoom = characteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

            if (maxZoom == null) {
                return UNSUPPORTED_ZOOM;
            }

            return maxZoom;
        } catch (CameraAccessException e) {
            return UNSUPPORTED_ZOOM;
        }
    }

    float getMinZoom() {
        return MIN_ZOOM;
    }
}
