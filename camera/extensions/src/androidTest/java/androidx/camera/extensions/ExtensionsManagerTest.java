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
import static org.mockito.Mockito.mock;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraX;
import androidx.camera.core.ConfigProvider;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfo;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link androidx.camera.extensions.ExtensionsManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionsManagerTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    // TODO(b/126431497): This shouldn't need to be static, but the initialization behavior does
    //  not allow us to reinitialize before each test.
    private static FakeCameraFactory sCameraFactory = new FakeCameraFactory();

    static {
        String cameraId = sCameraFactory.cameraIdForLensFacing(CameraX.LensFacing.BACK);
        sCameraFactory.insertCamera(cameraId,
                new FakeCamera(new FakeCameraInfo(), mock(CameraControlInternal.class)));
    }

    private CountDownLatch mLatch;
    FakeLifecycleOwner mFakeLifecycleOwner = new FakeLifecycleOwner();

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

    @After
    public void tearDown() {
        CameraX.unbindAll();
    }

    @Test
    public void canEanbleExtension_whenUseCaseGroupIsEmpty() {
        ExtensionsManager.enableExtension(EffectMode.HDR, mFakeLifecycleOwner);
        EffectMode effectMode = ExtensionsManager.getEffectMode(mFakeLifecycleOwner);
        assertThat(effectMode).isEqualTo(EffectMode.HDR);
    }

    @Test(expected = IllegalStateException.class)
    public void canNotEanbleExtension_whenUseCaseGroupIsNotEmpty_throwsException() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().build();
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);

        CameraX.bindToLifecycle(mFakeLifecycleOwner, imageCapture);
        ExtensionsManager.enableExtension(EffectMode.HDR, mFakeLifecycleOwner);
    }

    @Test
    public void canGetCorrectEffectMode() {
        FakeLifecycleOwner fakeLifecycleOwner2 = new FakeLifecycleOwner();
        FakeLifecycleOwner fakeLifecycleOwner3 = new FakeLifecycleOwner();

        ExtensionsManager.enableExtension(EffectMode.HDR, mFakeLifecycleOwner);
        ExtensionsManager.enableExtension(EffectMode.BOKEH, fakeLifecycleOwner2);

        EffectMode effectMode = ExtensionsManager.getEffectMode(mFakeLifecycleOwner);
        assertThat(effectMode).isEqualTo(EffectMode.HDR);

        effectMode = ExtensionsManager.getEffectMode(fakeLifecycleOwner2);
        assertThat(effectMode).isEqualTo(EffectMode.BOKEH);

        effectMode = ExtensionsManager.getEffectMode(fakeLifecycleOwner3);
        assertThat(effectMode).isEqualTo(EffectMode.NORMAL);
    }

    @Test
    public void canPostErrorCode() throws InterruptedException {
        final AtomicReference<ExtensionsErrorListener.ExtensionsErrorCode> resultErrorCode =
                new AtomicReference<>();
        ExtensionsManager.setExtensionsErrorListener(new ExtensionsErrorListener() {
            @Override
            public void onError(@NonNull ExtensionsErrorCode errorCode) {
                resultErrorCode.set(errorCode);
                mLatch.countDown();
            }
        });

        ExtensionsManager.postExtensionsError(ExtensionsErrorListener.ExtensionsErrorCode.UNKNOWN);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(resultErrorCode.get()).isNotNull();
    }
}
