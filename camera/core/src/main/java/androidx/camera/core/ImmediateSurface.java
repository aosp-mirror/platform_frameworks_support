<<<<<<< HEAD   (9d364e Merge "Merge empty history for sparse-5611434-L1110000032658)
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

import com.google.common.util.concurrent.ListenableFuture;

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
>>>>>>> BRANCH (8875d3 Merge "Merge cherrypicks of [982717, 982718] into sparse-564)
