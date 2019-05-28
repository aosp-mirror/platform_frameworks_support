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

package androidx.camera.extensions;

import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ConfigProvider;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfo;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
/**
 * Unit tests for {@link androidx.camera.extensions.ExtensionsErrorListener}.
 * */
public final class ExtensionsErrorListenerTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    // TODO(b/126431497): This shouldn't need to be static, but the initialization behavior does
    //  not allow us to reinitialize before each test.
    private static FakeCameraFactory sCameraFactory = new FakeCameraFactory();

    static {
        String cameraId = sCameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        sCameraFactory.insertCamera(cameraId,
                new FakeCamera(new FakeCameraInfo(), mock(CameraControl.class)));
    }

    private CountDownLatch mLatch;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        defaultConfigFactory.installDefaultProvider(FakeUseCaseConfig.class,
                new ConfigProvider<FakeUseCaseConfig>() {
                    @Override
                    public FakeUseCaseConfig getConfig(CameraX.LensFacing lensFacing) {
                        return new FakeUseCaseConfig.Builder().build();
                    }
                });
        AppConfig.Builder appConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(sCameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);

        // CameraX.init will actually init just once across all test cases. However we need to get
        // the real CameraFactory instance being injected into the init process.  So here we store
        // the CameraFactory instance in static fields.
        CameraX.init(context, appConfigBuilder.build());

        assumeTrue(CameraUtil.deviceHasCamera());
        mLatch = new CountDownLatch(1);
    }

    @Test
    public void receiveErrorCode_whenOnlyEnableImageCaptureExtender() throws InterruptedException {
        final Boolean[] isErrorCodeReceived = {false};
        ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                new ImageCaptureConfig.Builder().setLensFacing(LensFacing.BACK);
        HdrImageCaptureExtender imageCaptureExtender = HdrImageCaptureExtender.create(
                imageCaptureConfigBuilder);
        imageCaptureExtender.enableExtension();
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());

        PreviewConfig.Builder previewConfigBuilder = new PreviewConfig.Builder().setLensFacing(
                LensFacing.BACK);
        Preview preview = new Preview(previewConfigBuilder.build());

        ExtensionsManager.setExtensionsErrorListener(new ExtensionsErrorListener() {
            @Override
            public void onError(@NonNull ExtensionsErrorCode errorCode) {
                if (errorCode == ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED) {
                    isErrorCodeReceived[0] = true;
                }
            }
        });

        List<UseCase> useCaseList = Arrays.asList(imageCapture, preview);
        ImageCaptureExtender.checkPreviewEnabled(ExtensionsManager.EffectMode.HDR, useCaseList);
        PreviewExtender.checkImageCaptureEnabled(ExtensionsManager.EffectMode.HDR, useCaseList);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertTrue(isErrorCodeReceived[0]);
    }

    @Test
    public void receiveErrorCode_whenOnlyEnablePreviewExtender() throws InterruptedException {
        final Boolean[] isErrorCodeReceived = {false};
        ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                new ImageCaptureConfig.Builder().setLensFacing(LensFacing.BACK);
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());

        PreviewConfig.Builder previewConfigBuilder = new PreviewConfig.Builder().setLensFacing(
                LensFacing.BACK);
        HdrPreviewExtender previewExtender = HdrPreviewExtender.create(previewConfigBuilder);
        previewExtender.enableExtension();
        Preview preview = new Preview(previewConfigBuilder.build());

        ExtensionsManager.setExtensionsErrorListener(new ExtensionsErrorListener() {
            @Override
            public void onError(@NonNull ExtensionsErrorCode errorCode) {
                if (errorCode == ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED) {
                    isErrorCodeReceived[0] = true;
                }
            }
        });

        List<UseCase> useCaseList = Arrays.asList(imageCapture, preview);
        ImageCaptureExtender.checkPreviewEnabled(ExtensionsManager.EffectMode.HDR, useCaseList);
        PreviewExtender.checkImageCaptureEnabled(ExtensionsManager.EffectMode.HDR, useCaseList);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertTrue(isErrorCodeReceived[0]);
    }

    @Test
    public void notReceiveErrorCode_whenEnableBothImageCapturePreviewExtenders()
            throws InterruptedException {
        final Boolean[] isErrorCodeReceived = {false};
        ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                new ImageCaptureConfig.Builder().setLensFacing(LensFacing.BACK);
        HdrImageCaptureExtender imageCaptureExtender = HdrImageCaptureExtender.create(
                imageCaptureConfigBuilder);
        imageCaptureExtender.enableExtension();
        ExtensionsManager.setExtensionsErrorListener(new ExtensionsErrorListener() {
            @Override
            public void onError(@NonNull ExtensionsErrorCode errorCode) {
                if (errorCode == ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED
                        || errorCode == ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED) {
                    isErrorCodeReceived[0] = true;
                }
            }
        });

        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());

        PreviewConfig.Builder previewConfigBuilder = new PreviewConfig.Builder().setLensFacing(
                LensFacing.BACK);
        HdrPreviewExtender previewExtender = HdrPreviewExtender.create(previewConfigBuilder);
        previewExtender.enableExtension();
        Preview preview = new Preview(previewConfigBuilder.build());

        List<UseCase> useCaseList = Arrays.asList(imageCapture, preview);
        ImageCaptureExtender.checkPreviewEnabled(ExtensionsManager.EffectMode.HDR, useCaseList);
        PreviewExtender.checkImageCaptureEnabled(ExtensionsManager.EffectMode.HDR, useCaseList);

        // Waits for one second to wait for error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertFalse(isErrorCodeReceived[0]);
    }
}
