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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class ProcessingImageReaderTest {
    private static final int CAPTURE_ID_0 = 0;
    private static final int CAPTURE_ID_1 = 1;
    private static final int CAPTURE_ID_2 = 2;
    private static final int CAPTURE_ID_3 = 3;
    private static final long TIMESTAMP_0 = 0L;
    private static final long TIMESTAMP_1 = 1000L;
    private static final long TIMESTAMP_2 = 2000L;
    private static final long TIMESTAMP_3 = 4000L;
    private static final CaptureProcessor NOOP_PROCESSOR = new CaptureProcessor() {
        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {

        }

        @Override
        public void process(ImageProxyBundle bundle) {

        }

        @Override
        public void onResolutionUpdate(Size size) {

        }
    };

    private final CaptureStage mCaptureStage0 = new FakeCaptureStage(CAPTURE_ID_0, null);
    private final CaptureStage mCaptureStage1 = new FakeCaptureStage(CAPTURE_ID_1, null);
    private final CaptureStage mCaptureStage2 = new FakeCaptureStage(CAPTURE_ID_2, null);
    private final CaptureStage mCaptureStage3 = new FakeCaptureStage(CAPTURE_ID_3, null);
    private FakeImageReaderProxy mImageReaderProxy;
    private CaptureBundle mCaptureBundle;
    private Handler mMainHandler;

    @Before
    public void setUp() {
        mMainHandler = new Handler(Looper.getMainLooper());
        mCaptureBundle = CaptureBundles.createCaptureBundle(mCaptureStage0, mCaptureStage1);
        mImageReaderProxy = new FakeImageReaderProxy(8);
    }

    @Test
    public void canSetFuturesInSettableImageProxyBundle()
            throws InterruptedException, TimeoutException, ExecutionException {
        final AtomicReference<ImageProxyBundle> bundleRef = new AtomicReference<>();
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor captureProcessor = new CaptureProcessor() {
            @Override
            public void onOutputSurface(Surface surface, int imageFormat) {

            }

            @Override
            public void process(ImageProxyBundle bundle) {
                bundleRef.set(bundle);
            }

            @Override
            public void onResolutionUpdate(Size size) {

            }
        };
        ProcessingImageReader processingImageReader = new ProcessingImageReader(
                mImageReaderProxy, mMainHandler, mCaptureBundle, captureProcessor);
        Map<Integer, Long> resultMap = new HashMap<>();
        resultMap.put(CAPTURE_ID_0, TIMESTAMP_0);
        resultMap.put(CAPTURE_ID_1, TIMESTAMP_1);
        triggerAndVerify(bundleRef, resultMap);

        processingImageReader.setCaptureBundle(CaptureBundles.createCaptureBundle(mCaptureStage2,
                mCaptureStage3));
        Map<Integer, Long> resultMap1 = new HashMap<>();
        resultMap1.put(CAPTURE_ID_2, TIMESTAMP_2);
        resultMap1.put(CAPTURE_ID_3, TIMESTAMP_3);
        triggerAndVerify(bundleRef, resultMap1);
    }

    private void triggerAndVerify(AtomicReference<ImageProxyBundle> bundleRef,
            Map<Integer, Long> captureIdToTime)
            throws InterruptedException, ExecutionException, TimeoutException {
        // Feeds ImageProxy with all capture id on the initial list.
        for (Integer id : captureIdToTime.keySet()) {
            triggerImageAvailable(id, captureIdToTime.get(id));
        }

        // Ensure all posted tasks finish running
        ShadowLooper.runUiThreadTasks();

        ImageProxyBundle bundle = bundleRef.get();
        assertThat(bundle).isNotNull();

        // CaptureProcessor.process should be called once all ImageProxies on the
        // initial lists are ready. Then checks if the output has matched timestamp.
        for (Integer id : captureIdToTime.keySet()) {
            assertThat(bundle.getImageProxy(id).get(0,
                    TimeUnit.SECONDS).getTimestamp()).isEqualTo(captureIdToTime.get(id));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void imageReaderSizeIsSmallerThanCaptureBundle() {
        // Creates a ProcessingImageReader with maximum Image number smaller than CaptureBundle
        // size.
        ImageReaderProxy imageReaderProxy = new FakeImageReaderProxy(1);

        // Expects to throw exception when creating ProcessingImageReader.
        new ProcessingImageReader(imageReaderProxy, mMainHandler,
                mCaptureBundle, NOOP_PROCESSOR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void captureStageExceedMaxCaptureStage_setCaptureBundleThrowsException() {
        // Creates a ProcessingImageReader with maximum Image number.
        ProcessingImageReader processingImageReader = new ProcessingImageReader(100, 100,
                ImageFormat.YUV_420_888, 2, mMainHandler, mCaptureBundle,
                mock(CaptureProcessor.class));

        // Expects to throw exception when invoke the setCaptureBundle method with a
        // CaptureBundle size greater than maximum image number.
        processingImageReader.setCaptureBundle(
                CaptureBundles.createCaptureBundle(mCaptureStage1, mCaptureStage2, mCaptureStage3));
    }

    @Test
    public void testClearCaptureResult()
            throws InterruptedException, TimeoutException, ExecutionException {
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor mockCaptureProcessor = mock(CaptureProcessor.class);
        ProcessingImageReader processingImageReader = new ProcessingImageReader(
                mImageReaderProxy, mMainHandler,
                CaptureBundles.createCaptureBundle(mCaptureStage0, mCaptureStage1, mCaptureStage2),
                mockCaptureProcessor);

        // Trigger the ImageAvailable before clearCaptureResult() and the image should be closed
        // once the clearCaptureResult() was invoked.
        triggerImageAvailable(CAPTURE_ID_0, TIMESTAMP_0);

        // Assume the third capture result fail.
        processingImageReader.clearCaptureResult(Arrays.asList(true, true, false));

        // To verify all received ImageProxys should be closed.
        assertThat(mImageReaderProxy.getRemainingImageSize()).isEqualTo(0);

        // Trigger the ImageAvailable after clearCaptureResult().
        triggerImageAvailable(CAPTURE_ID_1, TIMESTAMP_1);

        // To verify the CAPTURE_ID_1 was closed.
        assertThat(mImageReaderProxy.getRemainingImageSize()).isEqualTo(0);

        // Never invoke the processor.process() since the capture result was cleared.
        verify(mockCaptureProcessor, never()).process(any());
    }

    @Test
    public void testClearCaptureResult_nextCaptureBundleCanWork()
            throws InterruptedException, TimeoutException, ExecutionException {
        final AtomicReference<ImageProxyBundle> bundleRef = new AtomicReference<>();
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor captureProcessor = new CaptureProcessor() {
            @Override
            public void onOutputSurface(Surface surface, int imageFormat) {

            }

            @Override
            public void process(ImageProxyBundle bundle) {
                bundleRef.set(bundle);
            }

            @Override
            public void onResolutionUpdate(Size size) {

            }
        };

        ProcessingImageReader processingImageReader = new ProcessingImageReader(
                mImageReaderProxy, mMainHandler, mCaptureBundle, captureProcessor);

        // Assume the first capture result fail.
        processingImageReader.clearCaptureResult(Arrays.asList(false, true));

        // To set the secondary capture bundle immediately after clearCaptureResult().
        // The secondary capture bundle should work fine.
        processingImageReader.setCaptureBundle(mCaptureBundle);

        // Trigger the ImageAvailable for the ImageProxy which belong to the first capture bundle.
        // The image should be closed.
        triggerImageAvailable(CAPTURE_ID_1, TIMESTAMP_1);

        // To verify the secondary capture bundle result should work fine.
        Map<Integer, Long> resultMap = new HashMap<>();
        resultMap.put(CAPTURE_ID_0, TIMESTAMP_2);
        resultMap.put(CAPTURE_ID_1, TIMESTAMP_3);
        triggerAndVerify(bundleRef, resultMap);
    }


    private void triggerImageAvailable(int captureId, long timestamp) throws InterruptedException {
        mImageReaderProxy.triggerImageAvailable(captureId, timestamp);
    }

}
