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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureFailure;
import androidx.camera.core.CameraCaptureResult;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraBurstCaptureCallbackTest {

    private CameraCaptureSession mSession;
    private CaptureRequest mRequest0;
    private CaptureRequest mRequest1;
    private TotalCaptureResult mResult;

    @Before
    public void setUp() {
        mSession = mock(CameraCaptureSession.class);
        mRequest0 = mock(CaptureRequest.class);
        mRequest1 = mock(CaptureRequest.class);
        mResult = mock(TotalCaptureResult.class);
    }

    @Test
    public void addCamera2Callbacks_withSameRequest() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks = new ArrayList<>();
        captureCallbacks.add(captureCallback0);
        captureCallbacks.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(captureCallback0).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(captureCallback1).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCamera2Callbacks_withSameRequestSeparately() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks1);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(captureCallback0).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(captureCallback1).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCamera2Callbacks_withDifferentRequest_doesNotCall() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks1);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest1, mResult);

        verify(captureCallback0, never()).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(captureCallback1, never()).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCameraCaptureCallbackContainer() {
        CameraCaptureSession.CaptureCallback captureCallback =
                mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureCallback cameraCaptureCallback =
                CaptureCallbackContainer.create(captureCallback);
        List<CameraCaptureCallback> cameraCaptureCallbacks = new ArrayList<>();
        cameraCaptureCallbacks.add(cameraCaptureCallback);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(captureCallback).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCameraCaptureCallbackContainer_withDifferentRequest_doesNotCall() {
        CameraCaptureSession.CaptureCallback captureCallback =
                mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureCallback cameraCaptureCallback =
                CaptureCallbackContainer.create(captureCallback);
        List<CameraCaptureCallback> cameraCaptureCallbacks = new ArrayList<>();
        cameraCaptureCallbacks.add(cameraCaptureCallback);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest1, mResult);

        verify(captureCallback, never()).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCameraCaptureCallback() {
        CameraCaptureCallback cameraCaptureCallback = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> cameraCaptureCallbacks = new ArrayList<>();
        cameraCaptureCallbacks.add(cameraCaptureCallback);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(cameraCaptureCallback).onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void addCameraCaptureCallback_withDifferentRequest_doesNotCall() {
        CameraCaptureCallback cameraCaptureCallback = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> cameraCaptureCallbacks = new ArrayList<>();
        cameraCaptureCallbacks.add(cameraCaptureCallback);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest1, mResult);

        verify(cameraCaptureCallback, never()).onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void addCameraCaptureCallbacks_withSameRequestSeparately() {
        CameraCaptureSession.CaptureCallback captureCallback =
                mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureCallback cameraCaptureCallback0 =
                CaptureCallbackContainer.create(captureCallback);
        List<CameraCaptureCallback> cameraCaptureCallbacks0 = new ArrayList<>();
        cameraCaptureCallbacks0.add(cameraCaptureCallback0);
        CameraCaptureCallback cameraCaptureCallback1 = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> cameraCaptureCallbacks1 = new ArrayList<>();
        cameraCaptureCallbacks1.add(cameraCaptureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks0);
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks1);

        CaptureFailure captureFailure = mock(CaptureFailure.class);
        burstCaptureCallback.onCaptureFailed(mSession, mRequest0, captureFailure);

        verify(captureCallback).onCaptureFailed(mSession, mRequest0, captureFailure);
        verify(cameraCaptureCallback1).onCaptureFailed(any(CameraCaptureFailure.class));
    }

    @Test
    public void addMixedCallbacks_withSameRequest() {
        CameraCaptureSession.CaptureCallback captureCallback =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks = new ArrayList<>();
        captureCallbacks.add(captureCallback);
        CameraCaptureCallback cameraCaptureCallback = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> cameraCaptureCallbacks = new ArrayList<>();
        cameraCaptureCallbacks.add(cameraCaptureCallback);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks);
        burstCaptureCallback.addCameraCaptureCallbacks(mRequest0, cameraCaptureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(captureCallback).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(cameraCaptureCallback).onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void callbackWithoutListener() {
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.onCaptureStarted(mSession, mRequest0, 0, 0);
    }
}
