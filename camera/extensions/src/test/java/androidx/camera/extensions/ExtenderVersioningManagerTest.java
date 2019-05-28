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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.camera.extensions.impl.ExtenderVersioning;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExtenderVersioningManagerTest {

    private ExtenderVersioningManager mExtenderVersioningManager;
    private ExtenderVersioning mFakeExtenderVersioningImpl;

    @Before
    public void setUp() {
        mExtenderVersioningManager = spy(ExtenderVersioningManager.class);
        mFakeExtenderVersioningImpl = mock(ExtenderVersioning.class);
        doReturn(mFakeExtenderVersioningImpl).when(mExtenderVersioningManager).create();
    }

    @Test
    public void testVendorReturnCurrentVersion() {
        initFakeVendorVersion(VersionName.CURRENT.toVersionString());

        assertTrue(mExtenderVersioningManager.isExtensionVersionSupported());
        assertEquals(mExtenderVersioningManager.getRuntimeVersion(),
                VersionName.CURRENT.getVersion());
    }

    @Test
    public void testVendorReturnInvalidVersion() {
        initFakeVendorVersion("0.0.0");

        assertFalse(mExtenderVersioningManager.isExtensionVersionSupported());
        assertNull(mExtenderVersioningManager.getRuntimeVersion());
    }

    @Test
    public void testVendorReturnEmptyVersion() {
        initFakeVendorVersion("");

        assertFalse(mExtenderVersioningManager.isExtensionVersionSupported());
        assertNull(mExtenderVersioningManager.getRuntimeVersion());
    }

    private void initFakeVendorVersion(String fakeResult) {
        when(mFakeExtenderVersioningImpl.checkApiVersion(any(String.class))).thenReturn(fakeResult);
        mExtenderVersioningManager.init();
    }
}
