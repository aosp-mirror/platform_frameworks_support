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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Rational;
import android.util.Size;
import android.view.WindowManager;

import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageFormatConstants;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfig;
import androidx.camera.testing.StreamConfigurationMapUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class MixedUseCaseInLimitedDeviceTest {
    protected static final String LIMITED_CAMERA_ID = "1";
    protected static final int DEFAULT_SENSOR_ORIENTATION = 90;
    protected final Size mDisplaySize = new Size(1280, 720);
    protected final Size mPreviewSize = mDisplaySize;
    protected final Size mRecordSize = new Size(3840, 2160);
    protected final Size mMaximumVideoSize = new Size(1920, 1080);
    protected final CamcorderProfileHelper mMockCamcorderProfileHelper =
            Mockito.mock(CamcorderProfileHelper.class);

    /**
     * Except for ImageFormat.JPEG or ImageFormat.YUV, other image formats will be mapped to
     * ImageFormat.PRIVATE (0x22) including SurfaceTexture or MediaCodec classes. Before Android
     * level 23, there is no ImageFormat.PRIVATE. But there is same internal code 0x22 for internal
     * corresponding format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED. Therefore, set 0x22 as default
     * image formate.
     */
    protected final int[] mSupportedFormats =
            new int[]{
                    ImageFormat.YUV_420_888,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_JPEG,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            };

    protected final Size[] mSupportedSizes =
            new Size[]{
                    new Size(4032, 3024),
                    new Size(3840, 2160),
                    new Size(1920, 1080),
                    new Size(1280, 720),
                    new Size(640, 480),
                    new Size(320, 240),
                    new Size(320, 180)
            };

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CameraDeviceSurfaceManager mSurfaceManager;

    @Before
    public void setUp() {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealWidth(mDisplaySize.getWidth());
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealHeight(mDisplaySize.getHeight());

        when(mMockCamcorderProfileHelper.hasProfile(anyInt(), anyInt())).thenReturn(true);

        setupLimitedLevelCamera();
    }

    @Test
    public void getSuggestedResolutionsForMixedUseCaseInLimitedDevice() {
        Rational aspectRatio = new Rational(16, 9);
        PreviewConfig.Builder previewConfigBuilder = new PreviewConfig.Builder();
        VideoCaptureConfig.Builder videoCaptureConfigBuilder = new VideoCaptureConfig.Builder();
        ImageCaptureConfig.Builder imageCaptureConfigBuilder = new ImageCaptureConfig.Builder();

        previewConfigBuilder.setTargetAspectRatio(aspectRatio);
        videoCaptureConfigBuilder.setTargetAspectRatio(aspectRatio);
        imageCaptureConfigBuilder.setTargetAspectRatio(aspectRatio);

        imageCaptureConfigBuilder.setLensFacing(CameraX.LensFacing.BACK);
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());
        videoCaptureConfigBuilder.setLensFacing(CameraX.LensFacing.BACK);
        VideoCapture videoCapture = new VideoCapture(videoCaptureConfigBuilder.build());
        previewConfigBuilder.setLensFacing(CameraX.LensFacing.BACK);
        Preview preview = new Preview(previewConfigBuilder.build());

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(imageCapture);
        useCases.add(videoCapture);
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                mSurfaceManager.getSuggestedResolutions(LIMITED_CAMERA_ID, null, useCases);

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        assertThat(suggestedResolutionMap).containsEntry(imageCapture, mRecordSize);
        assertThat(suggestedResolutionMap).containsEntry(videoCapture, mMaximumVideoSize);
        assertThat(suggestedResolutionMap).containsEntry(preview, mPreviewSize);
    }

    private void setupLimitedLevelCamera() {
        addBackFacingCamera(
                LIMITED_CAMERA_ID,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                null);
        initCameraX();
    }

    private void initCameraX() {
        AppConfig appConfig = createFakeAppConfig(mContext);
        mSurfaceManager = appConfig.getDeviceSurfaceManager(null);
        CameraX.init(mContext, appConfig);
    }

    private void addBackFacingCamera(String cameraId, int hardwareLevel, int[] capabilities) {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);

        shadowCharacteristics.set(
                CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK);

        shadowCharacteristics.set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, hardwareLevel);

        shadowCharacteristics.set(
                CameraCharacteristics.SENSOR_ORIENTATION, DEFAULT_SENSOR_ORIENTATION);

        if (capabilities != null) {
            shadowCharacteristics.set(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities);
        }

        ((ShadowCameraManager) Shadow.extract(
                ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE)))
                .addCamera(cameraId, characteristics);

        shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapUtil.generateFakeStreamConfigurationMap(
                        mSupportedFormats, mSupportedSizes));
    }

    private AppConfig createFakeAppConfig(Context context) {

        // Create the camera factory for creating Camera2 camera objects
        CameraFactory cameraFactory = new Camera2CameraFactory(context);

        // Create the DeviceSurfaceManager for Camera2
        CameraDeviceSurfaceManager surfaceManager =
                new Camera2DeviceSurfaceManager(context, mMockCamcorderProfileHelper);

        // Create default configuration factory
        ExtendableUseCaseConfigFactory configFactory = new ExtendableUseCaseConfigFactory();
        configFactory.installDefaultProvider(
                ImageAnalysisConfig.class,
                new ImageAnalysisConfigProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                ImageCaptureConfig.class,
                new ImageCaptureConfigProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                VideoCaptureConfig.class,
                new VideoCaptureConfigProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                PreviewConfig.class,
                new PreviewConfigProvider(cameraFactory, context));

        AppConfig.Builder appConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(configFactory);

        return appConfigBuilder.build();
    }
}
