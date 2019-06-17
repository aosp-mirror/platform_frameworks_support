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
    CameraControl DEFAULT_EMPTY_CAMERACONTROL = new CameraControl() {
    };
}
