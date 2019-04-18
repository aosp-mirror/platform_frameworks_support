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

import android.graphics.ImageFormat;
import android.os.Build;
import android.util.Size;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class DeferredImageReaderSurfaceTest {

    private Size mDefaultResolution;
    private static final int MAX_IMAGES = 2;
    private static final int IMG_FORMAT = ImageFormat.JPEG;


    private DeferredImageReaderSurface mDeferredImageReaderSurface;
    private ImageReaderProxy mImageReader;

    private final DeferredImageReaderSurface.ImageReaderCreator mImageReaderCreator =
            new DeferredImageReaderSurface.ImageReaderCreator() {
                @Override
                public ImageReaderProxy create(Size resolution) {


                    if (mImageReader != null) {
                        //---- add ImageReader to Release List
                        mDeferredImageReaderSurface.releaseImageReader(mImageReader);
                    }

                    setupImageReader(resolution);
                    return mImageReader;
                }

            };


    @Before
    public void setup() {
        mDefaultResolution = new Size(640, 480);
        mDeferredImageReaderSurface =
                new DeferredImageReaderSurface(mDefaultResolution, mImageReaderCreator);

        //---- create ImageReader and attach surface
        mDeferredImageReaderSurface.refresh();
        mDeferredImageReaderSurface.notifySurfaceAttached();

    }

    private void setupImageReader(Size resolution) {
        MetadataImageReader metadataImageReader = new MetadataImageReader(
                resolution.getWidth(),
                resolution.getHeight(), IMG_FORMAT, MAX_IMAGES, null);
        mImageReader = metadataImageReader;
    }

    @Test
    public void recreateImageReaderWhenRefresh() {

        mDeferredImageReaderSurface.refresh();
        mDeferredImageReaderSurface.notifySurfaceAttached();

        mDeferredImageReaderSurface.refresh();
        mDeferredImageReaderSurface.notifySurfaceAttached();

        assertThat(mDeferredImageReaderSurface.mImageReaderToReleaseList.size())
                .isEqualTo(2);
        assertThat(mDeferredImageReaderSurface.getAttachedCount())
                .isEqualTo(3);

        assertThat(mDeferredImageReaderSurface.getSurface()).isNotNull();
    }


    @Test
    public void releaseImageReaderListWhenDeatached() {

        mDeferredImageReaderSurface.refresh();
        mDeferredImageReaderSurface.notifySurfaceAttached();

        mDeferredImageReaderSurface.refresh();
        mDeferredImageReaderSurface.notifySurfaceAttached();

        assertThat(mDeferredImageReaderSurface.mImageReaderToReleaseList.size())
                .isEqualTo(2);
        mDeferredImageReaderSurface.notifySurfaceDetached();
        assertThat(mDeferredImageReaderSurface.getAttachedCount())
                .isEqualTo(2);

        assertThat(mDeferredImageReaderSurface.getSurface()).isNotNull();
        assertThat(mDeferredImageReaderSurface.mImageReaderToReleaseList.size())
                .isEqualTo(0);

    }

    @Test
    public void releaseImageReaderWhenDeatachedAll() {

        mDeferredImageReaderSurface.refresh();
        mDeferredImageReaderSurface.notifySurfaceAttached();

        mDeferredImageReaderSurface.notifySurfaceDetached();
        mDeferredImageReaderSurface.notifySurfaceDetached();

        assertThat(mDeferredImageReaderSurface.getAttachedCount())
                .isEqualTo(0);

        assertThat(mDeferredImageReaderSurface.getImageReader()).isNull();
        assertThat(mDeferredImageReaderSurface.mImageReaderToReleaseList.size())
                .isEqualTo(0);

    }


}
