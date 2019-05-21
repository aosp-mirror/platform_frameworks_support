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

package androidx.camera.extensions;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Size;
import android.view.Surface;

import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraCaptureResults;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageProxyBundle;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** A {@link CaptureProcessor} that calls a vendor provided preview processing implementation. */
public class AdaptingPreviewProcessor implements CaptureProcessor {
    private final PreviewImageProcessorImpl mImpl;

    AdaptingPreviewProcessor(PreviewImageProcessorImpl impl) {
        mImpl = impl;
    }

    @Override
    public void onOutputSurface(Surface surface, int imageFormat) {
        mImpl.onOutputSurface(surface, imageFormat);
        mImpl.onImageFormatUpdate(imageFormat);
    }

    @Override
    public void process(ImageProxyBundle bundle) {
        List<Integer> ids = bundle.getCaptureIds();

        if (ids.size() > 1) {
            throw new IllegalArgumentException("Processing bundle of size greater than 1.");
        }

        ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(ids.get(0));
        try {
            ImageProxy imageProxy = imageProxyListenableFuture.get(5, TimeUnit.SECONDS);
            Image image = imageProxy.getImage();
            if (image == null) {
                return;
            }

            ImageInfo imageInfo = imageProxy.getImageInfo();

            CameraCaptureResult result =
                    CameraCaptureResults.retrieveCameraCaptureResult(imageInfo);
            if (result == null) {
                mImpl.process(imageProxy.getImage(), null);
                return;
            }

            CaptureResult captureResult =
                    Camera2CameraCaptureResultConverter.getCaptureResult(result);
            if (captureResult == null) {
                mImpl.process(imageProxy.getImage(), null);

                return;
            }

            TotalCaptureResult totalCaptureResult = (TotalCaptureResult) captureResult;
            if (totalCaptureResult == null) {
                mImpl.process(imageProxy.getImage(), null);

                return;
            }

            mImpl.process(imageProxy.getImage(), totalCaptureResult);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            return;
        }
    }

    @Override
    public void onResolutionUpdate(Size size) {
        mImpl.onResolutionUpdate(size);
    }
}
