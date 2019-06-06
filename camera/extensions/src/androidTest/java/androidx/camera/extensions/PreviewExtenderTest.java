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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.Manifest;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

@RunWith(AndroidJUnit4.class)
public class PreviewExtenderTest {

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

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);

        PreviewExtender.PreviewExtenderAdapter previewExtenderAdapter =
                new PreviewExtender.PreviewExtenderAdapter(mockPreviewExtenderImpl);
        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setUseCaseEventListener(
                previewExtenderAdapter);
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));

        Preview useCase = new Preview(configBuilder.build());

        CameraX.bindToLifecycle(lifecycle, useCase);
        lifecycle.startAndResume();

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        Thread.sleep(3000);

        CameraX.unbind(useCase);

        InOrder inOrder = inOrder(mockPreviewExtenderImpl);
        inOrder.verify(mockPreviewExtenderImpl).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockPreviewExtenderImpl).onPresetSession();
        inOrder.verify(mockPreviewExtenderImpl).onEnableSession();
        inOrder.verify(mockPreviewExtenderImpl).getCaptureStage();
        inOrder.verify(mockPreviewExtenderImpl).onDisableSession();
        inOrder.verify(mockPreviewExtenderImpl).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockPreviewExtenderImpl);
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
