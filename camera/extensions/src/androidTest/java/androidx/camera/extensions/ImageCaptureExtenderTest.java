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

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Pair;

import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ImageCaptureExtenderTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
    }

    @Test
    @LargeTest
    public void extenderLifeCycleTest_noMoreInvokeBeforeAndAfterInitDeInit()
            throws InterruptedException {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();

        ArrayList<CaptureStageImpl> captureStageImpls = new ArrayList<>();
        captureStageImpls.add(new FakeCaptureStage());
        ImageCaptureExtenderImpl mockImageCaptureExtenderImpl = mock(
                ImageCaptureExtenderImpl.class);
        when(mockImageCaptureExtenderImpl.getCaptureStages()).thenReturn(captureStageImpls);

        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mockImageCaptureExtenderImpl);
        ImageCaptureConfig config =
                new ImageCaptureConfig.Builder().setCaptureBundle(
                        imageCaptureAdapter).setUseCaseEventListener(
                        imageCaptureAdapter).setCaptureProcessor(
                        mock(CaptureProcessor.class)).build();

        ImageCapture useCase = new ImageCapture(config);

        CameraX.bindToLifecycle(lifecycle, useCase);
        lifecycle.startAndResume();

        Thread.sleep(3000);

        CameraX.unbind(useCase);

        InOrder inOrder = inOrder(mockImageCaptureExtenderImpl);
        inOrder.verify(mockImageCaptureExtenderImpl).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockImageCaptureExtenderImpl, atLeastOnce()).getCaptureStages();
        inOrder.verify(mockImageCaptureExtenderImpl).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockImageCaptureExtenderImpl);
    }


    final class FakeCaptureStage implements CaptureStageImpl {

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public List<Pair<CaptureRequest.Key, Object>> getParameters() {
            List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();
            return parameters;
        }
    }
}
