/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import android.graphics.Matrix;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

class GhostViewUtils {

<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
    static GhostViewImpl addGhost(View view, ViewGroup viewGroup, Matrix matrix) {
        if (Build.VERSION.SDK_INT >= 21) {
            return GhostViewApi21.addGhost(view, viewGroup, matrix);
=======
    @Nullable
    static GhostView addGhost(@NonNull View view, @NonNull ViewGroup viewGroup,
            @Nullable Matrix matrix) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Use the platform implementation on P as we can't backport the shadows drawing.
            return GhostViewPlatform.addGhost(view, viewGroup, matrix);
        } else {
            return GhostViewPort.addGhost(view, viewGroup, matrix);
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)
        }
        return GhostViewApi14.addGhost(view, viewGroup);
    }

    static void removeGhost(View view) {
<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
        if (Build.VERSION.SDK_INT >= 21) {
            GhostViewApi21.removeGhost(view);
=======
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Use the platform implementation on P as we can't backport the shadows drawing.
            GhostViewPlatform.removeGhost(view);
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)
        } else {
            GhostViewApi14.removeGhost(view);
        }
    }

    private GhostViewUtils() {
    }
}
