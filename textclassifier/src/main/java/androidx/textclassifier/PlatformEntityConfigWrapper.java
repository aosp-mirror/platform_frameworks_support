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

package androidx.textclassifier;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.view.textclassifier.TextClassifier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Collection;

/**
 * @hide
 */
@RequiresApi(28)
@VersionedParcelize(jetifyAs = "android.support.textclassifier.PlatformEntityConfigWrapper")
@RestrictTo(LIBRARY_GROUP)
public final class PlatformEntityConfigWrapper implements VersionedParcelable {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(1)
    public TextClassifier.EntityConfig mPlatformEntityConfig;

    /**
     * Used for VersionedParcelable.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public PlatformEntityConfigWrapper() {
    }

    PlatformEntityConfigWrapper(@NonNull TextClassifier.EntityConfig platformEntityConfig) {
        mPlatformEntityConfig = Preconditions.checkNotNull(platformEntityConfig);
    }

    Collection<String> resolveEntityTypes(
            @Nullable Collection<String> defaultEntityTypes) {
        return mPlatformEntityConfig.resolveEntityListModifications(defaultEntityTypes);
    }

    @NonNull
    Collection<String> getHints() {
        return mPlatformEntityConfig.getHints();
    }

    boolean shouldIncludeDefaultEntityTypes() {
        // TODO: In Q+, we should return
        // mPlatformEntityConfig.shouldIncludeDefaultEntityTypes().
        return !mPlatformEntityConfig.getHints().isEmpty();
    }
}
