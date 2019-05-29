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

import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * Advertising ID provider Information.
 */
@AutoValue
public abstract class AdvertisingIdProviderInfo {

    // Create a no-args constructor so it doesn't appear in current.txt
    AdvertisingIdProviderInfo() {
    }

    /** Retrieves the advertising ID provider package name. */
    public abstract String getPackageName();

    /** Retrieves the advertising ID. */
    public abstract @Nullable Intent getSettingsIntent();

    /** Create a {@link Builder}. */
    static Builder builder() {
        return new AutoValue_AdvertisingIdProviderInfo.Builder();
    }

    /** The builder for {@link AdvertisingIdProviderInfo}. */
    @AutoValue.Builder
    abstract static class Builder {

        // Create a no-args constructor so it doesn't appear in current.txt
        Builder() {
        }

        abstract Builder setPackageName(String packageName);

        abstract Builder setSettingsIntent(Intent settingsIntent);

        abstract AdvertisingIdProviderInfo build();
    }
}
