<<<<<<< HEAD   (45b71a Merge "Merge empty history for sparse-5611686-L1840000032132)
=======
/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.view.Surface;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.ListenableFuture;

/**
 * A {@link DeferrableSurface} which always returns immediately.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class ImmediateSurface extends DeferrableSurface {
    private final Surface mSurface;

    public ImmediateSurface(Surface surface) {
        mSurface = surface;
    }

    @Override
    public ListenableFuture<Surface> getSurface() {
        return Futures.immediateFuture(mSurface);
    }
}
>>>>>>> BRANCH (2c8b21 Merge "Merge cherrypicks of [973155, 973156] into sparse-561)
