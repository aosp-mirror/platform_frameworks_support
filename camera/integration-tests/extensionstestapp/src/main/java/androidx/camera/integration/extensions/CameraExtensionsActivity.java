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
package androidx.camera.integration.extensions;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.ExtensionsErrorListener;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/** An activity that shows off how extensions can be applied */
public class CameraExtensionsActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraExtensionActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;

    private final SettableCallable<Boolean> mSettableResult = new SettableCallable<>();
    private final FutureTask<Boolean> mCompletableFuture = new FutureTask<>(mSettableResult);

    /** The cameraId to use. Assume that 0 is the typical back facing camera. */
    private String mCurrentCameraId = "0";

    private String mCurrentCameraFacing = "BACK";
    private LensFacing mLensFacing = LensFacing.BACK;

    private Preview mPreview;
    private ImageCapture mImageCapture;
    private ImageCaptureType mCurrentImageCaptureType = ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR;
    private EffectMode mCurrentEffectMode = EffectMode.HDR;

    private HandlerThread mHandlerThread = new HandlerThread("CameraExtensionsActivityHandler");

    /**
     * Creates a preview use case.
     *
     * <p>This use case observes a {@link SurfaceTexture}. The texture is connected to a {@link
     * TextureView} to display a camera preview.
     */
    void createPreview() {
        PreviewConfig.Builder builder =
                new PreviewConfig.Builder()
                        .setLensFacing(mLensFacing)
                        .setTargetName("Preview");

        mPreview = new Preview(builder.build());
        TextureView textureView = findViewById(R.id.textureView);
        mPreview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        // If TextureView was already created, need to re-add it to change the
                        // SurfaceTexture.
                        ViewGroup viewGroup = (ViewGroup) textureView.getParent();
                        viewGroup.removeView(textureView);
                        viewGroup.addView(textureView);
                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                    }
                });
    }

    enum ImageCaptureType {
        IMAGE_CAPTURE_TYPE_HDR,
        IMAGE_CAPTURE_TYPE_BOKEH,
        IMAGE_CAPTURE_TYPE_NIGHT,
        IMAGE_CAPTURE_TYPE_BEAUTY,
        IMAGE_CAPTURE_TYPE_AUTO,
        IMAGE_CAPTURE_TYPE_DEFAULT,
        IMAGE_CAPTURE_TYPE_NONE,
    }

    /**
     * Creates an image capture use case.
     *
     * <p>This use case takes a picture and saves it to a file, whenever the user clicks a button.
     */
    void createImageCapture() {
        Button captureButton = findViewById(R.id.Picture);

        if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_NONE) {
            captureButton.setOnClickListener(null);
            return;
        }

        ImageCaptureConfig.Builder builder =
                new ImageCaptureConfig.Builder()
                        .setLensFacing(mLensFacing)
                        .setTargetName("ImageCapture");

        if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR
                && ExtensionsManager.isExtensionAvailable(EffectMode.HDR, mLensFacing)) {
            builder.setCallbackHandler(new Handler(mHandlerThread.getLooper()));
        }

        mImageCapture = new ImageCapture(builder.build());
        final Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        final File dir =
                new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES),
                        "ExtensionsPictures");
        dir.mkdirs();
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mImageCapture.takePicture(
                                new File(
                                        dir,
                                        formatter.format(Calendar.getInstance().getTime())
                                                + mCurrentImageCaptureType.name()
                                                + ".jpg"),
                                new ImageCapture.OnImageSavedListener() {
                                    @Override
                                    public void onImageSaved(File file) {
                                        Log.d(TAG, "Saved image to " + file);

                                        // Trigger MediaScanner to scan the file
                                        Intent intent = new Intent(
                                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                        intent.setData(Uri.fromFile(file));
                                        sendBroadcast(intent);

                                        Toast.makeText(getApplicationContext(),
                                                "Saved image to " + file,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(
                                            ImageCapture.UseCaseError useCaseError,
                                            String message,
                                            Throwable cause) {
                                        Log.e(TAG, "Failed to save image - " + message, cause);
                                    }
                                });
                    }
                });
    }

    /** Creates all the use cases. */
    void createUseCases() {
        ExtensionsManager.setExtensionsErrorListener(new ExtensionsErrorListener() {
            @Override
            public void onError(@NonNull ExtensionsErrorCode errorCode) {
                Log.d(TAG, "Extensions error in error code: " + errorCode);
            }
        });

        Button toggleButton = findViewById(R.id.PhotoToggle);
        toggleButton.setText(mCurrentImageCaptureType.toString());

        CameraX.unbindAll();

        mImageCapture = null;
        mPreview = null;

        if (!ExtensionsManager.isExtensionAvailable(mCurrentEffectMode, mLensFacing)) {
            mCurrentEffectMode = EffectMode.NORMAL;
        }
        Log.d(TAG,
                "Current image capture type/effect mode - " + mCurrentImageCaptureType.name() + "/"
                        + mCurrentEffectMode.name());
        ExtensionsManager.enableExtension(mCurrentEffectMode, this);

        createImageCapture();
        createPreview();
        bindUseCases();
    }

    private void bindUseCases() {
        List<UseCase> useCases = new ArrayList();
        // When it is not IMAGE_CAPTURE_TYPE_NONE, mImageCapture won't be null.
        if (mImageCapture != null) {
            useCases.add(mImageCapture);
        }
        useCases.add(mPreview);
        CameraX.bindToLifecycle(this, useCases.toArray(new UseCase[useCases.size()]));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_extensions);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);

        mHandlerThread.start();

        // Get params from adb extra string
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String newCameraFacing = bundle.getString("cameraFacing");
            if (newCameraFacing != null) {
                mCurrentCameraFacing = newCameraFacing;
            }
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        setupCamera();
                    }
                })
                .start();
        setupPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandlerThread.quitSafely();
    }

    private void setupCamera() {
        try {
            // Wait for permissions before proceeding.
            if (!mCompletableFuture.get()) {
                Log.d(TAG, "Permissions denied.");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred getting permission future: " + e);
        }

        try {
            Log.d(TAG, "Camera Facing: " + mCurrentCameraFacing);
            if (mCurrentCameraFacing.equalsIgnoreCase("BACK")) {
                mLensFacing = LensFacing.BACK;
            } else if (mCurrentCameraFacing.equalsIgnoreCase("FRONT")) {
                mLensFacing = LensFacing.FRONT;
            } else {
                throw new RuntimeException("Invalid lens facing: " + mCurrentCameraFacing);
            }
            mCurrentCameraId = CameraX.getCameraWithLensFacing(mLensFacing);
        } catch (Exception e) {
            Log.e(TAG, "Unable to obtain camera with specified facing. " + e.getMessage());
        }

        Log.d(TAG, "Using cameraId: " + mCurrentCameraId);

        // Run this on the UI thread to manipulate the Textures & Views.
        CameraExtensionsActivity.this.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Button button = findViewById(R.id.PhotoToggle);
                        button.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        // Toggle to next capture type and set it as current
                                        switch (mCurrentImageCaptureType) {
                                            case IMAGE_CAPTURE_TYPE_HDR:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_BOKEH;
                                                mCurrentEffectMode = EffectMode.BOKEH;
                                                break;
                                            case IMAGE_CAPTURE_TYPE_BOKEH:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_NIGHT;
                                                mCurrentEffectMode = EffectMode.NIGHT;
                                                break;
                                            case IMAGE_CAPTURE_TYPE_NIGHT:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_BEAUTY;
                                                mCurrentEffectMode = EffectMode.BEAUTY;
                                                break;
                                            case IMAGE_CAPTURE_TYPE_BEAUTY:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_AUTO;
                                                mCurrentEffectMode = EffectMode.AUTO;
                                                break;
                                            case IMAGE_CAPTURE_TYPE_AUTO:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_DEFAULT;
                                                mCurrentEffectMode = EffectMode.NORMAL;
                                                break;
                                            case IMAGE_CAPTURE_TYPE_DEFAULT:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_NONE;
                                                mCurrentEffectMode = EffectMode.NORMAL;
                                                break;
                                            case IMAGE_CAPTURE_TYPE_NONE:
                                                mCurrentImageCaptureType =
                                                        ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR;
                                                mCurrentEffectMode = EffectMode.HDR;
                                                break;
                                        }

                                        createUseCases();
                                    }
                                });

                        createUseCases();
                    }
                });
    }

    private void setupPermissions() {
        if (!allPermissionsGranted()) {
            makePermissionRequest();
        } else {
            mSettableResult.set(true);
            mCompletableFuture.run();
        }
    }

    private void makePermissionRequest() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
    }

    /** Returns true if all the necessary permissions have been granted already. */
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Tries to acquire all the necessary permissions through a dialog. */
    private String[] getRequiredPermissions() {
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException exception) {
            Log.e(TAG, "Failed to obtain all required permissions.", exception);
            return new String[0];
        }
        String[] permissions = info.requestedPermissions;
        if (permissions != null && permissions.length > 0) {
            return permissions;
        } else {
            return new String[0];
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions Granted.");
                    mSettableResult.set(true);
                    mCompletableFuture.run();
                } else {
                    Log.d(TAG, "Permissions Denied.");
                    mSettableResult.set(false);
                    mCompletableFuture.run();
                }
                return;
            }
            default:
                // No-op
        }
    }

    /** A {@link Callable} whose return value can be set. */
    private static final class SettableCallable<V> implements Callable<V> {
        private final AtomicReference<V> mValue = new AtomicReference<>();

        public void set(V value) {
            mValue.set(value);
        }

        @Override
        public V call() {
            return mValue.get();
        }
    }
}
