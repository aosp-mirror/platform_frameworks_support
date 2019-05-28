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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.Manifest;
import android.content.Context;

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
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfo;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
/**
 * Unit tests for {@link androidx.camera.extensions.ImageCaptureExtender} class.
 * */
public final class ImageCaptureExtenderTest {
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

    private FakeLifecycleOwner mLifecycle;
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
        mLifecycle = new FakeLifecycleOwner();

        mLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() throws InterruptedException {
        CameraX.unbindAll();

        // Wait some time for the cameras to close. We need the cameras to close to bring CameraX
        // back
        // to the initial state.
        Thread.sleep(3000);
    }

    @Test
    public void receiveErrorCode_whenOnlyEnableImageCaptureExtender() throws InterruptedException {
        final Boolean[] isErrorCodeReceived = {false};
        ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                new ImageCaptureConfig.Builder().setLensFacing(LensFacing.BACK);
        HdrImageCaptureExtender imageCaptureExtender = HdrImageCaptureExtender.create(
                imageCaptureConfigBuilder);
        imageCaptureExtender.enableExtension();
        imageCaptureExtender.setExtenderErrorListener(new ExtenderErrorListener() {
            @Override
            public void onError(ExtenderErrorCode errorCore) {
                if (errorCore == ExtenderErrorCode.PREVIEW_EXTENSION_REQUIRED) {
                    isErrorCodeReceived[0] = true;
                }
            }
        });

        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());

        PreviewConfig.Builder previewConfigBuilder = new PreviewConfig.Builder().setLensFacing(
                LensFacing.BACK);
        Preview preview = new Preview(previewConfigBuilder.build());

        CameraX.bindToLifecycle(mLifecycle, imageCapture, preview);
        mLifecycle.startAndResume();

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(isErrorCodeReceived[0]).isTrue();
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
        imageCaptureExtender.setExtenderErrorListener(new ExtenderErrorListener() {
            @Override
            public void onError(ExtenderErrorCode errorCore) {
                if (errorCore == ExtenderErrorCode.PREVIEW_EXTENSION_REQUIRED) {
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

        CameraX.bindToLifecycle(mLifecycle, imageCapture, preview);
        mLifecycle.startAndResume();

        // Waits for one second to wait for error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(isErrorCodeReceived[0]).isFalse();
    }
}
