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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Size;

import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraUseCaseAggregatorTest {
    private static final CameraX.LensFacing DEFAULT_LENS_FACING = CameraX.LensFacing.BACK;
    static CameraFactory sCameraFactory;

    Camera mCamera;
    String mCameraId;

    Semaphore mSemaphore;
    HandlerThread mCameraHandlerThread;
    Handler mCameraHandler;
    MessageQueue mMessageQueue;


    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static String getCameraIdForLensFacingUnchecked(CameraX.LensFacing lensFacing) {
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
    public void setUp() {
        mCameraId = getCameraIdForLensFacingUnchecked(DEFAULT_LENS_FACING);

        mCameraHandlerThread = new HandlerThread("cameraThread");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());
        mSemaphore = new Semaphore(0);
        mCamera = new Camera(CameraUtil.getCameraManager(), mCameraId, mCameraHandler);

        // To get MessageQueue from HandlerThread.
        final CountDownLatch latchforMsgQueue = new CountDownLatch(1);
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                mMessageQueue = Looper.myQueue();
                latchforMsgQueue.countDown();
            }
        });

        try {
            latchforMsgQueue.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }

    }

    @After
    public void teardown() throws InterruptedException {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        // Wait a little bit for the camera device to close.
        // TODO(b/111991758): Listen for the close signal when it becomes available.
        Thread.sleep(2000);

        if (mCameraHandlerThread != null) {
            mCameraHandlerThread.quitSafely();
        }
    }

    // Block handler so that we can verify the aggregation.
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

    // unblock handler so that aggregated action can be executed.
    private void unblockHandler() {
        mSemaphore.release();
    }

    // wait until aggregated action is done.
    private void waitHandlerIdle() throws InterruptedException {
        final CountDownLatch latchForWaitIdle = new CountDownLatch(1);


        // If the posted runnable runs, it means the previous runnnables are already executed.
        mMessageQueue.addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                latchForWaitIdle.countDown();
                return false;

            }
        });

        latchForWaitIdle.await(3, TimeUnit.SECONDS);
    }

    private UseCase createUseCase() {
        FakeUseCaseConfig config =
                new FakeUseCaseConfig.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .build();

        TestUseCase testUseCase = new TestUseCase(config);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, new Size(640, 480));
        testUseCase.updateSuggestedResolution(suggestedResolutionMap);
        return testUseCase;
    }

    @Test
    public void addOnline_SameUseCases() {
        // Block handler so that we can verify the aggregation.
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));

        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(1);
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).containsExactly(useCase1);
        assertThat(mCamera.isUseCaseOnline(useCase1)).isFalse();

        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));
    }

    @Test
    public void addOnline_alreadyOnline() throws InterruptedException {
        // Block handler so that we can verify the aggregation.
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));
        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(1);
        unblockHandler();
        waitHandlerIdle();

        //Usecase1 is online now.
        assertThat(mCamera.isUseCaseOnline(useCase1)).isTrue();

        blockHandler();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));

        //no pending online use cases since UseCase1 is already online.
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).isEmpty();

        unblockHandler();
        // Surface is attached when (1) UseCase added to online (2) Camera session opened
        // So here we need to wait until camera close before we start to verify the attach count
        mCamera.close();
        waitHandlerIdle();

        // Surface is only attached once.
        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(1);


        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));
    }

    @Test
    public void addOnline_twoUseCases() throws InterruptedException {
        // Block handler so that we can verify the aggregation.
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));
        UseCase useCase2 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase2));

        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(1);
        assertThat(getUseCaseSurface(useCase2).getAttachedCount()).isEqualTo(1);
        assertThat(mCamera.isUseCaseOnline(useCase1)).isFalse();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isFalse();
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).containsExactly(useCase1,
                useCase2);

        // unblock handler so that aggregated action can be executed.
        unblockHandler();
        // wait until aggregated action is done.
        waitHandlerIdle();

        // aggregator executes the action and clear the queue.
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).isEmpty();
        assertThat(mCamera.isUseCaseOnline(useCase1)).isTrue();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isTrue();

        mCamera.removeOnlineUseCase(Arrays.asList(useCase1, useCase2));
    }

    @Test
    public void addOnline_fromPendingOffline() throws InterruptedException {
        blockHandler();

        // First make UseCase online
        UseCase useCase = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase));
        unblockHandler();
        waitHandlerIdle();

        blockHandler();
        // Then put useCase in pendingOffline.
        mCamera.removeOnlineUseCase(Arrays.asList(useCase));

        // Then add the same UseCase .
        mCamera.addOnlineUseCase(Arrays.asList(useCase));

        assertThat(mCamera.mUseCaseAggregator.getPendingOffline()).isEmpty();
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).isEmpty();

        unblockHandler();
        waitHandlerIdle();

        assertThat(mCamera.isUseCaseOnline(useCase)).isTrue();

        mCamera.close();
        waitHandlerIdle();
        assertThat(getUseCaseSurface(useCase).getAttachedCount()).isEqualTo(1);

        mCamera.removeOnlineUseCase(Arrays.asList(useCase));
    }

    @Test
    public void removeOnline_notOnline() throws InterruptedException {
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));

        // useCase2 is not online , should not be added to pendingOffline
        assertThat(mCamera.mUseCaseAggregator.getPendingOffline()).isEmpty();
        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(0);

    }

    @Test
    public void removeOnline_fromPendingOnline() throws InterruptedException {
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1));
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).containsExactly(useCase1);
        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(1);

        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));

        // useCase is pending for online, it should just remove it from pendingOnline instead of
        // being added to pendingOffline.
        assertThat(mCamera.mUseCaseAggregator.getPendingOffline()).isEmpty();
        assertThat(mCamera.mUseCaseAggregator.getPendingOnline()).isEmpty();

        // Should detach the surface since we remove it from pending Online .
        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(0);
    }

    @Test
    public void removeOnline_fromOnlineUseCases() throws InterruptedException {
        blockHandler();

        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1, useCase2));

        unblockHandler();
        waitHandlerIdle();

        assertThat(mCamera.isUseCaseOnline(useCase1)).isTrue();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isTrue();

        blockHandler();
        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));

        assertThat(mCamera.mUseCaseAggregator.getPendingOffline()).containsExactly(useCase1);

        unblockHandler();
        waitHandlerIdle();

        assertThat(mCamera.mUseCaseAggregator.getPendingOffline()).isEmpty();
        assertThat(mCamera.isUseCaseOnline(useCase1)).isFalse();
        assertThat(mCamera.isUseCaseOnline(useCase2)).isTrue();

        // Surface is attached when (1) UseCase added to online (2) Camera session opened
        // So here we need to wait until camera close before we start to verify the attach count
        mCamera.close();
        waitHandlerIdle();

        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(0);
        assertThat(getUseCaseSurface(useCase2).getAttachedCount()).isEqualTo(1);


        mCamera.removeOnlineUseCase(Arrays.asList(useCase2));
    }

    @Test
    public void removeOnline_twoSameUseCase() throws InterruptedException {
        blockHandler();

        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera.addOnlineUseCase(Arrays.asList(useCase1, useCase2));

        unblockHandler();
        waitHandlerIdle();

        // remove twice
        blockHandler();
        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));
        mCamera.removeOnlineUseCase(Arrays.asList(useCase1));

        assertThat(mCamera.mUseCaseAggregator.getPendingOffline()).containsExactly(useCase1);

        unblockHandler();
        // Surface is attached when (1) UseCase added to online (2) Camera session opened
        // So here we need to wait until camera close before we start to verify the attach count
        mCamera.close();
        waitHandlerIdle();

        assertThat(getUseCaseSurface(useCase1).getAttachedCount()).isEqualTo(0);

        mCamera.removeOnlineUseCase(Arrays.asList(useCase2));
    }


    private DeferrableSurface getUseCaseSurface(UseCase useCase) {
        return useCase.getSessionConfig(mCameraId).getSurfaces().get(0);
    }

    private static class TestUseCase extends FakeUseCase {
        HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
        Handler mHandler;
        ImageReader mImageReader;

        TestUseCase(
                FakeUseCaseConfig config) {
            super(config);

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
            CameraX.LensFacing lensFacing =
                    ((CameraDeviceConfig) getUseCaseConfig()).getLensFacing();
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
            builder.addSurface(new ImmediateSurface(mImageReader.getSurface()));

            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
