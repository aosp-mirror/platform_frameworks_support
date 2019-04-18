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
import androidx.annotation.UiThread;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;


/**
 * A {@link DeferrableSurface} which invoke listener callback to recreate ImageReader and keep new
 * Surface of ImageReader.
 */
final class DeferredImageReaderSurface extends DeferrableSurface {
    @Nullable
    Size mResolution;

    @Nullable
    Surface mSurface;

    @NonNull
    SurfaceCreator mSurfaceCreator;

    DeferredImageReaderSurface(Size resolution,
            SurfaceCreator surfaceCreator) {
        mResolution = resolution;
        mSurfaceCreator = surfaceCreator;
    }

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
                                        if (mSurface == null) {
                                            mSurface = mSurfaceCreator
                                                    .create(mResolution);
                                        }
                                        completer.set(mSurface);
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
                mSurface = mSurfaceCreator.create(mResolution);
            }
        });

    }

    @UiThread
    void setResolution(Size resolution) {
        mResolution = resolution;
    }

    interface SurfaceCreator {
        Surface create(Size resolution);
    }

}
