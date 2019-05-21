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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ProcessingSurfaceTexture {
    private static final String TAG = "SurfaceTextureIR";
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
    private FutureCallback<List<ImageProxy>> mCaptureStageReadyCallback =
            new FutureCallback<List<ImageProxy>>() {
                @Override
                public void onSuccess(@Nullable List<ImageProxy> imageProxyList) {
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

    @GuardedBy("mLock")
    private final ImageReaderProxy mInputImageReader;

    @GuardedBy("mLock")
    private final Surface mSurface;

    @GuardedBy("mLock")
    @Nullable
    Handler mHandler;

    CaptureStage mCaptureStage;

    private static final int sMaxImages = 3;

    @NonNull
    CaptureProcessor mCaptureProcessor;

    @GuardedBy("mLock")
    SettableImageProxyBundle mSettableImageProxyBundle = null;

    /**
     * Create a {@link ProcessingSurfaceTexture} with specific configurations.
     *
     * @param width            Width of the ImageReader
     * @param height           Height of the ImageReader
     * @param format           Image format
     * @param maxImages        Maximum Image number the ImageReader can hold. The capacity should
     *                         be greater than the captureBundle size in order to hold all the
     *                         Images needed with this processing.
     * @param handler          Handler for executing
     *                         {@link ImageReaderProxy.OnImageAvailableListener}
     * @param captureBundle    The {@link CaptureBundle} includes the processing information
     * @param captureProcessor The {@link CaptureProcessor} to be invoked when the Images are ready
     */
    ProcessingSurfaceTexture(int width, int height, int format,
            @Nullable Handler handler,
            @NonNull CaptureStage captureStage, @NonNull CaptureProcessor captureProcessor,
            Surface surfaceTexture) {
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                sMaxImages,
                handler);

        mSurface = surfaceTexture;

        mHandler = handler;
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onOutputSurface(mSurface, mInputImageReader.getImageFormat());
        mCaptureProcessor.onResolutionUpdate(
                new Size(mInputImageReader.getWidth(), mInputImageReader.getHeight()));

        mCaptureStage = captureStage;
        setCaptureStage(captureStage);
    }

    public void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            mInputImageReader.close();
            mSettableImageProxyBundle.close();
            mClosed = true;
        }
    }

    /** Sets a CaptureStage */
    public void setCaptureStage(@NonNull CaptureStage captureStage) {
        synchronized (mLock) {
            mSettableImageProxyBundle = new SettableImageProxyBundle(
                    Collections.singletonList(captureStage.getId()));
            setupSettableImageProxyBundleCallbacks();
        }
    }

    /** Returns necessary camera callbacks to retrieve metadata from camera result. */
    @Nullable
    CameraCaptureCallback getCameraCaptureCallback() {
        if (mInputImageReader instanceof MetadataImageReader) {
            return ((MetadataImageReader) mInputImageReader).getCameraCaptureCallback();
        } else {
            return CameraCaptureCallbacks.createNoOpCallback();
        }
    }

    void setupSettableImageProxyBundleCallbacks() {
        List<ListenableFuture<ImageProxy>> futureList = new ArrayList<>();
        futureList.add(mSettableImageProxyBundle.getImageProxy((mCaptureStage.getId())));

        Futures.addCallback(Futures.allAsList(futureList), mCaptureStageReadyCallback,
                CameraXExecutors.directExecutor());
    }

    // Incoming Image from InputImageReader. Acquires it and add to SettableImageProxyBundle.
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
                        return;
                    }

                    mSettableImageProxyBundle.addImageProxy(image);
                }
            }
        }
    }

    public Surface getSurface() {
        return mInputImageReader.getSurface();
    }
}
