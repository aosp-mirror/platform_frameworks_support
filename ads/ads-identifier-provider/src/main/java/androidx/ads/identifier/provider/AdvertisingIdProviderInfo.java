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
    @Nullable
    public abstract Intent getSettingsIntent();

    /** Create a {@link Builder}. */
    static Builder builder() {
        return new AutoValue_AdvertisingIdProviderInfo.Builder();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof AdvertisingIdProviderInfo) {
            AdvertisingIdProviderInfo that = (AdvertisingIdProviderInfo) o;
            return this.getPackageName().equals(that.getPackageName())
                    && settingsIntentEquals(this.getSettingsIntent(), that.getSettingsIntent());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash *= 1000003;
        hash ^= getPackageName().hashCode();
        hash *= 1000003;
        hash ^= settingsIntentHashCode(getSettingsIntent());
        return hash;
    }

    private static boolean settingsIntentEquals(
            @Nullable Intent intent1, @Nullable Intent intent2) {
        if (intent1 == null || intent2 == null) return intent1 == intent2;
        return (intent1.getAction() == null ? intent2.getAction() == null
                : intent1.getAction().equals(intent2.getAction())) && (
                intent1.getComponent() == null ? intent2.getComponent() == null
                        : intent1.getComponent().equals(intent2.getComponent()));
    }

    private static int settingsIntentHashCode(@Nullable Intent intent) {
        if (intent == null) return 0;
        int hash = 1;
        hash *= 1000003;
        hash ^= (intent.getAction() == null) ? 0 : intent.getAction().hashCode();
        hash *= 1000003;
        hash ^= (intent.getComponent() == null) ? 0 : intent.getComponent().hashCode();
        return hash;
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
