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

package androidx.camera.core;

import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;


/**
 * A {@link DeferrableSurface} that does processing and outputs a {@link SurfaceTexture}.
 */
class ProcessingSurfaceTexture extends DeferrableSurface {
    private static final String TAG = "ProcessingSurfaceTextur";

    // Callback when Image is ready from InputImageReader.
    private final ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReaderProxy reader) {
                    imageIncoming(reader);
                }
            };

    // Callback when all the ImageProxies in SettableImageProxyBundle are ready.
    private final FutureCallback<ImageProxy> mCaptureStageReadyCallback =
            new FutureCallback<ImageProxy>() {
                @Override
                public void onSuccess(@Nullable ImageProxy imageProxy) {
                    if (mClosed) {
                        return;
                    }
                    mCaptureProcessor.process(mSettableImageProxyBundle);
                    mSettableImageProxyBundle.reset();
                    setupImageProxyBundle(mCaptureStage);
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            };

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    volatile boolean mClosed = false;

    @NonNull
    private final Size mResolution;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final MetadataImageReader mInputImageReader;

    // The Surface that is backed by mInputImageReader
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Surface mInputSurface;

    // Maximum number of images in the input ImageReader
    private static final int MAX_IMAGES = 2;

    // The output SurfaceTexture
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            SurfaceTexture mSurfaceTexture;

    // The Surface that is backed by mSurfaceTexture
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            Surface mSurfaceTextureSurface;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final CaptureStage mCaptureStage;


    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    final CaptureProcessor mCaptureProcessor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            SettableImageProxyBundle mSettableImageProxyBundle = null;

    private final CameraCaptureCallback mCameraCaptureCallback;

    @NonNull
    private final Handler mHandler;

    /**
     * Create a {@link ProcessingSurfaceTexture} with specific configurations.
     *
     * @param width            Width of the ImageReader
     * @param height           Height of the ImageReader
     * @param format           Image format
     * @param handler          Handler for executing
     *                         {@link ImageReaderProxy.OnImageAvailableListener}. If this is
     *                         {@code null} then execution will be done on the calling thread's
     *                         {@link Looper}.
     * @param captureStage     The {@link CaptureStage} includes the processing information
     * @param captureProcessor The {@link CaptureProcessor} to be invoked when the Images are ready
     */
    ProcessingSurfaceTexture(int width, int height, int format, @Nullable Handler handler,
            @NonNull CaptureStage captureStage, @NonNull CaptureProcessor captureProcessor) {

        mResolution = new Size(width, height);

        if (handler != null) {
            mHandler = handler;
        } else {
            Looper looper = Looper.myLooper();

            if (looper == null) {
                throw new IllegalStateException(
                        "Creating a ProcessingSurfaceTexture requires a non-null Handler, or be "
                                + "created on a thread with a Looper.");
            }

            mHandler = new Handler(looper);
        }

        // input
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                MAX_IMAGES,
                mHandler);
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
        mInputSurface = mInputImageReader.getSurface();
        mCameraCaptureCallback = mInputImageReader.getCameraCaptureCallback();

        // output
        mSurfaceTexture = FixedSizeSurfaceTextures.createDetachedSurfaceTexture(mResolution);
        mSurfaceTextureSurface = new Surface(mSurfaceTexture);

        // processing
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onOutputSurface(mSurfaceTextureSurface, PixelFormat.RGBA_8888);
        mCaptureProcessor.onResolutionUpdate(mResolution);
        mCaptureStage = captureStage;
        setupImageProxyBundle(mCaptureStage);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if {@link #close()} has already been called
     */
    @Override
    @Nullable
    public ListenableFuture<Surface> getSurface() {
        if (mClosed) {
            throw new IllegalStateException("ProcessingSurfaceTexture already closed!");
        }
        return Futures.immediateFuture(mInputSurface);
    }

    /**
     * Returns the SurfaceTexture that the result of the processing gets written to.
     *
     * <p> Whenever {@link #resetSurfaceTexture()} is called the previous output becomes invalidated
     * so this method should be called again to retrieve a new output SurfaceTexture. This should
     * only be called by the consumer thread.
     *
     * @throws IllegalStateException if {@link #close()} has already been called
     */
    SurfaceTexture getSurfaceTexture() {
        if (mClosed) {
            throw new IllegalStateException("ProcessingSurfaceTexture already closed!");
        }

        return mSurfaceTexture;
    }

    /**
     * Resets the output {@link SurfaceTexture}.
     *
     * <p> This should only be called by the consumer thread.
     *
     * @throws IllegalStateException if {@link #close()} has already been called
     */
    void resetSurfaceTexture() {
        if (mClosed) {
            throw new IllegalStateException("ProcessingSurfaceTexture already closed!");
        }

        mSurfaceTexture.release();
        mSurfaceTextureSurface.release();

        mSurfaceTexture = FixedSizeSurfaceTextures.createDetachedSurfaceTexture(mResolution);
        mSurfaceTextureSurface = new Surface(mSurfaceTexture);
        mCaptureProcessor.onOutputSurface(mSurfaceTextureSurface, PixelFormat.RGBA_8888);
    }

    /**
     * Returns necessary camera callbacks to retrieve metadata from camera result.
     *
     * @throws IllegalStateException if {@link #close()} has already been called
     */
    @Nullable
    CameraCaptureCallback getCameraCaptureCallback() {
        if (mClosed) {
            throw new IllegalStateException("ProcessingSurfaceTexture already closed!");
        }

        return mCameraCaptureCallback;
    }

    /**
     * Close the {@link ProcessingSurfaceTexture}.
     *
     * <p> After closing the ProcessingSurfaceTexture it should not be used again. A new instance
     * should be created. This should only be called by the consumer thread.
     */
    public void close() {
        if (mClosed) {
            return;
        }
        mClosed = true;


        mSurfaceTexture.release();
        mSurfaceTextureSurface.release();

        // Release this in the handler's thread because the mSettableImageProxyBundle is
        // processed on that thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSettableImageProxyBundle.close();
            }
        });

        // Need to wait for Surface has been detached before closing it
        setOnSurfaceDetachedListener(CameraXExecutors.directExecutor(),
                new OnSurfaceDetachedListener() {
                    @Override
                    public void onSurfaceDetached() {
                        mInputImageReader.close();
                        mInputSurface.release();
                    }
                });
    }

    // Sets up mImageProxyBundle with the specified CaptureStage
    @SuppressWarnings("WeakerAccess")
    void setupImageProxyBundle(@NonNull CaptureStage captureStage) {
        mSettableImageProxyBundle = new SettableImageProxyBundle(
                Collections.singletonList(captureStage.getId()));
        setupSettableImageProxyBundleCallbacks();
    }

    private void setupSettableImageProxyBundleCallbacks() {
        ListenableFuture<ImageProxy> future = mSettableImageProxyBundle.getImageProxy(
                mCaptureStage.getId());

        Futures.addCallback(future, mCaptureStageReadyCallback, CameraXExecutors.directExecutor());
    }

    // Incoming Image from InputImageReader. Acquires it and add to SettableImageProxyBundle.
    @SuppressWarnings("WeakerAccess")
    void imageIncoming(ImageReaderProxy imageReader) {
        if (mClosed) {
            return;
        }

        ImageProxy image = null;
        try {
            image = imageReader.acquireNextImage();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to acquire next image.", e);
        }

        if (image != null) {
            ImageInfo imageInfo = image.getImageInfo();
            if (imageInfo == null) {
                image.close();
                return;
            }

            Integer tag = (Integer) imageInfo.getTag();
            if (mCaptureStage.getId() != tag) {
                Log.w(TAG, "ImageProxyBundle does not contain this id: " + tag);
                image.close();
            } else {
                mSettableImageProxyBundle.addImageProxy(image);
            }
        }
    }
}
