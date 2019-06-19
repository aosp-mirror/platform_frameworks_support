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

package androidx.camera.extensions;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.graphics.SurfaceTexture;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class VendorExtensionTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static final int TIME_TO_WAIT_FOR_DATA_SECONDS = 3;

    private FakeLifecycleOwner mLifecycleOwner;
    private CountDownLatch mDataLatch;

    /**
     * OnImageCapturedListener that unlocks the latch waiting for the first image data to appear.
     */
    private final ImageCapture.OnImageCapturedListener mOnImageAvailableListener =
            new ImageCapture.OnImageCapturedListener() {
                @Override
                public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                    if (image != null) {
                        image.close();
                        mDataLatch.countDown();
                    }
                }
            };

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());

        mLifecycleOwner = new FakeLifecycleOwner();
        mLifecycleOwner.startAndResume();

        mDataLatch = new CountDownLatch(1);
    }

    @Test
    public void testBokehExtender() throws InterruptedException {
        for (CameraX.LensFacing facing : getAvailableCameraLensFacing()) {
            if (ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.BOKEH,
                    facing)) {
                ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                        new ImageCaptureConfig.Builder().setLensFacing(facing);
                PreviewConfig.Builder previewConfigBuilder =
                        new PreviewConfig.Builder().setLensFacing(facing);

                BokehImageCaptureExtender imageCaptureExtender = BokehImageCaptureExtender.create(
                        imageCaptureConfigBuilder);
                BokehPreviewExtender previewExtender = BokehPreviewExtender.create(
                        previewConfigBuilder);

                // Make sure we are testing on the testlib implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(BokehPreviewExtenderImpl.class);

                testExtension(previewExtender, imageCaptureExtender, previewConfigBuilder,
                        imageCaptureConfigBuilder);
            }
        }
    }

    @Test
    public void testHdrExtender() throws InterruptedException {
        for (CameraX.LensFacing facing : getAvailableCameraLensFacing()) {
            if (ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.HDR, facing)) {

                ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                        new ImageCaptureConfig.Builder().setLensFacing(facing);
                PreviewConfig.Builder previewConfigBuilder =
                        new PreviewConfig.Builder().setLensFacing(facing);

                HdrImageCaptureExtender imageCaptureExtender = HdrImageCaptureExtender.create(
                        imageCaptureConfigBuilder);
                HdrPreviewExtender previewExtender = HdrPreviewExtender.create(
                        previewConfigBuilder);

                // Make sure we are testing on the testlib implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(HdrPreviewExtenderImpl.class);

                testExtension(previewExtender, imageCaptureExtender, previewConfigBuilder,
                        imageCaptureConfigBuilder);
            }
        }
    }

    @Test
    public void testBeautyExtender() throws InterruptedException {
        for (CameraX.LensFacing facing : getAvailableCameraLensFacing()) {
            if (ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.BEAUTY,
                    facing)) {

                ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                        new ImageCaptureConfig.Builder().setLensFacing(facing);
                PreviewConfig.Builder previewConfigBuilder =
                        new PreviewConfig.Builder().setLensFacing(facing);

                BeautyImageCaptureExtender imageCaptureExtender = BeautyImageCaptureExtender.create(
                        imageCaptureConfigBuilder);
                BeautyPreviewExtender previewExtender = BeautyPreviewExtender.create(
                        previewConfigBuilder);

                // Make sure we are testing on the testlib implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(BeautyPreviewExtenderImpl.class);

                testExtension(previewExtender, imageCaptureExtender, previewConfigBuilder,
                        imageCaptureConfigBuilder);
            }
        }
    }

    @Test
    public void testNightExtender() throws InterruptedException {
        for (CameraX.LensFacing facing : getAvailableCameraLensFacing()) {
            if (ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.NIGHT,
                    facing)) {

                ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                        new ImageCaptureConfig.Builder().setLensFacing(facing);
                PreviewConfig.Builder previewConfigBuilder =
                        new PreviewConfig.Builder().setLensFacing(facing);

                NightImageCaptureExtender imageCaptureExtender = NightImageCaptureExtender.create(
                        imageCaptureConfigBuilder);
                NightPreviewExtender previewExtender = NightPreviewExtender.create(
                        previewConfigBuilder);

                // Make sure we are testing on the testlib implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(NightPreviewExtenderImpl.class);

                testExtension(previewExtender, imageCaptureExtender, previewConfigBuilder,
                        imageCaptureConfigBuilder);
            }
        }
    }

    @Test
    public void testAutoExtender() throws InterruptedException {
        for (CameraX.LensFacing facing : getAvailableCameraLensFacing()) {
            if (ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.AUTO, facing)) {

                ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                        new ImageCaptureConfig.Builder().setLensFacing(facing);
                PreviewConfig.Builder previewConfigBuilder =
                        new PreviewConfig.Builder().setLensFacing(facing);

                AutoImageCaptureExtender imageCaptureExtender = AutoImageCaptureExtender.create(
                        imageCaptureConfigBuilder);
                AutoPreviewExtender previewExtender = AutoPreviewExtender.create(
                        previewConfigBuilder);

                // Make sure we are testing on the testlib implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(AutoPreviewExtenderImpl.class);

                testExtension(previewExtender, imageCaptureExtender, previewConfigBuilder,
                        imageCaptureConfigBuilder);
            }
        }
    }

    /**
     * To invoke the enableExtension() method and verify the EventListener and
     * CallbackEventListener instance was added in config builder.
     */
    private void testExtension(PreviewExtender previewExtender,
            ImageCaptureExtender imageCaptureExtender, PreviewConfig.Builder previewBuilder,
            ImageCaptureConfig.Builder imageCaptureBuilder)
            throws InterruptedException {

        // Enable the PreviewExtension and verify the callback was set to config.
        assertTrue(previewExtender.isExtensionAvailable());
        previewExtender.enableExtension();


        PreviewConfig previewConfig = previewBuilder.build();
        assertNotNull(previewConfig.getUseCaseEventListener());
        CameraEventCallbacks callback1 = new Camera2Config(previewConfig).getCameraEventCallback(
                null);
        assertNotNull(callback1);
        assertEquals(callback1.getAllItems().size(), 1);
        assertThat(callback1.getAllItems().get(0)).isInstanceOf(
                PreviewExtender.PreviewExtenderAdapter.class);

        // Enable the ImageCaptureExtension and verify the callback was set to config.
        assertTrue(imageCaptureExtender.isExtensionAvailable());
        imageCaptureExtender.enableExtension();

        ImageCaptureConfig imageCaptureConfig = imageCaptureBuilder.build();
        assertNotNull(imageCaptureConfig.getUseCaseEventListener());
        assertNotNull(imageCaptureConfig.getCaptureBundle());
        CameraEventCallbacks callback2 = new Camera2Config(
                imageCaptureConfig).getCameraEventCallback(null);
        assertNotNull(callback2);
        assertEquals(callback2.getAllItems().size(), 1);
        assertThat(callback2.getAllItems().get(0)).isInstanceOf(
                ImageCaptureExtender.ImageCaptureAdapter.class);

        // To test bind/unbind and take picture.
        ImageCapture useCase = new ImageCapture(imageCaptureConfig);
        Preview useCase1 = new Preview(previewConfig);
        CameraX.bindToLifecycle(mLifecycleOwner, useCase, useCase1);

        final SurfaceTextureCallable
                surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase1.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        useCase.takePicture(mOnImageAvailableListener);
        waitForData();
        CameraX.unbind(useCase, useCase1);
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

    /**
     * Wait for data to get produced by capture event.
     *
     * @throws InterruptedException if data is not produced after a set amount of time.
     */
    private void waitForData() throws InterruptedException {
        mDataLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
    }

    private Set<CameraX.LensFacing> getAvailableCameraLensFacing() {
        // Start with all camera directions.
        Set<CameraX.LensFacing> available = new LinkedHashSet<>(
                Arrays.asList(CameraX.LensFacing.values()));

        if (!hasCameraWithLensFacing(CameraX.LensFacing.BACK)) {
            available.remove(CameraX.LensFacing.BACK);
        }

        if (!hasCameraWithLensFacing(CameraX.LensFacing.FRONT)) {
            available.remove(CameraX.LensFacing.FRONT);
        }

        return available;
    }

    private boolean hasCameraWithLensFacing(CameraX.LensFacing lensFacing) {
        String cameraId;
        try {
            cameraId = CameraX.getCameraWithLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to query lens facing.", e);
        }

        return cameraId != null;
    }
}
