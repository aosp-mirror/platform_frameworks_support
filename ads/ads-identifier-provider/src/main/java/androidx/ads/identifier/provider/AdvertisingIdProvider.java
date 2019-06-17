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

package androidx.ads.identifier.provider;

import androidx.annotation.NonNull;

/**
 * The AndroidX Ads Identifier Provider who wants to provide a resettable identifier used for ads
 * purpose should implement this interface.
 *
 * <p>Note: The implementation of this interface must be completely thread-safe.
 */
public interface AdvertisingIdProvider {
    /** Retrieves the advertising ID in the UUID format. */
    @NonNull
    String getId();

    /** Retrieves whether the user has limit ad tracking enabled or not. */
    boolean isLimitAdTrackingEnabled();
}
