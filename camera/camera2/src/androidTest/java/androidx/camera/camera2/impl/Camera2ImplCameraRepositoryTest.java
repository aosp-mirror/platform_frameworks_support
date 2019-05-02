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

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks.SessionStateCallback;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraRepository;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * Contains tests for {@link androidx.camera.core.CameraRepository} which require an actual
 * implementation to run.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2ImplCameraRepositoryTest {
    private CameraRepository mCameraRepository;
    private UseCaseGroup mUseCaseGroup;
    private FakeUseCaseConfig mConfig;
    private CallbackAttachingFakeUseCase mUseCase;
    private CameraFactory mCameraFactory;
    private String mCameraId;

    private String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
        try {
            return mCameraFactory.cameraIdForLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }
    }

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() throws AssumptionViolatedException {
        assumeTrue(checkCamera() > 0);
        mCameraRepository = new CameraRepository();
        mCameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
        mCameraRepository.init(mCameraFactory);
        mUseCaseGroup = new UseCaseGroup();
        mConfig = new FakeUseCaseConfig.Builder().setLensFacing(LensFacing.BACK).build();
        mCameraId = getCameraIdForLensFacingUnchecked(mConfig.getLensFacing());
        mUseCase = new CallbackAttachingFakeUseCase(mConfig, mCameraId);
        mUseCaseGroup.addUseCase(mUseCase);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (checkCamera() > 0) {
            mCameraRepository.onGroupInactive(mUseCaseGroup);

            // Wait some time for the cameras to close.
            // We need the cameras to close to bring CameraX
            // back to the initial state.
            Thread.sleep(3000);
        }
    }

    @Test
    public void cameraDeviceCallsAreForwardedToCallback() throws InterruptedException {
        mUseCase.addStateChangeListener(
                mCameraRepository.getCamera(
                        getCameraIdForLensFacingUnchecked(mConfig.getLensFacing())));
        mUseCase.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        // Wait for the CameraDevice.onOpened callback.
        mUseCase.mDeviceStateCallback.waitForOnOpened(1);

        mCameraRepository.onGroupInactive(mUseCaseGroup);

        // Wait for the CameraDevice.onClosed callback.
        mUseCase.mDeviceStateCallback.waitForOnClosed(1);
    }

    @Test
    public void cameraSessionCallsAreForwardedToCallback() throws InterruptedException {
        mUseCase.addStateChangeListener(
                mCameraRepository.getCamera(
                        getCameraIdForLensFacingUnchecked(mConfig.getLensFacing())));
        mUseCase.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        // Wait for the CameraCaptureSession.onConfigured callback.
        mUseCase.mSessionStateCallback.waitForOnConfigured(1);

        // Camera doesn't currently call CaptureSession.release(), because it is recommended that
        // we don't explicitly call CameraCaptureSession.close(). Rather, we rely on another
        // CameraCaptureSession to get opened. See
        // https://developer.android.com/reference/android/hardware/camera2/CameraCaptureSession
        // .html#close()
    }

    private int checkCamera() {
        Log.i("Camera2ImplCameraRepositoryTest", "checkCamera");
        int length = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                length = ((CameraManager) ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE)).getCameraIdList().length;
            } catch (CameraAccessException e) {
                Log.e("Camera2ImplCameraRepositoryTest", "checkCamera CAMERA_SERVICE", e);
            }
            Log.i("Camera2ImplCameraRepositoryTest", "id length" + length);
        }
        ////////////////////////////////////
        int noc = Camera.getNumberOfCameras();
        Log.i("Camera2ImplCameraRepositoryTest", "Camera.getNumberOfCameras(): " + noc);

        /////////////////////////////////////////
        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        boolean frontCam, rearCam;

        //Must have a targetSdk >= 9 defined in the AndroidManifest
        frontCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        rearCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        Log.i("Camera2ImplCameraRepositoryTest", "frontCam" + frontCam);
        Log.i("Camera2ImplCameraRepositoryTest", "rearCam" + rearCam);

        boolean featureCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean featureCameraAny = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        ////////////////////////////////////////
        if (length <= 0 | noc <= 0 | frontCam | rearCam | featureCamera | featureCameraAny) {
            return 0;
        } else {
            return 1;
        }
    }
    /** A fake use case which attaches to a camera with various callbacks. */
    private static class CallbackAttachingFakeUseCase extends FakeUseCase {
        private final DeviceStateCallback mDeviceStateCallback = new DeviceStateCallback();
        private final SessionStateCallback mSessionStateCallback = new SessionStateCallback();
        private final SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);

        CallbackAttachingFakeUseCase(FakeUseCaseConfig config, String cameraId) {
            super(config);
            // Use most supported resolution for different supported hardware level devices,
            // especially for legacy devices.
            mSurfaceTexture.setDefaultBufferSize(640, 480);

            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            builder.addSurface(new ImmediateSurface(new Surface(mSurfaceTexture)));
            builder.addDeviceStateCallback(mDeviceStateCallback);
            builder.addSessionStateCallback(mSessionStateCallback);

            attachToCamera(cameraId, builder.build());
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            return suggestedResolutionMap;
        }

        void doNotifyActive() {
            super.notifyActive();
        }
    }
}
