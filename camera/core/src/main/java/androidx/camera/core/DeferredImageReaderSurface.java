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


import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;


/**
 * A {@link DeferrableSurface} which invoke listener callback to recreate ImageReader and keep new
 * Surface of ImageReader.
 */
final class DeferredImageReaderSurface extends DeferrableSurface {

    private static final String TAG = "DeferredImageReader";
    @Nullable
    final Size mResolution;

    @NonNull
    final ImageReaderCreator mImageReaderCreator;
    ImageReaderProxy mImageReader;

    @VisibleForTesting
    final List<ImageReaderProxy> mImageReaderToReleaseList = new ArrayList<>();

    DeferredImageReaderSurface(Size resolution, ImageReaderCreator imageReaderCreator) {
        mResolution = resolution;
        mImageReaderCreator = imageReaderCreator;

        // setup SurfaceDetachedListener to release active ImageReader
        setOnSurfaceDetachedListener(
                CameraXExecutors.mainThreadExecutor(),
                new DeferrableSurface.OnSurfaceDetachedListener() {
                    @Override
                    public void onSurfaceDetached() {
                        // release Active ImageReader when detached
                        if (mImageReader != null) {
                            // There will be the stric mode erro under API Level 23. Add surface
                            // release to dereference.
                            Surface surface = mImageReader.getSurface();
                            if (surface != null) {
                                surface.release();
                            }
                            mImageReader.close();
                            mImageReader = null;
                        }
                    }

                });

    }

    /**
     * Returns the {@link Surface} that is backed by a {@link ImageReaderProxy}.
     *
     * <p>If the {@link ImageReaderProxy} has already been released then the surface will be reset
     * using a new {@link ImageReaderProxy}.
     */
    @Override
    public ListenableFuture<Surface> getSurface() {
        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<Surface>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull final CallbackToFutureAdapter.Completer<Surface> completer) {
                        Runnable checkAndSetRunnable =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mImageReader == null) {
                                            mImageReader = mImageReaderCreator.create(mResolution);
                                        }
                                        completer.set(mImageReader.getSurface());
                                    }
                                };
                        runOnMainThread(checkAndSetRunnable);
                        return "DeferredImageReaderSurface";
                    }
                });

    }

    void runOnMainThread(Runnable runnable) {
        Executor executor =
                (Looper.myLooper() == Looper.getMainLooper())
                        ? CameraXExecutors.directExecutor()
                        : CameraXExecutors.mainThreadExecutor();
        executor.execute(runnable);
    }


    @Override
    public void refresh() {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                // add the old one to release list
                if (mImageReader != null) {
                    mImageReaderToReleaseList.add(mImageReader);
                }

                mImageReader = mImageReaderCreator.create(mResolution);
            }
        });

    }

    @Override
    public void notifySurfaceDetached() {
        // Release active image reader in detached listener
        super.notifySurfaceDetached();

        // Release inactive image readers
        if (!mImageReaderToReleaseList.isEmpty()) {
            for (ImageReaderProxy imageReader : mImageReaderToReleaseList) {
                // There will be the stric mode erro under API Level 23. Add surface
                // release to dereference.
                Surface surface = imageReader.getSurface();
                if (surface != null) {
                    surface.release();
                }

                imageReader.close();
            }

            mImageReaderToReleaseList.clear();
        }

    }

    @VisibleForTesting
    ImageReaderProxy getImageReader() {
        return mImageReader;
    }

    interface ImageReaderCreator {
        ImageReaderProxy create(Size resolution);
    }

}
