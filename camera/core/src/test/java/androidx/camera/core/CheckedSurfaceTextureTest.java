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

import static com.google.common.truth.Truth.assertThat;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.camera.core.CheckedSurfaceTexture.OnTextureChangedListener;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CheckedSurfaceTextureTest {

    private Size mDefaultResolution;
    private CheckedSurfaceTexture mCheckedSurfaceTexture;
    private SurfaceTexture mLatestSurfaceTexture;
    private final CheckedSurfaceTexture.OnTextureChangedListener mTextureChangedListener =
            new OnTextureChangedListener() {
                @Override
                public void onTextureChanged(
                        @Nullable SurfaceTexture newOutput, @Nullable Size newResolution) {
                    mLatestSurfaceTexture = newOutput;
                }
            };

    @Before
    public void setup() {
        mDefaultResolution = new Size(640, 480);
        mCheckedSurfaceTexture =
                new CheckedSurfaceTexture(mTextureChangedListener);
        mCheckedSurfaceTexture.setResolution(mDefaultResolution);
    }

    @Test
    public void previewOutputUpdatesWhenReset() {
        // Create the initial surface texture
        mCheckedSurfaceTexture.resetSurfaceTexture();

        // Surface texture should have been set
        SurfaceTexture initialOutput = mLatestSurfaceTexture;

        // Create a new surface texture
        mCheckedSurfaceTexture.resetSurfaceTexture();

        assertThat(initialOutput).isNotNull();
        assertThat(mLatestSurfaceTexture).isNotNull();
        assertThat(mLatestSurfaceTexture).isNotEqualTo(initialOutput);
    }

    @Test
    public void getUpdatedSurfaceTextureWhenReleased() throws InterruptedException,
            ExecutionException {
        mCheckedSurfaceTexture.resetSurfaceTexture();

        ListenableFuture<Surface> surfaceFuture = mCheckedSurfaceTexture.getOrCreateSurface();
        surfaceFuture.get();
        SurfaceTexture surfaceTexture1 = mLatestSurfaceTexture;
        mLatestSurfaceTexture.release();

        ListenableFuture<Surface> surfaceFuture2 = mCheckedSurfaceTexture.getOrCreateSurface();
        surfaceFuture2.get();
        SurfaceTexture surfaceTexture2 = mLatestSurfaceTexture;

        assertThat(surfaceTexture1).isNotSameAs(surfaceTexture2);

    }

    @Test
    public void surfaceTextureIsReleasedByOwnerLaterWhenDetached() throws InterruptedException,
            ExecutionException {
        mCheckedSurfaceTexture.notifySurfaceAttached();
        mCheckedSurfaceTexture.resetSurfaceTexture();
        ListenableFuture<Surface> surfaceFuture = mCheckedSurfaceTexture.getOrCreateSurface();
        surfaceFuture.get();
        FixedSizeSurfaceTexture surfaceTexture = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;

        surfaceTexture.release();

        assertThat(surfaceTexture.mIsSuperReleased).isFalse();

        mCheckedSurfaceTexture.notifySurfaceDetached();
        Thread.sleep(100);

        assertThat(surfaceTexture.mIsSuperReleased).isTrue();
    }

    @Test
    public void releaseCheckedSurfaceTexture() throws InterruptedException, ExecutionException {
        mCheckedSurfaceTexture.notifySurfaceAttached();
        mCheckedSurfaceTexture.resetSurfaceTexture();
        ListenableFuture<Surface> surfaceFuture = mCheckedSurfaceTexture.getOrCreateSurface();
        surfaceFuture.get();
        FixedSizeSurfaceTexture surfaceTexture = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;


        mCheckedSurfaceTexture.release();
        assertThat(surfaceTexture.mIsSuperReleased).isFalse();


        mCheckedSurfaceTexture.notifySurfaceDetached();
        Thread.sleep(100);

        assertThat(surfaceTexture.mIsSuperReleased).isTrue();

    }

    @Test
    public void surfaceMapAfterResetSurfaceTexture()
            throws InterruptedException, ExecutionException {
        mCheckedSurfaceTexture.notifySurfaceAttached();

        mCheckedSurfaceTexture.resetSurfaceTexture();
        Surface surface = mCheckedSurfaceTexture.getOrCreateSurface().get();
        FixedSizeSurfaceTexture surfaceTexture = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;

        assertThat(mCheckedSurfaceTexture.mResourceMap.size()).isEqualTo(1);
        CheckedSurfaceTexture.Resource resource = mCheckedSurfaceTexture.mResourceMap.get(
                surfaceTexture);
        assertThat(resource.mSurfaceTexture).isSameAs(surfaceTexture);
        assertThat(resource.mSurface).isSameAs(surface);


        mCheckedSurfaceTexture.resetSurfaceTexture();
        Surface surface2 = mCheckedSurfaceTexture.getOrCreateSurface().get();
        FixedSizeSurfaceTexture surfaceTexture2 = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;
        assertThat(mCheckedSurfaceTexture.mResourceMap.size()).isEqualTo(2);

        mCheckedSurfaceTexture.notifySurfaceDetached();
        Thread.sleep(100);

        assertThat(mCheckedSurfaceTexture.mResourceMap.size()).isEqualTo(1);
        CheckedSurfaceTexture.Resource resource2 = mCheckedSurfaceTexture.mResourceMap.get(
                surfaceTexture2);
        assertThat(resource2.mSurfaceTexture).isSameAs(surfaceTexture2);
        assertThat(resource2.mSurface).isSameAs(surface2);

        assertThat(mCheckedSurfaceTexture.mResourceMap.get(surfaceTexture)).isNull();
    }

    @Test
    public void surfaceMapAfterSurfaceTextureReleased()
            throws InterruptedException, ExecutionException {
        mCheckedSurfaceTexture.notifySurfaceAttached();

        mCheckedSurfaceTexture.resetSurfaceTexture();
        Surface surface = mCheckedSurfaceTexture.getOrCreateSurface().get();
        FixedSizeSurfaceTexture surfaceTexture = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;
        assertThat(mCheckedSurfaceTexture.mResourceMap.size()).isEqualTo(1);

        surfaceTexture.release();

        assertThat(mCheckedSurfaceTexture.mResourceMap.size()).isEqualTo(1);

        mCheckedSurfaceTexture.notifySurfaceDetached();
        Thread.sleep(100);

        assertThat(mCheckedSurfaceTexture.mResourceMap.size()).isEqualTo(0);
    }

    @Test
    public void getRecentSurfaceBeforeAndAfterGetOrCreateSurface()
            throws InterruptedException, ExecutionException {

        Surface recentSurface1 = mCheckedSurfaceTexture.getRecentSurface();
        assertThat(recentSurface1).isNull();

        Surface surface = mCheckedSurfaceTexture.getOrCreateSurface().get();
        Surface recentSurface2 = mCheckedSurfaceTexture.getRecentSurface();
        assertThat(recentSurface2).isEqualTo(surface);
    }

    @Test
    public void getRecentSurfaceTwiceReturnsSameSurface()
            throws InterruptedException, ExecutionException {

        Surface surface = mCheckedSurfaceTexture.getOrCreateSurface().get();

        Surface recentSurface1 = mCheckedSurfaceTexture.getRecentSurface();
        Surface recentSurface2 = mCheckedSurfaceTexture.getRecentSurface();
        assertThat(recentSurface1).isEqualTo(surface);
        assertThat(recentSurface1).isEqualTo(recentSurface2);

    }

    @Test
    public void getOrCreateSurfaceCreateNewSurface()
            throws InterruptedException, ExecutionException {

        Surface surface1 = mCheckedSurfaceTexture.getOrCreateSurface().get();
        Surface recentSurface1 = mCheckedSurfaceTexture.getRecentSurface();
        assertThat(surface1).isEqualTo(recentSurface1);

        Surface surface2 = mCheckedSurfaceTexture.getOrCreateSurface().get();
        Surface recentSurface2 = mCheckedSurfaceTexture.getRecentSurface();

        // calls getOrCreateSurface() again will create new Surface
        assertThat(surface2).isNotEqualTo(recentSurface1);
        assertThat(surface2).isEqualTo(recentSurface2);

    }
}
