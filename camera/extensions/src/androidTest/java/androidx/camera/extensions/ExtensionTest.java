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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraDevice;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.AppConfig;
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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@LargeTest
public class ExtensionTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private FakeLifecycleOwner mLifecycleOwner;
    private ImageCaptureConfig.Builder mImageCaptureConfigBuilder;
    private PreviewConfig.Builder mPreviewConfigBuilder;
    private CameraDevice.StateCallback mCameraStatusCallback;
    private ExtensionsManager.EffectMode mEffectMode;
    private CameraX.LensFacing mLensFacing;

    @Parameterized.Parameters
    public static Collection getParameters() {
        return Arrays.asList(new Object[][] {
                { ExtensionsManager.EffectMode.BOKEH, CameraX.LensFacing.FRONT },
                { ExtensionsManager.EffectMode.BOKEH, CameraX.LensFacing.BACK },
                { ExtensionsManager.EffectMode.HDR, CameraX.LensFacing.FRONT },
                { ExtensionsManager.EffectMode.HDR, CameraX.LensFacing.BACK },
                { ExtensionsManager.EffectMode.BEAUTY, CameraX.LensFacing.FRONT },
                { ExtensionsManager.EffectMode.BEAUTY, CameraX.LensFacing.BACK },
                { ExtensionsManager.EffectMode.NIGHT, CameraX.LensFacing.FRONT },
                { ExtensionsManager.EffectMode.NIGHT, CameraX.LensFacing.BACK },
                { ExtensionsManager.EffectMode.AUTO, CameraX.LensFacing.FRONT },
                { ExtensionsManager.EffectMode.AUTO, CameraX.LensFacing.BACK }
        });
    }

    public ExtensionTest(ExtensionsManager.EffectMode effectMode, CameraX.LensFacing lensFacing) {
        mEffectMode = effectMode;
        mLensFacing = lensFacing;
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());

        Context context = ApplicationProvider.getApplicationContext();
        AppConfig appConfig = Camera2AppConfig.create(context);
        CameraX.init(context, appConfig);

        mLifecycleOwner = new FakeLifecycleOwner();
        mLifecycleOwner.startAndResume();

        mImageCaptureConfigBuilder = new ImageCaptureConfig.Builder();
        mPreviewConfigBuilder = new PreviewConfig.Builder();

        mCameraStatusCallback = Mockito.mock(CameraDevice.StateCallback.class);
        new Camera2Config.Extender(mImageCaptureConfigBuilder).setDeviceStateCallback(
                mCameraStatusCallback);
    }

    @Test
    public void testCanBindToLifeCycleAndTakePicture() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(mLensFacing));
        assumeTrue(ExtensionsManager.isExtensionAvailable(mEffectMode, mLensFacing));

        enableExtension(mEffectMode, mLensFacing);

        Preview.OnPreviewOutputUpdateListener mockOnPreviewOutputUpdateListener = mock(
                Preview.OnPreviewOutputUpdateListener.class);
        ImageCapture.OnImageCapturedListener mockOnImageCapturedListener = mock(
                ImageCapture.OnImageCapturedListener.class);

        // To test bind/unbind and take picture.
        ImageCapture useCase = new ImageCapture(mImageCaptureConfigBuilder.build());
        Preview useCase1 = new Preview(mPreviewConfigBuilder.build());

        try {
            CameraX.bindToLifecycle(mLifecycleOwner, useCase, useCase1);

            // To set the update listener and Preview will change to active state.
            useCase1.setOnPreviewOutputUpdateListener(mockOnPreviewOutputUpdateListener);
            verify(mockOnPreviewOutputUpdateListener, timeout(3000)).onUpdated(any(
                    Preview.PreviewOutput.class));

            useCase.takePicture(mockOnImageCapturedListener);

            // Verify the image captured.
            ArgumentCaptor<ImageProxy> imageProxy = ArgumentCaptor.forClass(ImageProxy.class);
            verify(mockOnImageCapturedListener, timeout(3000)).onCaptureSuccess(
                    imageProxy.capture(), anyInt());
            assertNotNull(imageProxy.getValue());
            imageProxy.getValue().close(); // Close the image after verification.

            // Verify the take picture should not have any error happen.
            verify(mockOnImageCapturedListener, never()).onError(
                    any(ImageCapture.UseCaseError.class),
                    anyString(), any(Throwable.class));
        } finally {
            CameraX.unbind(useCase, useCase1);

            // Make sure camera was closed.
            verify(mCameraStatusCallback, timeout(3000)).onClosed(any(CameraDevice.class));
        }
    }

    @Test
    public void testEventCallbackInConfig() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(mLensFacing));
        assumeTrue(ExtensionsManager.isExtensionAvailable(mEffectMode, mLensFacing));

        enableExtension(mEffectMode, mLensFacing);

        // Verify Preview config should have related callback.
        PreviewConfig previewConfig = mPreviewConfigBuilder.build();
        assertNotNull(previewConfig.getUseCaseEventListener());
        CameraEventCallbacks callback1 = new Camera2Config(previewConfig).getCameraEventCallback(
                null);
        assertNotNull(callback1);
        assertEquals(callback1.getAllItems().size(), 1);
        assertThat(callback1.getAllItems().get(0)).isInstanceOf(
                PreviewExtender.PreviewExtenderAdapter.class);

        // Verify ImageCapture config should have related callback.
        ImageCaptureConfig imageCaptureConfig = mImageCaptureConfigBuilder.build();
        assertNotNull(imageCaptureConfig.getUseCaseEventListener());
        assertNotNull(imageCaptureConfig.getCaptureBundle());
        CameraEventCallbacks callback2 = new Camera2Config(
                imageCaptureConfig).getCameraEventCallback(null);
        assertNotNull(callback2);
        assertEquals(callback2.getAllItems().size(), 1);
        assertThat(callback2.getAllItems().get(0)).isInstanceOf(
                ImageCaptureExtender.ImageCaptureAdapter.class);
    }

    /**
     * To invoke the enableExtension() method for different effect.
     */
    private void enableExtension(ExtensionsManager.EffectMode effectMode,
            CameraX.LensFacing lensFacing) {

        mImageCaptureConfigBuilder.setLensFacing(lensFacing);
        mPreviewConfigBuilder.setLensFacing(lensFacing);

        ImageCaptureExtender imageCaptureExtender = null;
        PreviewExtender previewExtender = null;

        switch (effectMode) {
            case HDR:
                imageCaptureExtender = HdrImageCaptureExtender.create(mImageCaptureConfigBuilder);
                previewExtender = HdrPreviewExtender.create(mPreviewConfigBuilder);

                // Make sure we are testing on the testlib/Vendor implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(HdrPreviewExtenderImpl.class);
                break;
            case BOKEH:
                imageCaptureExtender = BokehImageCaptureExtender.create(
                        mImageCaptureConfigBuilder);
                previewExtender = BokehPreviewExtender.create(mPreviewConfigBuilder);

                // Make sure we are testing on the testlib/Vendor implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(BokehPreviewExtenderImpl.class);
                break;
            case BEAUTY:
                imageCaptureExtender = BeautyImageCaptureExtender.create(
                        mImageCaptureConfigBuilder);
                previewExtender = BeautyPreviewExtender.create(mPreviewConfigBuilder);

                // Make sure we are testing on the testlib/Vendor implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(BeautyPreviewExtenderImpl.class);
                break;
            case NIGHT:
                imageCaptureExtender = NightImageCaptureExtender.create(mImageCaptureConfigBuilder);
                previewExtender = NightPreviewExtender.create(mPreviewConfigBuilder);

                // Make sure we are testing on the testlib/Vendor implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(NightPreviewExtenderImpl.class);
                break;
            case AUTO:
                imageCaptureExtender = AutoImageCaptureExtender.create(mImageCaptureConfigBuilder);
                previewExtender = AutoPreviewExtender.create(mPreviewConfigBuilder);

                // Make sure we are testing on the testlib/Vendor implementation.
                assertThat(previewExtender.mImpl).isInstanceOf(AutoPreviewExtenderImpl.class);
                break;
        }

        assertNotNull(imageCaptureExtender);
        assertNotNull(previewExtender);

        assertTrue(previewExtender.isExtensionAvailable());
        previewExtender.enableExtension();
        assertTrue(imageCaptureExtender.isExtensionAvailable());
        imageCaptureExtender.enableExtension();
    }
}
