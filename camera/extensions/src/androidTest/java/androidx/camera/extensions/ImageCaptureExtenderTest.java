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

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Pair;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.Config;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class ImageCaptureExtenderTest {
    private static final int STAGE_ID = 0;
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.BACK;
    private ImageCaptureConfig.Builder mBuilder = new ImageCaptureConfig.Builder();
    private ImageCaptureExtender mImageCaptureExtender;
    private FakeImageCaptureExtenderImpl mFakeImageCaptureExtenderImpl =
            new FakeImageCaptureExtenderImpl();
    private FakeLifecycleOwner mLifecycleOwner = new FakeLifecycleOwner();
    private ImageCaptureExtenderImpl mMockImageCaptureExtenderImpl = mock(
            ImageCaptureExtenderImpl.class);
    private ArrayList<CaptureStageImpl> mCaptureStages = new ArrayList<>();

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        mCaptureStages.add(new SettableCaptureStage(STAGE_ID));
        when(mMockImageCaptureExtenderImpl.getCaptureStages()).thenReturn(mCaptureStages);

        Context context = ApplicationProvider.getApplicationContext();
        AppConfig appConfig = Camera2AppConfig.create(context);
        CameraX.init(context, appConfig);

        mBuilder.setLensFacing(LENS_FACING);
        mImageCaptureExtender = FakeImageCaptureExtender.create(mBuilder);
        mFakeImageCaptureExtenderImpl.setCaptureProcessor(mock(CaptureProcessorImpl.class));
        mImageCaptureExtender.init(mBuilder, mFakeImageCaptureExtenderImpl,
                ExtensionsManager.EffectMode.NORMAL);
        mImageCaptureExtender.enableExtension();
    }

    @Test
    @SmallTest
    public void canCheckIsExtensionAvailable() {
        // Extension availability is defaulted to true
        assertThat(mImageCaptureExtender.isExtensionAvailable()).isTrue();
        // Deactivate extension
        mFakeImageCaptureExtenderImpl.setExtensionAvailable(false);
        assertThat(mImageCaptureExtender.isExtensionAvailable()).isFalse();
    }

    @Test
    @SmallTest
    public void canEnableExtension() {
        ImageCaptureConfig config = mBuilder.build();
        assertThat(config.getMaxCaptureStages()).isEqualTo(
                mFakeImageCaptureExtenderImpl.getMaxCaptureStage());
        checkConfigContainsCaptureStages(mBuilder,
                mFakeImageCaptureExtenderImpl.getCaptureStages());
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreGetCaptureStagesBeforeAndAfterInitDeInit() {
        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mMockImageCaptureExtenderImpl, null);
        ImageCaptureConfig.Builder configBuilder =
                new ImageCaptureConfig.Builder().setCaptureBundle(
                        imageCaptureAdapter).setUseCaseEventListener(
                        imageCaptureAdapter).setCaptureProcessor(
                        mock(CaptureProcessor.class));

        ImageCapture useCase = new ImageCapture(configBuilder.build());
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        // To verify the event callbacks in order, and to verification of the getCaptureStages()
        // is also used to wait for the capture session created. The test for the unbind
        // would come after the capture session was created.
        InOrder inOrder = inOrder(mMockImageCaptureExtenderImpl);
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).getCaptureStages();

        // Unbind the use case to test the onDeInit.
        CameraX.unbind(useCase);

        // To verify the deInit should been called.
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mMockImageCaptureExtenderImpl);
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreCameraEventCallbacksBeforeAndAfterInitDeInit() {
        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mMockImageCaptureExtenderImpl, null);
        ImageCaptureConfig.Builder configBuilder =
                new ImageCaptureConfig.Builder().setCaptureBundle(
                        imageCaptureAdapter).setUseCaseEventListener(
                        imageCaptureAdapter).setCaptureProcessor(
                        mock(CaptureProcessor.class));
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(imageCaptureAdapter));


        ImageCapture useCase = new ImageCapture(configBuilder.build());
        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        // To verify the event callbacks in order, and to verification of the onEnableSession()
        // is also used to wait for the capture session created. The test for the unbind
        // would come after the capture session was created.
        InOrder inOrder = inOrder(mMockImageCaptureExtenderImpl);
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onPresetSession();
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onEnableSession();

        // Unbind the use case to test the onDisableSession and onDeInit.
        CameraX.unbind(useCase);

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onDisableSession();
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onDeInit();

        // This test item only focus on onPreset, onEnable and onDisable callback testing,
        // ignore all the getCaptureStages callbacks.
        verify(mMockImageCaptureExtenderImpl, atLeastOnce()).getCaptureStages();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mMockImageCaptureExtenderImpl);
    }

    private void checkConfigContainsCaptureStages(ImageCaptureConfig.Builder builder,
            List<CaptureStageImpl> captureStages) {
        Camera2Config.Builder camera2ConfigurationBuilder =
                new Camera2Config.Builder();

        for (CaptureStageImpl captureStage : captureStages) {
            assertThat(captureStage).isNotNull();
            for (Pair<CaptureRequest.Key, Object> captureParameter :
                    captureStage.getParameters()) {
                camera2ConfigurationBuilder.setCaptureRequestOption(captureParameter.first,
                        captureParameter.second);
            }
        }

        final Camera2Config camera2Config = camera2ConfigurationBuilder.build();

        for (Config.Option<?> option : camera2Config.listOptions()) {
            @SuppressWarnings("unchecked") // Options/values are being copied directly
                    Config.Option<Object> objectOpt = (Config.Option<Object>) option;
            boolean findAMatch = false;
            for (CaptureStage captureStage :
                    builder.build().getCaptureBundle().getCaptureStages()) {
                if (captureStage.getCaptureConfig().getImplementationOptions().containsOption(
                        objectOpt)) {
                    findAMatch = true;
                    break;
                }
            }
            assertThat(findAMatch).isTrue();
            findAMatch = false;
        }
    }
}
