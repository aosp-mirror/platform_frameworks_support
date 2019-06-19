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

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.IntBuffer;
import java.util.Collections;


/** A {@link DeferrableSurface} that does processing and outputs a {@link SurfaceTexture}. */
class ProcessingSurfaceTexture extends DeferrableSurface {
    private static final String TAG = "ProcessingSurfaceTextur";
    private final Object mLock = new Object();

    // Callback when Image is ready from InputImageReader.
    private ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReaderProxy reader) {
                    imageIncoming(reader);
                }
            };

    // Callback when all the ImageProxies in SettableImageProxyBundle are ready.
    private FutureCallback<ImageProxy> mCaptureStageReadyCallback =
            new FutureCallback<ImageProxy>() {
                @Override
                public void onSuccess(@Nullable ImageProxy imageProxy) {
                    mCaptureProcessor.process(mSettableImageProxyBundle);
                    mSettableImageProxyBundle.reset();
                    setCaptureStage(mCaptureStage);
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            };

    @GuardedBy("mLock")
    private boolean mClosed = false;

    @NonNull
    private final Size mResolution;

    @GuardedBy("mLock")
    private final ImageReaderProxy mInputImageReader;

    // The Surface that is backed by mInputImageReader
    @GuardedBy("mLock")
    private final Surface mInputSurface;

    private SurfaceTexture mSurfaceTexture;

    // The Surface that is backed by mSurfaceTexture
    private Surface mSurfaceTextureSurface;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CaptureStage mCaptureStage;

    private static final int sMaxImages = 2;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    CaptureProcessor mCaptureProcessor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    SettableImageProxyBundle mSettableImageProxyBundle = null;

    /**
     * Create a {@link ProcessingSurfaceTexture} with specific configurations.
     *
     * @param width            Width of the ImageReader
     * @param height           Height of the ImageReader
     * @param format           Image format
     * @param handler          Handler for executing
     *                         {@link ImageReaderProxy.OnImageAvailableListener}
     * @param captureStage     The {@link CaptureStage} includes the processing information
     * @param captureProcessor The {@link CaptureProcessor} to be invoked when the Images are ready
     */
    ProcessingSurfaceTexture(int width, int height, int format,
            @Nullable Handler handler,
            @NonNull CaptureStage captureStage, @NonNull CaptureProcessor captureProcessor) {
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                sMaxImages,
                handler);
        mResolution = new Size(width, height);

        // input
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
        mInputSurface = mInputImageReader.getSurface();

        // output
        mSurfaceTexture = createDetachedSurfaceTexture(mResolution);
        mSurfaceTextureSurface = new Surface(mSurfaceTexture);

        // processing
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onOutputSurface(mSurfaceTextureSurface,
                mInputImageReader.getImageFormat());
        mCaptureProcessor.onResolutionUpdate(mResolution);
        mCaptureStage = captureStage;
        setCaptureStage(mCaptureStage);
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
     * so this method should be called again to retrieve a new output SurfaceTexture.
     *
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
     * @throws IllegalStateException if {@link #close()} has already been called
     */
    void resetSurfaceTexture() {
        synchronized (mLock) {
            if (mClosed) {
                throw new IllegalStateException("ProcessingSurfaceTexture already closed!");
            }
            mSurfaceTexture.release();
            mSurfaceTextureSurface.release();

            mSurfaceTexture = createDetachedSurfaceTexture(mResolution);
            mSurfaceTextureSurface = new Surface(mSurfaceTexture);
            mCaptureProcessor.onOutputSurface(mSurfaceTextureSurface,
                    mInputImageReader.getImageFormat());
        }
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

        if (mInputImageReader instanceof MetadataImageReader) {
            return ((MetadataImageReader) mInputImageReader).getCameraCaptureCallback();
        } else {
            return CameraCaptureCallbacks.createNoOpCallback();
        }
    }

    /**
     * Close the {@link ProcessingSurfaceTexture}.
     *
     * <p> After closing the ProcessingSurfaceTexture it should not be used again. A new instance
     * should be created.
     */
    public void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            mInputImageReader.close();
            mSettableImageProxyBundle.close();
            mSurfaceTexture.release();
            mSurfaceTextureSurface.release();
            mInputSurface.release();

            mClosed = true;
        }
    }

    /** Sets a CaptureStage */
    private void setCaptureStage(@NonNull CaptureStage captureStage) {
        synchronized (mLock) {
            mSettableImageProxyBundle = new SettableImageProxyBundle(
                    Collections.singletonList(captureStage.getId()));
            setupSettableImageProxyBundleCallbacks();
        }
    }

    private FixedSizeSurfaceTexture createDetachedSurfaceTexture(Size resolution) {
        IntBuffer buffer = IntBuffer.allocate(1);
        GLES20.glGenTextures(1, buffer);

        FixedSizeSurfaceTexture surfaceTexture = new FixedSizeSurfaceTexture(buffer.get(),
                resolution);
        surfaceTexture.detachFromGLContext();

        return surfaceTexture;
    }

    private void setupSettableImageProxyBundleCallbacks() {
        ListenableFuture<ImageProxy> future = mSettableImageProxyBundle.getImageProxy(
                (mCaptureStage.getId()));

        Futures.addCallback(future, mCaptureStageReadyCallback, CameraXExecutors.directExecutor());
    }

    // Incoming Image from InputImageReader. Acquires it and add to SettableImageProxyBundle.
    @SuppressWarnings("WeakerAccess")
    void imageIncoming(ImageReaderProxy imageReader) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            ImageProxy image = null;
            try {
                image = imageReader.acquireNextImage();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to acquire latest image.", e);
            } finally {
                if (image != null) {
                    Integer tag = (Integer) image.getImageInfo().getTag();
                    if (mCaptureStage.getId() != tag) {
                        Log.w(TAG, "ImageProxyBundle does not contain this id: " + tag);
                        image.close();
                    } else {
                        mSettableImageProxyBundle.addImageProxy(image);
                    }
                }
            }
        }
    }
}
