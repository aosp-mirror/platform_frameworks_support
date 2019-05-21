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
import java.util.List;

class SurfaceTextureImageReader implements ImageReaderProxy {
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
                    setCaptureBundle(mCaptureBundle);
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
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    @GuardedBy("mLock")
    @Nullable
    Handler mHandler;

    CaptureBundle mCaptureBundle;

    @NonNull
    CaptureProcessor mCaptureProcessor;

    @GuardedBy("mLock")
    SettableImageProxyBundle mSettableImageProxyBundle = null;

    private final List<Integer> mCaptureIdList = new ArrayList<>();

    /**
     * Create a {@link SurfaceTextureImageReader} with specific configurations.
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
    SurfaceTextureImageReader(int width, int height, int format,
            @Nullable Handler handler,
            @NonNull CaptureBundle captureBundle, @NonNull CaptureProcessor captureProcessor,
            Surface surfaceTexture) {
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                3,
                handler);

        init(handler, captureBundle, captureProcessor, surfaceTexture);
    }


    private void init(@Nullable Handler handler, @NonNull CaptureBundle captureBundle,
            @NonNull CaptureProcessor captureProcessor, Surface surfaceTexture) {
        mHandler = handler;
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onOutputSurface(surfaceTexture, getImageFormat());
        mCaptureProcessor.onResolutionUpdate(
                new Size(mInputImageReader.getWidth(), mInputImageReader.getHeight()));

        mCaptureBundle = captureBundle;
        setCaptureBundle(captureBundle);
    }

    @Override
    @Nullable
    public ImageProxy acquireLatestImage() {
        return null;
    }

    @Override
    @Nullable
    public ImageProxy acquireNextImage() {
        return null;
    }

    @Override
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

    @Override
    public int getHeight() {
        synchronized (mLock) {
            return mInputImageReader.getHeight();
        }
    }

    @Override
    public int getWidth() {
        synchronized (mLock) {
            return mInputImageReader.getWidth();
        }
    }

    @Override
    public int getImageFormat() {
        synchronized (mLock) {
            return mInputImageReader.getImageFormat();
        }
    }

    @Override
    public int getMaxImages() {
        synchronized (mLock) {
            return mInputImageReader.getMaxImages();
        }
    }

    @Override
    public Surface getSurface() {
        synchronized (mLock) {
            return mInputImageReader.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(
            @Nullable final ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler) {
        synchronized (mLock) {
            mListener = listener;
            mHandler = handler;
            mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
        }
    }

    /** Sets a CaptureBundle */
    public void setCaptureBundle(@NonNull CaptureBundle captureBundle) {
        synchronized (mLock) {
            if (captureBundle.getCaptureStages() != null) {
                if (mInputImageReader.getMaxImages() < captureBundle.getCaptureStages().size()) {
                    throw new IllegalArgumentException(
                            "CaptureBundle is lager than InputImageReader.");
                }

                mCaptureIdList.clear();

                for (CaptureStage captureStage : captureBundle.getCaptureStages()) {
                    if (captureStage != null) {
                        mCaptureIdList.add(captureStage.getId());
                    }
                }
            }

            mSettableImageProxyBundle = new SettableImageProxyBundle(mCaptureIdList);
            setupSettableImageProxyBundleCallbacks();
        }
    }

    /** Returns necessary camera callbacks to retrieve metadata from camera result. */
    @Nullable
    CameraCaptureCallback getCameraCaptureCallback() {
        if (mInputImageReader instanceof MetadataImageReader) {
            return ((MetadataImageReader) mInputImageReader).getCameraCaptureCallback();
        } else {
            return null;
        }
    }

    void setupSettableImageProxyBundleCallbacks() {
        List<ListenableFuture<ImageProxy>> futureList = new ArrayList<>();
        for (Integer id : mCaptureIdList) {
            futureList.add(mSettableImageProxyBundle.getImageProxy((id)));
        }
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
                    if (!mCaptureIdList.contains(tag)) {
                        Log.w(TAG, "ImageProxyBundle does not contain this id: " + tag);
                        image.close();
                        return;
                    }

                    mSettableImageProxyBundle.addImageProxy(image);
                }
            }
        }
    }
}
