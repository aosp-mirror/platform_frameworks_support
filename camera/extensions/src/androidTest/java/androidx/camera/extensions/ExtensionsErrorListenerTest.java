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

import android.Manifest;

import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
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
 * Unit tests for {@link androidx.camera.extensions.ExtensionsErrorListener}.
 * */
public final class ExtensionsErrorListenerTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private FakeLifecycleOwner mLifecycle;
    private CountDownLatch mLatch;

    @Before
    public void setUp() {
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
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());

        PreviewConfig.Builder previewConfigBuilder = new PreviewConfig.Builder().setLensFacing(
                LensFacing.BACK);
        Preview preview = new Preview(previewConfigBuilder.build());

        ExtensionsManager.setExtensionsErrorListener(new ExtensionsErrorListener() {
            @Override
            public void onError(ExtensionsErrorCode errorCode) {
                if (errorCode == ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED) {
                    isErrorCodeReceived[0] = true;
                }
            }
        });

        CameraX.bindToLifecycle(mLifecycle, imageCapture, preview);
        mLifecycle.startAndResume();

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
            public void onError(ExtensionsErrorCode errorCode) {
                if (errorCode == ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED) {
                    isErrorCodeReceived[0] = true;
                }
            }
        });

        CameraX.bindToLifecycle(mLifecycle, imageCapture, preview);
        mLifecycle.startAndResume();

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
            public void onError(ExtensionsErrorCode errorCode) {
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

        CameraX.bindToLifecycle(mLifecycle, imageCapture, preview);
        mLifecycle.startAndResume();

        // Waits for one second to wait for error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertFalse(isErrorCodeReceived[0]);
    }
}
