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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
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
import android.hardware.camera2.CaptureResult;
import android.util.Pair;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraCaptureResults;
import androidx.camera.core.CameraX;
import androidx.camera.core.Config;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageInfoProcessor;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class PreviewExtenderTest {
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.BACK;
    private PreviewConfig.Builder mBuilder = new PreviewConfig.Builder();
    private PreviewExtender mPreviewExtender;
    private FakePreviewExtenderImpl mFakePreviewExtenderImpl =
            new FakePreviewExtenderImpl();
    private FakeLifecycleOwner mLifecycleOwner = new FakeLifecycleOwner();
    private PreviewExtenderImpl mMockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());

        Context context = ApplicationProvider.getApplicationContext();
        AppConfig appConfig = Camera2AppConfig.create(context);
        CameraX.init(context, appConfig);

        mBuilder.setLensFacing(LENS_FACING);
        mPreviewExtender = FakePreviewExtender.create(mBuilder);
        mFakePreviewExtenderImpl.setProcessorType(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR);
        mPreviewExtender.init(mBuilder, mFakePreviewExtenderImpl,
                ExtensionsManager.EffectMode.NORMAL);
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreInvokeBeforeAndAfterInitDeInit() {
        PreviewExtender.PreviewExtenderAdapter previewExtenderAdapter =
                new PreviewExtender.PreviewExtenderAdapter(mMockPreviewExtenderImpl, null);
        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setUseCaseEventListener(
                previewExtenderAdapter).setLensFacing(CameraX.LensFacing.BACK);
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));

        Preview useCase = new Preview(configBuilder.build());

        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        // To set the update listener and Preview will change to active state.
        useCase.setOnPreviewOutputUpdateListener(mock(Preview.OnPreviewOutputUpdateListener.class));

        // To verify the call in order after bind to life cycle, and to verification of the
        // getCaptureStages() is also used to wait for the capture session created. The test for
        // the unbind would come after the capture session was created.
        InOrder inOrder = inOrder(mMockPreviewExtenderImpl);
        inOrder.verify(mMockPreviewExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mMockPreviewExtenderImpl, timeout(3000)).onPresetSession();
        inOrder.verify(mMockPreviewExtenderImpl, timeout(3000)).onEnableSession();
        inOrder.verify(mMockPreviewExtenderImpl, timeout(3000)).getCaptureStage();

        // Unbind the use case to test the onDisableSession and onDeInit.
        CameraX.unbind(useCase);

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mMockPreviewExtenderImpl, timeout(3000)).onDisableSession();
        inOrder.verify(mMockPreviewExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mMockPreviewExtenderImpl);
    }

    @Test
    @MediumTest
    public void getCaptureStagesTest_shouldSetToRepeatingRequest() {
        ImageInfoProcessor mockImageInfoProcessor = mock(ImageInfoProcessor.class);

        // Set up a result for getCaptureStages() testing.
        List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();
        parameters.add(Pair.create(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD));
        CaptureStageImpl mockCaptureStageImpl = mock(CaptureStageImpl.class);
        when(mockCaptureStageImpl.getParameters()).thenReturn(parameters);

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mockPreviewExtenderImpl.getCaptureStage()).thenReturn(mockCaptureStageImpl);

        PreviewExtender.PreviewExtenderAdapter previewExtenderAdapter =
                new PreviewExtender.PreviewExtenderAdapter(mockPreviewExtenderImpl, null);
        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setUseCaseEventListener(
                previewExtenderAdapter).setLensFacing(CameraX.LensFacing.BACK);
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));
        configBuilder.setImageInfoProcessor(mockImageInfoProcessor);

        Preview useCase = new Preview(configBuilder.build());

        CameraX.bindToLifecycle(mLifecycleOwner, useCase);
        mLifecycleOwner.startAndResume();

        // To set the update listener and Preview will change to active state.
        useCase.setOnPreviewOutputUpdateListener(mock(Preview.OnPreviewOutputUpdateListener.class));

        ArgumentCaptor<ImageInfo> imageInfo = ArgumentCaptor.forClass(ImageInfo.class);
        verify(mockImageInfoProcessor, timeout(3000)).process(imageInfo.capture());
        CameraCaptureResult result = CameraCaptureResults.retrieveCameraCaptureResult(
                imageInfo.getValue());
        assertNotNull(result);

        CaptureResult captureResult = Camera2CameraCaptureResultConverter.getCaptureResult(result);
        assertNotNull(captureResult);

        // To verify the capture result should include the parameter of the getCaptureStages().
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_EFFECT_MODE)).isEqualTo(
                CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD);

        CameraX.unbind(useCase);
        verify(mockPreviewExtenderImpl, timeout(3000)).onDeInit();
    }

    @Test
    @SmallTest
    public void canCheckIsExtensionAvailable() {
        // Extension availability is defaulted to true
        assertThat(mPreviewExtender.isExtensionAvailable()).isTrue();
        // Deactivate extension
        mFakePreviewExtenderImpl.setExtensionAvailable(false);
        assertThat(mPreviewExtender.isExtensionAvailable()).isFalse();
    }

    @Test
    @SmallTest
    public void canEnableExtension() {
        mPreviewExtender.enableExtension();
        CaptureStageImpl captureStage = mFakePreviewExtenderImpl.getCaptureStage();
        assertThat(captureStage).isNotNull();

        Camera2Config.Builder camera2ConfigurationBuilder =
                new Camera2Config.Builder();

        for (Pair<CaptureRequest.Key, Object> captureParameter :
                captureStage.getParameters()) {
            camera2ConfigurationBuilder.setCaptureRequestOption(captureParameter.first,
                    captureParameter.second);
        }

        final Camera2Config camera2Config = camera2ConfigurationBuilder.build();

        for (Config.Option<?> option : camera2Config.listOptions()) {
            @SuppressWarnings("unchecked") // Options/values are being copied directly
                    Config.Option<Object> objectOpt = (Config.Option<Object>) option;
            assertThat(mBuilder.getMutableConfig().containsOption(objectOpt)).isTrue();
        }
    }
}
