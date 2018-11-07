/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media.widget;

import android.view.Surface;
import android.view.View;

import androidx.media2.XMediaPlayer;

interface VideoViewInterface {
    void setSurfaceListener(SurfaceListener l);
    int getViewType();
    void setMediaPlayer(XMediaPlayer mp);

    /**
     * Indicates if the view's surface is available.
     *
     * @return true if the surface is available.
     */
    boolean hasAvailableSurface();
    Surface getSurface();

    /**
     * An instance of VideoViewInterface calls these surface notification methods accordingly if
     * a listener has been registered via {@link #setSurfaceListener(SurfaceListener)}.
     */
    interface SurfaceListener {
        void onSurfaceCreated(View view, int width, int height);
        void onSurfaceDestroyed(View view);
        void onSurfaceChanged(View view, int width, int height);
        void onSurfaceTakeOverDone(VideoViewInterface view);
    }
}
