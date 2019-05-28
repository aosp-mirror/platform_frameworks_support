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

package androidx.camera.extensions.impl;

/**
 * Provides interfaces that the vendor needs to implement to handle the version check.
 */
public interface ExtenderVersioning {

    /**
     * Provide the current CameraX extension library version to vendor library and vendor would
     * need to return the supported version for this device. If the returned version is not
     * supported by CameraX library, the Preview and ImageCapture would not be able to enable the
     * specific effects provided by the vendor.
     *
     * <p> The CameraX library uses the semantic versioning in a form of
     * MAJOR.MINOR.PATCH-description.
     * We will increment the
     * MAJOR version when make incompatible API changes,
     * MINOR version when add functionality in a backwards-compatible manner, and
     * PATCH version when make backwards-compatible bug fixes. And the description can be ignored.
     *
     * <p> The returned version from the vendor would be compatible for CameraX when MAJOR number is
     * matched and the MINOR number is matched or exists in previous release.
     *
     * @param version the version of CameraX library formatted as MAJOR.MINOR.PATCH-description.
     * @return the version that vendor supported in this device. The MAJOR.MINOR.PATCH format
     * should be used.
     */
    String checkApiVersion(String version);
}
