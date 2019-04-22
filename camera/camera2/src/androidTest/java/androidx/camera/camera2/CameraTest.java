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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CameraTest {

    private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;
    static CameraFactory sCameraFactory;

    Camera mCamera;

    TestUseCase mFakeUseCase;
    OnImageAvailableListener mMockOnImageAvailableListener;
    String mCameraId;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
        try {
            return sCameraFactory.cameraIdForLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }
    }

    @BeforeClass
    public static void classSetup() {
        sCameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
    }

    @Before
    public void setup() {
        mMockOnImageAvailableListener = Mockito.mock(ImageReader.OnImageAvailableListener.class);
        FakeUseCaseConfig config =
                new FakeUseCaseConfig.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .build();
        mCameraId = getCameraIdForLensFacingUnchecked(DEFAULT_LENS_FACING);
        mFakeUseCase = new TestUseCase(config, mMockOnImageAvailableListener);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, new Size(640, 480));
        mFakeUseCase.updateSuggestedResolution(suggestedResolutionMap);

        mCamera = (Camera) sCameraFactory.getCamera(mCameraId);
    }

    @After
    public void teardown() throws InterruptedException {
        // Need to release the camera no matter what is done, otherwise the CameraDevice is not
        // closed.
        // When the CameraDevice is not closed, then it can cause problems with interferes with
        // other
        // test cases.
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        // Wait a little bit for the camera device to close.
        // TODO(b/111991758): Listen for the close signal when it becomes available.
        Thread.sleep(2000);

        if (mFakeUseCase != null) {
            mFakeUseCase.close();
            mFakeUseCase = null;
        }

        if (mCameraHandlerThread != null) {
            mCameraHandlerThread.quitSafely();
        }

    }

    @Test
    public void onlineUseCase() {
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera.release();
    }

    @Test
    public void activeUseCase() {
        mCamera.open();

        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera.release();
    }

    @Test
    public void onlineAndActiveUseCase() throws InterruptedException {
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, timeout(4000).atLeastOnce())
                .onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void removeOnlineUseCase() {
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.removeOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void unopenedCamera() {
        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.removeOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void closedCamera() {
        mCamera.open();

        mCamera.close();
        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.removeOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releaseUnopenedCamera() {
        mCamera.release();
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releasedOpenedCamera() {
        mCamera.release();
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<UseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    Semaphore mSemaphore;
    HandlerThread mCameraHandlerThread;
    Handler mCameraHandler;

    private void setupAggregateTest() {
        mCameraHandlerThread = new HandlerThread("cameraThread");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());
        mSemaphore = new Semaphore(0);
        mCamera = new Camera(CameraUtil.getCameraManager(), mCameraId, mCameraHandler);
        //Block the handler until semaphore is released.
        blockHandler();
    }

    private void blockHandler() {
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mSemaphore.acquire();
                } catch (InterruptedException e) {

                }
            }
        });
    }

    private void waitHandlerIdle() throws InterruptedException {
        final Object idleLock = new Object();
        // If the posted runnable runs, it means the previous runnables are already executed.
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (idleLock) {
                    idleLock.notify();
                }
            }
        });

        synchronized (idleLock) {
            idleLock.wait(1000);
        }

        Thread.sleep(50);
    }

    @Test
    public void addOnlineAggregate() throws InterruptedException {

        setupAggregateTest();

        UseCase useCase1 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));

        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(1);
        assertThat(mCamera.mPendingAddOnline).containsExactly(useCase1);
        assertThat(mCamera.isUseCaseOnline(useCase1)).isFalse();

        UseCase useCase2 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase2));

        assertThat(getUseCaseSurface(useCase2).getAttachedCount()).isEqualTo(1);
        assertThat(mCamera.mPendingAddOnline).containsExactly(useCase1, useCase2);

        UseCase useCase3 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase3));
        assertThat(getUseCaseSurface(useCase3).getAttachedCount()).isEqualTo(1);
        assertThat(mCamera.mPendingAddOnline).containsExactly(useCase1, useCase2, useCase3);

        mSemaphore.release();
        waitHandlerIdle();

        assertThat(mCamera.mPendingAddOnline).isEmpty();
        assertThat(mCamera.isUseCaseOnline(useCase1)).isTrue();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isTrue();
        assertThat(mCamera.isUseCaseOnline(useCase3)).isTrue();

        mCamera.removeOnlineUseCase(Arrays.asList(useCase1, useCase2, useCase3));
    }

    @Test
    public void removeOnlineAggregate() throws InterruptedException {

        setupAggregateTest();

        UseCase useCase1 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));
        UseCase useCase2 = createUseCase();

        mCamera.removeOnlineUseCase(Arrays.asList(useCase2));
        // useCase2 is not online , should not be added to mPendingRemoveOnline
        assertThat(mCamera.mPendingRemoveOnline).isEmpty();
        assertThat(getUseCaseSurface(useCase2).getAttachedCount()).isEqualTo(0);


        mCamera.addOnlineUseCase(Arrays.asList(useCase2));
        UseCase useCase3 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase3));

        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));
        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(0);

        assertThat(mCamera.mPendingRemoveOnline).isEmpty();
        assertThat(mCamera.mPendingAddOnline).containsExactly(useCase2, useCase3);

        mSemaphore.release();
        waitHandlerIdle();

        assertThat(mCamera.mPendingRemoveOnline).isEmpty();
        assertThat(mCamera.mPendingAddOnline).isEmpty();
        assertThat(mCamera.isUseCaseOnline(useCase1)).isFalse();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isTrue();
        assertThat(mCamera.isUseCaseOnline(useCase3)).isTrue();

        blockHandler();

        mCamera.removeOnlineUseCase(Arrays.asList(useCase2));
        mCamera.removeOnlineUseCase(Arrays.asList(useCase2));
        mCamera.removeOnlineUseCase(Arrays.asList(useCase3));

        assertThat(mCamera.mPendingRemoveOnline).containsExactly(useCase2, useCase3);

        mSemaphore.release();
        waitHandlerIdle();
        assertThat(mCamera.mPendingRemoveOnline.size()).isEqualTo(0);
        assertThat(mCamera.mPendingAddOnline.size()).isEqualTo(0);
        assertThat(mCamera.isUseCaseOnline(useCase1)).isFalse();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isFalse();
        assertThat(mCamera.isUseCaseOnline(useCase3)).isFalse();

        // Surface is attached when (1) UseCased added to online (2) Camera session opened
        // So here we need to wait until camera close before we start to verify the attach count
        mCamera.close();
        waitHandlerIdle();

        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(0);
        assertThat(getUseCaseSurface(useCase2).getAttachedCount()).isEqualTo(0);
        assertThat(getUseCaseSurface(useCase3).getAttachedCount()).isEqualTo(0);


    }

    @Test
    public void removeAndAddOnlineAggregate() throws InterruptedException {

        setupAggregateTest();

        UseCase useCase = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase));
        mSemaphore.release();
        waitHandlerIdle();

        blockHandler();

        mCamera.removeOnlineUseCase(Arrays.asList(useCase));
        mCamera.addOnlineUseCase(Arrays.asList(useCase));

        assertThat(mCamera.mPendingRemoveOnline).isEmpty();
        assertThat(mCamera.mPendingAddOnline).isEmpty();

        mSemaphore.release();
        waitHandlerIdle();

        assertThat(mCamera.isUseCaseOnline(useCase)).isTrue();

        mCamera.close();
        waitHandlerIdle();
        assertThat(getUseCaseSurface(useCase).getAttachedCount()).isEqualTo(1);

    }

    private UseCase createUseCase() {
        FakeUseCaseConfig config =
                new FakeUseCaseConfig.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .build();

        TestUseCase testUseCase = new TestUseCase(config, mMockOnImageAvailableListener);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, new Size(640, 480));
        testUseCase.updateSuggestedResolution(suggestedResolutionMap);
        return testUseCase;
    }

    private DeferrableSurface getUseCaseSurface(UseCase useCase) {
        return useCase.getSessionConfig(mCameraId).getSurfaces().get(0);
    }

    private static class TestUseCase extends FakeUseCase {
        private final ImageReader.OnImageAvailableListener mImageAvailableListener;
        HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
        Handler mHandler;
        ImageReader mImageReader;

        TestUseCase(
                FakeUseCaseConfig config,
                ImageReader.OnImageAvailableListener listener) {
            super(config);
            mImageAvailableListener = listener;
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            String cameraId = getCameraIdForLensFacingUnchecked(config.getLensFacing());
            suggestedResolutionMap.put(cameraId, new Size(640, 480));
            updateSuggestedResolution(suggestedResolutionMap);
        }

        void close() {
            mHandler.removeCallbacksAndMessages(null);
            mHandlerThread.quitSafely();
            if (mImageReader != null) {
                mImageReader.close();
            }
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            LensFacing lensFacing = ((CameraDeviceConfig) getUseCaseConfig()).getLensFacing();
            String cameraId = getCameraIdForLensFacingUnchecked(lensFacing);
            Size resolution = suggestedResolutionMap.get(cameraId);
            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            mImageReader =
                    ImageReader.newInstance(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            ImageFormat.YUV_420_888, /*maxImages*/
                            2);
            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mHandler);
            builder.addSurface(new ImmediateSurface(mImageReader.getSurface()));

            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
