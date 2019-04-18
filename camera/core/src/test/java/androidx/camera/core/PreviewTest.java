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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.Preview.OnPreviewOutputUpdateListener;
import androidx.camera.core.Preview.PreviewOutput;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class PreviewTest {
    // Use most supported resolution for different supported hardware level devices,
    // especially for legacy devices.
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size SECONDARY_RESOLUTION = new Size(320, 240);
    private static final Size TERTIARY_RESOLUTION = new Size(1920, 1080);

    private static final String CAMERA_ID = "0";

    private PreviewConfig mDefaultConfig;
    @Mock
    private OnPreviewOutputUpdateListener mMockListener;
    private FakeLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        // Instantiates OnPreviewOutputUpdateListener before each test run.
        mMockListener = mock(OnPreviewOutputUpdateListener.class);
        Context context = ApplicationProvider.getApplicationContext();

        AppConfig appConfig = FakeAppConfig.create();

        FakeCameraFactory cameraFactory = new FakeCameraFactory();
        cameraFactory.insertCamera(CAMERA_ID, new FakeCamera(CAMERA_ID));

        FakeCameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        surfaceManager.setSuggestedResolution(CAMERA_ID, Preview.class, DEFAULT_RESOLUTION);

        ExtendableUseCaseConfigFactory configFactory = new ExtendableUseCaseConfigFactory();
        configFactory.installDefaultProvider(PreviewConfig.class,
                new ConfigProvider<PreviewConfig>() {
                    @Override
                    public PreviewConfig getConfig(LensFacing lensFacing) {
                        return new PreviewConfig.Builder()
                                .setLensFacing(LensFacing.BACK)
                                .setOptionUnpacker(new SessionConfig.OptionUnpacker() {
                                    @Override
                                    public void unpack(UseCaseConfig<?> config,
                                            SessionConfig.Builder builder) {
                                    }
                                }).build();
                    }
                });

        appConfig = AppConfig.Builder.fromConfig(appConfig)
                .setCameraFactory(cameraFactory)
                .setDeviceSurfaceManager(surfaceManager)
                .setUseCaseConfigFactory(configFactory)
                .build();
        CameraX.init(context, appConfig);

        // init CameraX before creating Preview to get preview size with CameraX's context
        mDefaultConfig = Preview.DEFAULT_CONFIG.getConfig(null);

        mLifecycleOwner = new FakeLifecycleOwner();
    }

    @After
    public void tearDown() {
        Lifecycle.State state = mLifecycleOwner.getLifecycle().getCurrentState();
        if (state.isAtLeast(Lifecycle.State.RESUMED)) {
            mLifecycleOwner.pauseAndStop();
            state = mLifecycleOwner.getLifecycle().getCurrentState();
        }

        if (state.isAtLeast(Lifecycle.State.STARTED)) {
            mLifecycleOwner.stop();
            state = mLifecycleOwner.getLifecycle().getCurrentState();
        }

        if (state.isAtLeast(Lifecycle.State.CREATED)) {
            mLifecycleOwner.destroy();
        }
    }

    @Test
    public void useCaseIsConstructedWithDefaultConfiguration() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(useCase.getSessionConfig(CAMERA_ID).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0)).isNotNull();
    }

    @Test
    public void useCaseIsConstructedWithCustomConfiguration() {
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(LensFacing.BACK).build();
        Preview useCase = new Preview(config);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(useCase.getSessionConfig(CAMERA_ID).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0)).isNotNull();
    }

    @Test
    public void focusRegionCanBeSet() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        CameraControl cameraControl = mock(CameraControl.class);
        useCase.attachCameraControl(CAMERA_ID, cameraControl);

        Rect rect = new Rect(/*left=*/ 200, /*top=*/ 200, /*right=*/ 800, /*bottom=*/ 800);
        useCase.focus(rect, rect, mock(OnFocusListener.class));

        ArgumentCaptor<Rect> rectArgumentCaptor1 = ArgumentCaptor.forClass(Rect.class);
        ArgumentCaptor<Rect> rectArgumentCaptor2 = ArgumentCaptor.forClass(Rect.class);
        verify(cameraControl).focus(rectArgumentCaptor1.capture(), rectArgumentCaptor2.capture(),
                any(OnFocusListener.class), any(Handler.class));
        assertThat(rectArgumentCaptor1.getValue()).isEqualTo(rect);
        assertThat(rectArgumentCaptor2.getValue()).isEqualTo(rect);
    }

    @Test
    public void zoomRegionCanBeSet() {
        Preview useCase = new Preview(mDefaultConfig);

        CameraControl cameraControl = mock(CameraControl.class);
        useCase.attachCameraControl(CAMERA_ID, cameraControl);

        Rect rect = new Rect(/*left=*/ 200, /*top=*/ 200, /*right=*/ 800, /*bottom=*/ 800);
        useCase.zoom(rect);

        ArgumentCaptor<Rect> rectArgumentCaptor = ArgumentCaptor.forClass(Rect.class);
        verify(cameraControl).setCropRegion(rectArgumentCaptor.capture());
        assertThat(rectArgumentCaptor.getValue()).isEqualTo(rect);
    }

    @Test
    public void torchModeCanBeSet() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        useCase.enableTorch(true);

        assertThat(useCase.isTorchOn()).isTrue();
    }

    /**
     * Verifies the SurfaceTexture will not actually be released until the camera is finished.
     *
     * <p>Even if the user has called SurfaceTexture#release(), it should not actually be released
     * until the camera is finished with it.
     */
    @Test
    public void surfaceTextureIsNotReleasedUntilCameraIsFinished()
            throws InterruptedException, ExecutionException, TimeoutException {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        FixedSizeSurfaceTexture surfaceTexture =
                (FixedSizeSurfaceTexture) future0.get(1, TimeUnit.SECONDS);

        // User action to release the surface texture
        surfaceTexture.release();
        boolean wasReleasedAfterUserAction = surfaceTexture.mIsSuperReleased;
        // Use case should no longer be using the surface after stop
        mLifecycleOwner.pauseAndStop();
        boolean wasReleasedAfterCameraFinished = surfaceTexture.mIsSuperReleased;

        assertThat(wasReleasedAfterUserAction).isFalse();
        assertThat(wasReleasedAfterCameraFinished).isTrue();
    }

    /**
     * Verifies the SurfaceTexture will not actually be released until the user releases.
     *
     * <p>Even if the camera has finished using the latest SurfaceTexture, it will not be released
     * until the user calls SurfaceTexture#release().
     */
    @Test
    public void surfaceTextureIsNotReleasedUntilUserReleases()
            throws InterruptedException, ExecutionException, TimeoutException {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        FixedSizeSurfaceTexture surfaceTexture =
                (FixedSizeSurfaceTexture) future0.get(1, TimeUnit.SECONDS);

        // Use case should no longer be using the surface after stop
        mLifecycleOwner.pauseAndStop();
        boolean wasReleasedBeforeUserAction = surfaceTexture.mIsSuperReleased;
        // User action to release the surface texture
        surfaceTexture.release();
        boolean isReleasedAfterUserAction = surfaceTexture.mIsSuperReleased;

        assertThat(wasReleasedBeforeUserAction).isFalse();
        assertThat(isReleasedAfterUserAction).isTrue();
    }

    @Test
    public void listenedSurfaceTextureIsNotReleased_whenCleared()
            throws InterruptedException, ExecutionException, TimeoutException {
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future = new FutureTask<>(surfaceTextureCallable);

        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future.run();
                    }
                });

        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();
        FixedSizeSurfaceTexture surfaceTexture =
                (FixedSizeSurfaceTexture) future.get(1, TimeUnit.SECONDS);

        useCase.clear();

        assertThat(surfaceTexture.mIsSuperReleased).isFalse();
    }

    @Test
    public void surfaceTexture_isListenedOnlyOnce()
            throws InterruptedException, ExecutionException, TimeoutException {

        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        // Ensure the output is created
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        SurfaceTexture surfaceTexture0 = future0.get();

        // Setting a new OnPreviewOutputUpdateListener should create a new SurfaceTexture
        final SurfaceTextureCallable surfaceTextureCallable1 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future1 = new FutureTask<>(surfaceTextureCallable1);
        useCase.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable1.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future1.run();
                    }
                });

        SurfaceTexture surfaceTexture1 = future1.get(1, TimeUnit.SECONDS);

        assertThat(surfaceTexture0).isNotSameAs(surfaceTexture1);
    }

    @Test
    public void updateSessionConfigWithSuggestedResolution()
            throws ExecutionException, InterruptedException {
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(LensFacing.BACK).build();
        Preview useCase = new Preview(config);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final Size[] sizes = {SECONDARY_RESOLUTION, TERTIARY_RESOLUTION};

        // Initial resolution should be set by initially connecting to the camera.
        Surface previousSurface = useCase.getSessionConfig(CAMERA_ID).getSurfaces().get(
                0).getSurface().get();
        for (Size size : sizes) {
            useCase.updateSuggestedResolution(Collections.singletonMap(CAMERA_ID, size));

            List<Surface> surfaces =
                    DeferrableSurfaces.surfaceList(
                            useCase.getSessionConfig(CAMERA_ID).getSurfaces());


            assertWithMessage("Failed at Size: " + size).that(surfaces).hasSize(1);
            assertWithMessage("Failed at Size: " + size).that(surfaces.get(0)).isNotNull();
            Surface surface = surfaces.get(0);
            assertThat(surface).isNotSameAs(previousSurface);
            previousSurface = surface;
        }
    }

    @Test
    public void previewOutputListenerCanBeSetAndRetrieved() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        Preview.OnPreviewOutputUpdateListener previewOutputListener =
                useCase.getOnPreviewOutputUpdateListener();
        useCase.setOnPreviewOutputUpdateListener(mMockListener);

        OnPreviewOutputUpdateListener retrievedPreviewOutputListener =
                useCase.getOnPreviewOutputUpdateListener();

        assertThat(previewOutputListener).isNull();
        assertThat(retrievedPreviewOutputListener).isSameAs(mMockListener);
    }

    @Test
    public void clear_removePreviewOutputListener() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        useCase.setOnPreviewOutputUpdateListener(mMockListener);
        useCase.clear();

        assertThat(useCase.getOnPreviewOutputUpdateListener()).isNull();
    }

    @Test
    public void previewOutput_isResetOnUpdatedResolution() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final AtomicInteger calledCount = new AtomicInteger(0);
        useCase.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        calledCount.incrementAndGet();
                    }
                });

        int initialCount = calledCount.get();

        useCase.updateSuggestedResolution(
                Collections.singletonMap(CAMERA_ID, SECONDARY_RESOLUTION));

        int countAfterUpdate = calledCount.get();

        assertThat(initialCount).isEqualTo(1);
        assertThat(countAfterUpdate).isEqualTo(2);
    }

    @Test
    public void previewOutput_updatesWithTargetRotation() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.setTargetRotation(Surface.ROTATION_0);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final AtomicReference<PreviewOutput> latestPreviewOutput = new AtomicReference<>();
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        latestPreviewOutput.set(previewOutput);
                    }
                });

        Preview.PreviewOutput initialOutput = latestPreviewOutput.get();

        useCase.setTargetRotation(Surface.ROTATION_90);

        assertThat(initialOutput).isNotNull();
        assertThat(initialOutput.getSurfaceTexture())
                .isEqualTo(latestPreviewOutput.get().getSurfaceTexture());
        assertThat(initialOutput.getRotationDegrees())
                .isNotEqualTo(latestPreviewOutput.get().getRotationDegrees());
    }

    @Test
    public void previewOutput_isResetByReleasedSurface()
            throws InterruptedException, ExecutionException {
        final Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);

        final SurfaceTextureCallable surfaceTextureCallable1 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future1 = new FutureTask<>(surfaceTextureCallable1);
        useCase.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
                        SurfaceTexture surfaceTexture = previewOutput.getSurfaceTexture();
                        if (!future0.isDone()) {
                            // Release the surface texture
                            surfaceTexture.release();
                            surfaceTextureCallable0.setSurfaceTexture(surfaceTexture);
                            future0.run();
                            return;
                        }

                        surfaceTextureCallable1.setSurfaceTexture(surfaceTexture);
                        future1.run();
                    }
                });

        // Wait for the surface texture to be released
        SurfaceTexture surfaceTexture0 = future0.get();

        // Cause the surface to reset. CheckedSurfaceTexture will detect the SurfaceTexture is
        // released and create a new SurfaceTexture.
        useCase.getSessionConfig(CAMERA_ID).getSurfaces().get(0).getSurface().get();

        // Wait for the surface to reset
        SurfaceTexture surfaceTexture1 = future1.get();

        assertThat(surfaceTexture0).isNotNull();
        assertThat(surfaceTexture1).isNotNull();
        assertThat(surfaceTexture0).isNotSameAs(surfaceTexture1);
    }

    @Test
    public void outputIsPublished_whenListenerIsSetBefore()
            throws InterruptedException, ExecutionException {

        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
    }

    @Test
    public void outputIsPublished_whenListenerIsSetAfter()
            throws InterruptedException, ExecutionException {

        Preview useCase = new Preview(mDefaultConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);

        useCase.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
    }

    private static final class SurfaceTextureCallable implements Callable<SurfaceTexture> {
        SurfaceTexture mSurfaceTexture;

        void setSurfaceTexture(SurfaceTexture surfaceTexture) {
            this.mSurfaceTexture = surfaceTexture;
        }

        @Override
        public SurfaceTexture call() {
            return mSurfaceTexture;
        }
    }
}
